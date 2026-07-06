package com.hermex.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hermex.app.data.auth.AuthManager
import com.hermex.app.data.auth.AuthState
import com.hermex.app.data.model.ApprovalPendingResponse
import com.hermex.app.data.model.ChatMessage
import com.hermex.app.data.model.ClarificationPendingResponse
import com.hermex.app.data.model.ConnectionState
import com.hermex.app.data.model.ContextWindowSnapshot
import com.hermex.app.ui.theme.HermexBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    onBack: () -> Unit,
    onSkillsClick: () -> Unit = {},
    onMemoryClick: () -> Unit = {},
    viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(
            context = (LocalContext.current.applicationContext as? android.app.Application)
                ?: error("Application context required for ViewModel"),
            sessionId = sessionId
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val authState by AuthManager.getInstance(LocalContext.current).state.collectAsState()
    val serverUrl = (authState as? AuthState.LoggedIn)?.serverUrl

    var selectedModel by remember { mutableStateOf<String?>(null) }
    var selectedProvider by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionId) {
        viewModel.loadMessages()
    }

    LaunchedEffect(uiState.messages.size, uiState.isStreaming) {
        if (uiState.messages.isNotEmpty()) {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            val nearBottom = lastVisibleItem >= totalItems - 2
            if (nearBottom || !uiState.isStreaming) {
                coroutineScope.launch {
                    listState.animateScrollToItem(uiState.messages.size - 1)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (uiState.connectionState) {
                                        ConnectionState.Connected -> Color(0xFF4CAF50)
                                        ConnectionState.Reconnecting -> Color(0xFFFFC107)
                                        ConnectionState.Disconnected -> Color(0xFFF44336)
                                    }
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = uiState.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                            when (uiState.connectionState) {
                                ConnectionState.Reconnecting -> Text(
                                    text = "Reconnecting...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFC107)
                                )
                                ConnectionState.Disconnected -> Text(
                                    text = "Disconnected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF44336)
                                )
                                else -> {
                                    if (uiState.isStreaming) {
                                        Text(
                                            text = "Streaming...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HermexBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onMemoryClick) {
                        Icon(Icons.Default.Storage, contentDescription = "Memory")
                    }
                    IconButton(onClick = onSkillsClick) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Skills")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Error banner
            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Messages
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.messages.isEmpty() && !uiState.isStreaming -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Send a message to start the conversation",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = uiState.messages,
                            key = { it.messageId ?: it.hashCode().toString() }
                        ) { message ->
                            MessageBubbleView(
                                message = message,
                                isStreaming = uiState.isStreaming && message.role == "assistant" && message.messageId?.startsWith("streaming_") == true,
                                sessionId = sessionId,
                                serverUrl = serverUrl
                            )
                        }

                        // Live tool calls
                        if (uiState.liveToolCalls.isNotEmpty()) {
                            item(key = "live_tools") {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    uiState.liveToolCalls.forEach { toolCall ->
                                        ToolCallCardView(
                                            toolCall = toolCall,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Streaming indicator
                        if (uiState.isStreaming && uiState.liveReasoningText.isEmpty()) {
                            item(key = "streaming") {
                                StreamingIndicator()
                            }
                        }
                    }
                }
            }

            // Context window indicator
            uiState.contextWindow?.let { contextWindow ->
                ContextWindowIndicator(contextWindow = contextWindow)
            }

            // Composer
            ChatComposerView(
                isStreaming = uiState.isStreaming,
                isSending = uiState.isSending,
                onSend = { viewModel.sendMessage(it, selectedModel, selectedProvider) },
                onCancel = { viewModel.cancelStream() },
                onSteer = { viewModel.steerChat(it) },
                currentModel = selectedModel,
                currentProvider = selectedProvider,
                onModelChange = { model, provider ->
                    selectedModel = model
                    selectedProvider = provider
                }
            )
        }
    }

    // Approval overlay
    if (uiState.approvalPending != null) {
        ApprovalOverlay(
            response = uiState.approvalPending!!,
            onApprove = { viewModel.respondToApproval(it) },
            onReject = { viewModel.dismissApproval() },
            onDismiss = { viewModel.dismissApproval() }
        )
    }

    // Clarification overlay
    if (uiState.clarificationPending != null) {
        ClarificationOverlay(
            response = uiState.clarificationPending!!,
            onRespond = { viewModel.respondToClarification(it) },
            onDismiss = { viewModel.dismissClarification() }
        )
    }
}

class ChatViewModelFactory(
    private val context: android.app.Application,
    private val sessionId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(context, sessionId) as T
    }
}

@Composable
fun ContextWindowIndicator(contextWindow: ContextWindowSnapshot) {
    val threshold = contextWindow.thresholdTokens ?: 0
    val current = contextWindow.lastPromptTokens ?: 0
    val max = contextWindow.contextLength ?: 0

    if (max > 0 && threshold > 0) {
        val progress = current.toFloat() / max.toFloat()
        val isNearLimit = current >= (threshold * 0.8).toInt()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp),
                color = when {
                    progress > 0.9f -> MaterialTheme.colorScheme.error
                    isNearLimit -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${current}/${max}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ApprovalOverlay(
    response: ApprovalPendingResponse,
    onApprove: (String) -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit
) {
    val pending = response.pending ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Approval Required") },
        text = {
            Column {
                Text(
                    text = pending.displayDescription,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (pending.preview != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pending.preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onReject) {
                    Text("Reject")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { onApprove("approved") }) {
                    Text("Approve")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun ClarificationOverlay(
    response: ClarificationPendingResponse,
    onRespond: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val pending = response.pending ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clarification Needed") },
        text = {
            Column {
                Text(
                    text = pending.displayQuestion,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (pending.displayChoices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    pending.displayChoices.forEach { choice ->
                        TextButton(
                            onClick = { onRespond(choice) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(choice)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}
