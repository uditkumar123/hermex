package com.hermex.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hermex.app.data.api.SSEClient
import com.hermex.app.data.auth.AuthManager
import com.hermex.app.data.auth.AuthState
import com.hermex.app.data.model.*
import com.hermex.app.data.repository.ChatRepository
import com.hermex.app.data.repository.SessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val title: String = "Chat",
    val activeStreamId: String? = null,
    val liveReasoningText: String = "",
    val liveToolCalls: List<ToolStreamEvent> = emptyList(),
    val contextWindow: ContextWindowSnapshot? = null,
    val approvalPending: ApprovalPendingResponse? = null,
    val clarificationPending: ClarificationPendingResponse? = null
)

class ChatViewModel(
    application: Application,
    private val sessionId: String
) : AndroidViewModel(application) {

    private val authManager = AuthManager.getInstance(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val sessionRepo: SessionRepository? by lazy {
        val url = (authManager.state.value as? AuthState.LoggedIn)?.serverUrl
        url?.let { SessionRepository(it, getApplication()) }
    }

    private val chatRepo: ChatRepository? by lazy {
        val url = (authManager.state.value as? AuthState.LoggedIn)?.serverUrl
        url?.let { ChatRepository(it, getApplication()) }
    }

    private var streamJob: Job? = null
    private var currentModel: String? = null
    private var currentModelProvider: String? = null
    private var currentWorkspace: String? = null
    private var currentProfile: String? = null

    init {
        authManager.initialize()
    }

    fun loadMessages() {
        val repo = sessionRepo ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repo.fetchSession(sessionId, includeMessages = true, messageLimit = 50).fold(
                onSuccess = { session ->
                    currentModel = session.model
                    currentModelProvider = session.modelProvider
                    currentWorkspace = session.workspace
                    currentProfile = session.profile
                    _uiState.update {
                        it.copy(
                            messages = session.messages ?: emptyList(),
                            isLoading = false,
                            title = session.title?.trim()?.ifEmpty { null } ?: "Chat",
                            contextWindow = ContextWindowSnapshot(
                                contextLength = session.contextLength,
                                thresholdTokens = session.thresholdTokens,
                                lastPromptTokens = session.lastPromptTokens,
                                inputTokens = session.inputTokens,
                                outputTokens = session.outputTokens,
                                estimatedCost = session.estimatedCost
                            )
                        )
                    }

                    val activeStreamId = session.activeStreamId?.trim()?.ifEmpty { null }
                    if (activeStreamId != null) {
                        reconnectToStream(activeStreamId)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message
                        )
                    }
                }
            )
        }
    }

    fun sendMessage(text: String) {
        val repo = chatRepo ?: return
        val message = text.trim()
        if (message.isEmpty()) return

        val userMessage = ChatMessage(
            role = "user",
            content = message,
            timestamp = System.currentTimeMillis() / 1000.0,
            messageId = "local_${System.currentTimeMillis()}"
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                isSending = true,
                isStreaming = false,
                errorMessage = null,
                liveReasoningText = "",
                liveToolCalls = emptyList()
            )
        }

        viewModelScope.launch {
            repo.startChat(
                sessionId = sessionId,
                message = message,
                workspace = currentWorkspace,
                model = currentModel,
                modelProvider = currentModelProvider,
                profile = currentProfile
            ).fold(
                onSuccess = { response ->
                    val streamId = response.streamId
                    if (streamId != null) {
                        _uiState.update {
                            it.copy(
                                isSending = false,
                                isStreaming = true,
                                activeStreamId = streamId
                            )
                        }
                        startStreaming(streamId)
                    } else {
                        _uiState.update {
                            it.copy(
                                isSending = false,
                                errorMessage = response.error ?: "No stream ID returned"
                            )
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = e.message
                        )
                    }
                }
            )
        }
    }

    private fun startStreaming(streamId: String) {
        val repo = chatRepo ?: return
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            val assistantMessageId = "streaming_${System.currentTimeMillis()}"
            var accumulatedText = ""
            var accumulatedReasoning = ""
            val toolCalls = mutableListOf<ToolStreamEvent>()

            repo.streamChat(streamId).collect { event ->
                when (event) {
                    is SSEEvent.Token -> {
                        accumulatedText += event.text
                        updateStreamingMessage(assistantMessageId, accumulatedText, accumulatedReasoning, toolCalls)
                    }

                    is SSEEvent.Reasoning -> {
                        accumulatedReasoning += event.text
                        _uiState.update { it.copy(liveReasoningText = accumulatedReasoning) }
                    }

                    is SSEEvent.ToolStarted -> {
                        toolCalls.add(event.event)
                        _uiState.update { it.copy(liveToolCalls = toolCalls.toList()) }
                    }

                    is SSEEvent.ToolCompleted -> {
                        val idx = toolCalls.indexOfFirst { it.stableId == event.event.stableId }
                        if (idx >= 0) {
                            toolCalls[idx] = event.event
                        } else {
                            toolCalls.add(event.event)
                        }
                        _uiState.update { it.copy(liveToolCalls = toolCalls.toList()) }
                    }

                    is SSEEvent.Title -> {
                        if (event.title != null) {
                            _uiState.update { it.copy(title = event.title) }
                        }
                    }

                    is SSEEvent.Done -> {
                        val finalMessages = if (event.session?.messages != null) {
                            event.session.messages
                        } else {
                            val finalMsg = ChatMessage(
                                role = "assistant",
                                content = accumulatedText.ifEmpty { null },
                                reasoning = accumulatedReasoning.ifEmpty { null },
                                timestamp = System.currentTimeMillis() / 1000.0,
                                messageId = assistantMessageId
                            )
                            _uiState.value.messages.filterNot { it.messageId == assistantMessageId } + finalMsg
                        }
                        _uiState.update {
                            it.copy(
                                messages = finalMessages,
                                isStreaming = false,
                                activeStreamId = null,
                                liveReasoningText = "",
                                liveToolCalls = emptyList(),
                                contextWindow = event.usage ?: it.contextWindow
                            )
                        }
                    }

                    is SSEEvent.StreamEnd -> {
                        _uiState.update {
                            it.copy(
                                isStreaming = false,
                                activeStreamId = null,
                                liveReasoningText = "",
                                liveToolCalls = emptyList()
                            )
                        }
                    }

                    is SSEEvent.Cancelled -> {
                        _uiState.update {
                            it.copy(
                                isStreaming = false,
                                activeStreamId = null,
                                liveReasoningText = "",
                                liveToolCalls = emptyList()
                            )
                        }
                    }

                    is SSEEvent.Error -> {
                        _uiState.update {
                            it.copy(
                                isStreaming = false,
                                activeStreamId = null,
                                errorMessage = event.message,
                                liveReasoningText = "",
                                liveToolCalls = emptyList()
                            )
                        }
                    }

                    is SSEEvent.TransportError -> {
                        _uiState.update {
                            it.copy(
                                isStreaming = false,
                                activeStreamId = null,
                                errorMessage = "Connection lost: ${event.message}",
                                liveReasoningText = "",
                                liveToolCalls = emptyList()
                            )
                        }
                    }

                    is SSEEvent.ApprovalPending -> {
                        _uiState.update { it.copy(approvalPending = event.response) }
                    }

                    is SSEEvent.ClarificationPending -> {
                        _uiState.update { it.copy(clarificationPending = event.response) }
                    }

                    else -> { /* ignore */ }
                }
            }
        }
    }

    private fun updateStreamingMessage(
        messageId: String,
        content: String,
        reasoning: String,
        toolCalls: List<ToolStreamEvent>
    ) {
        val existingMessages = _uiState.value.messages.filterNot { it.messageId == messageId }
        val streamingMsg = ChatMessage(
            role = "assistant",
            content = content.ifEmpty { null },
            reasoning = reasoning.ifEmpty { null },
            timestamp = System.currentTimeMillis() / 1000.0,
            messageId = messageId
        )
        _uiState.update {
            it.copy(messages = existingMessages + streamingMsg)
        }
    }

    private fun reconnectToStream(streamId: String) {
        _uiState.update { it.copy(isStreaming = true, activeStreamId = streamId) }
        startStreaming(streamId)
    }

    fun cancelStream() {
        val streamId = _uiState.value.activeStreamId ?: return
        streamJob?.cancel()
        viewModelScope.launch {
            chatRepo?.cancelChat(streamId)
        }
        _uiState.update {
            it.copy(
                isStreaming = false,
                activeStreamId = null,
                liveReasoningText = "",
                liveToolCalls = emptyList()
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissApproval() {
        _uiState.update { it.copy(approvalPending = null) }
    }

    fun dismissClarification() {
        _uiState.update { it.copy(clarificationPending = null) }
    }

    fun steerChat(text: String) {
        val repo = chatRepo ?: return
        val streamId = _uiState.value.activeStreamId ?: return
        viewModelScope.launch {
            repo.steerChat(sessionId, text).fold(
                onSuccess = { response ->
                    if (response.accepted != true) {
                        _uiState.update {
                            it.copy(errorMessage = response.error ?: "Steer rejected")
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            )
        }
    }

    fun respondToApproval(choice: String) {
        val repo = chatRepo ?: return
        viewModelScope.launch {
            repo.respondToApproval(sessionId, choice).fold(
                onSuccess = {
                    _uiState.update { it.copy(approvalPending = null) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            )
        }
    }

    fun respondToClarification(response: String) {
        val repo = chatRepo ?: return
        viewModelScope.launch {
            repo.respondToClarification(sessionId, response).fold(
                onSuccess = {
                    _uiState.update { it.copy(clarificationPending = null) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
        chatRepo?.stop()
    }
}
