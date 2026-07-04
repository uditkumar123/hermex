package com.hermex.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hermex.app.data.api.SSEClient
import com.hermex.app.data.auth.AuthManager
import com.hermex.app.data.auth.AuthState
import com.hermex.app.data.model.*
import com.hermex.app.data.repository.ChatRepository
import com.hermex.app.data.repository.OfflineMessageRepository
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
    val connectionState: ConnectionState = ConnectionState.Connected,
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

    private val cacheRepo by lazy { OfflineMessageRepository(getApplication()) }

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
                    val cached = cacheRepo.getMessages(sessionId)
                    if (cached.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                messages = cached,
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.message
                            )
                        }
                    }
                }
            )
        }
    }

    fun sendMessage(text: String) {
        val message = text.trim()
        if (message.isEmpty()) return

        if (message.startsWith("/")) {
            val resolved = resolveSlashCommand(message)
            if (resolved != null) {
                val commandName = resolved.second["command"] ?: ""
                val commandArgs = resolved.second["args"]?.trim().orEmpty()
                if (dispatchSlashCommand(commandName, commandArgs)) return
            }
        }

        val repo = chatRepo ?: return

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

    private fun dispatchSlashCommand(commandName: String, args: String): Boolean {
        when (commandName) {
            "steer" -> {
                if (args.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "Usage: /steer <text>") }
                } else {
                    steerChat(args)
                }
                return true
            }
            "interrupt" -> {
                cancelStream()
                return true
            }
        }

        if (slashCommands.none { it.name == commandName }) {
            _uiState.update { it.copy(errorMessage = "Unknown command: /$commandName") }
            return true
        }

        when (commandName) {
            "help" -> addSystemMessage(formatHelpText())
            "status" -> dispatchStatus()
            "queue" -> dispatchStatus("queue")
            "title" -> if (args.isNotEmpty()) dispatchRename(args) else addSystemMessage("Usage: /title <text>")
            "undo" -> dispatchUndo()
            "retry" -> dispatchRetry()
            "compress", "compact" -> dispatchCompress()
            "model" -> if (args.isNotEmpty()) dispatchUpdate(model = args) else addSystemMessage("Usage: /model <name>")
            "workspace" -> if (args.isNotEmpty()) dispatchUpdate(workspace = args) else addSystemMessage("Usage: /workspace <path>")
            "personality" -> if (args.isNotEmpty()) dispatchPersonality(args) else addSystemMessage("Usage: /personality <name>")
            "reasoning" -> dispatchReasoning(args)
            "new" -> dispatchNewSession(args)
            "skills" -> addSystemMessage("Open the Skills screen from the menu to view and manage skills.")
            "btw" -> if (args.isNotEmpty()) dispatchBtw(args) else addSystemMessage("Usage: /btw <message>")
            "background", "bg" -> if (args.isNotEmpty()) dispatchBackground(args) else addSystemMessage("Usage: /$commandName <message>")
            "branch", "fork" -> dispatchBranch()
            else -> addSystemMessage("Command /$commandName will be sent to the server.")
        }
        return true
    }

    private fun addSystemMessage(text: String) {
        val msg = ChatMessage(
            role = "system",
            content = text,
            timestamp = System.currentTimeMillis() / 1000.0,
            messageId = "system_${System.currentTimeMillis()}"
        )
        _uiState.update { it.copy(messages = it.messages + msg) }
    }

    private fun formatHelpText(): String {
        val byCategory = slashCommands.groupBy { it.category }
        val sb = StringBuilder("**Available Commands**\n\n")
        byCategory.forEach { (category, cmds) ->
            sb.appendLine("**$category**")
            cmds.forEach { cmd ->
                sb.appendLine("- `${cmd.hint}` — ${cmd.description}")
            }
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun dispatchStatus(mode: String = "status") {
        val repo = sessionRepo ?: run { addSystemMessage("Session repository not available."); return }
        viewModelScope.launch {
            repo.fetchSessionStatus(sessionId).fold(
                onSuccess = { status ->
                    val msg = buildString {
                        appendLine("**${if (mode == "queue") "Queue" else "Session Status"}**")
                        status.isStreaming?.let { appendLine("- Streaming: $it") }
                        status.pendingUserMessage?.let { appendLine("- Pending message: $it") }
                        status.activeStreamId?.let { appendLine("- Active stream: $it") }
                    }
                    addSystemMessage(msg.trimEnd())
                },
                onFailure = { addSystemMessage("Failed to fetch status: ${it.message}") }
            )
        }
    }

    private fun dispatchRename(title: String) {
        val repo = sessionRepo ?: run { addSystemMessage("Session repository not available."); return }
        viewModelScope.launch {
            repo.renameSession(sessionId, title).fold(
                onSuccess = {
                    _uiState.update { it.copy(title = title) }
                    addSystemMessage("Session renamed to \"$title\".")
                },
                onFailure = { addSystemMessage("Failed to rename: ${it.message}") }
            )
        }
    }

    private fun dispatchUndo() {
        val repo = sessionRepo ?: run { addSystemMessage("Session repository not available."); return }
        viewModelScope.launch {
            repo.undoSession(sessionId).fold(
                onSuccess = { addSystemMessage("Last assistant message undone."); loadMessages() },
                onFailure = { addSystemMessage("Failed to undo: ${it.message}") }
            )
        }
    }

    private fun dispatchRetry() {
        val repo = sessionRepo ?: run { addSystemMessage("Session repository not available."); return }
        viewModelScope.launch {
            repo.retrySession(sessionId).fold(
                onSuccess = { addSystemMessage("Retrying last response..."); loadMessages() },
                onFailure = { addSystemMessage("Failed to retry: ${it.message}") }
            )
        }
    }

    private fun dispatchCompress() {
        val repo = sessionRepo ?: run { addSystemMessage("Session repository not available."); return }
        viewModelScope.launch {
            repo.compressSession(sessionId).fold(
                onSuccess = { addSystemMessage("Context window compressed.") },
                onFailure = { addSystemMessage("Failed to compress: ${it.message}") }
            )
        }
    }

    private fun dispatchUpdate(model: String? = null, workspace: String? = null) {
        val repo = sessionRepo ?: run { addSystemMessage("Session repository not available."); return }
        viewModelScope.launch {
            repo.updateSession(sessionId, model = model, workspace = workspace).fold(
                onSuccess = {
                    if (model != null) {
                        currentModel = model
                        addSystemMessage("Model switched to \"$model\".")
                    }
                    if (workspace != null) {
                        currentWorkspace = workspace
                        addSystemMessage("Workspace switched to \"$workspace\".")
                    }
                },
                onFailure = { addSystemMessage("Failed to update: ${it.message}") }
            )
        }
    }

    private fun dispatchPersonality(name: String) {
        val repo = sessionRepo ?: run { addSystemMessage("Session repository not available."); return }
        viewModelScope.launch {
            repo.switchProfile(name).fold(
                onSuccess = { currentProfile = name; addSystemMessage("Personality switched to \"$name\".") },
                onFailure = { addSystemMessage("Failed to switch personality: ${it.message}") }
            )
        }
    }

    private fun dispatchReasoning(level: String) {
        val valid = listOf("low", "medium", "high")
        if (level.lowercase() !in valid) {
            addSystemMessage("Usage: /reasoning <level> where level is one of: ${valid.joinToString(", ")}")
            return
        }
        addSystemMessage("Reasoning set to \"$level\". This will apply on the next message.")
    }

    private fun dispatchNewSession(title: String) {
        val repo = sessionRepo ?: run { addSystemMessage("Session repository not available."); return }
        viewModelScope.launch {
            repo.createSession().fold(
                onSuccess = { session ->
                    val msg = if (title.isNotEmpty()) {
                        repo.renameSession(session.sessionId ?: "", title)
                        "New session created: \"$title\" (${session.sessionId}). Use the session list to switch."
                    } else {
                        "New session created (${session.sessionId}). Use the session list to switch."
                    }
                    addSystemMessage(msg)
                },
                onFailure = { addSystemMessage("Failed to create session: ${it.message}") }
            )
        }
    }

    private fun dispatchBtw(text: String) {
        val repo = sessionRepo ?: run { addSystemMessage("Session repository not available."); return }
        viewModelScope.launch {
            repo.sendBtw(sessionId, text).fold(
                onSuccess = { addSystemMessage("Background message sent: \"$text\"") },
                onFailure = { addSystemMessage("Failed to send btw: ${it.message}") }
            )
        }
    }

    private fun dispatchBackground(text: String) {
        val repo = sessionRepo ?: run { addSystemMessage("Session repository not available."); return }
        viewModelScope.launch {
            repo.backgroundStart(sessionId, text).fold(
                onSuccess = { addSystemMessage("Background task started: \"$text\"") },
                onFailure = { addSystemMessage("Failed to start background task: ${it.message}") }
            )
        }
    }

    private fun dispatchBranch() {
        val repo = sessionRepo ?: run { addSystemMessage("Session repository not available."); return }
        viewModelScope.launch {
            repo.branchSession(sessionId).fold(
                onSuccess = {
                    addSystemMessage("Session branched. Use the session list to switch to the new branch.")
                },
                onFailure = { addSystemMessage("Failed to branch: ${it.message}") }
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

            repo.streamChat(streamId).collect { streamEvent ->
                when (streamEvent) {
                    is SSEStreamEvent.StateChange -> {
                        _uiState.update { it.copy(connectionState = streamEvent.state) }
                    }
                    is SSEStreamEvent.Event -> {
                        val event = streamEvent.event
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
                                cacheRepo.cacheMessages(sessionId, finalMessages)
                            }

                            is SSEEvent.StreamEnd -> {
                                val currentMessages = _uiState.value.messages
                                _uiState.update {
                                    it.copy(
                                        isStreaming = false,
                                        activeStreamId = null,
                                        liveReasoningText = "",
                                        liveToolCalls = emptyList()
                                    )
                                }
                                cacheRepo.cacheMessages(sessionId, currentMessages)
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
