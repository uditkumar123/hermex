package com.hermex.app.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.TextView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.hermex.app.data.auth.AuthManager
import com.hermex.app.data.auth.AuthState
import com.hermex.app.data.model.ChatMessage
import com.hermex.app.ui.theme.*
import com.hermex.app.util.toRelativeTime
import io.noties.markwon.Markwon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleView(
    message: ChatMessage,
    isStreaming: Boolean = false,
    sessionId: String? = null,
    serverUrl: String? = null,
    onRegenerate: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .combinedClickable(
                    onClick = { },
                    onLongClick = { showContextMenu = true }
                )
                .padding(12.dp)
        ) {
            Column {
                if (message.hasReasoning && message.reasoning != null) {
                    ReasoningBlock(
                        reasoning = message.reasoning,
                        isStreaming = isStreaming
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (message.displayContent.isNotEmpty()) {
                    val markwon = remember { Markwon.create(context) }
                    val displayContent = addCodeBlockLabels(message.displayContent)
                    val spannable = remember(displayContent) {
                        markwon.toMarkdown(displayContent)
                    }
                    val textColor = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary.toArgb()
                    } else {
                        MaterialTheme.colorScheme.onSurface.toArgb()
                    }
                    AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                setTextColor(textColor)
                                textSize = 16f
                            }
                        },
                        update = { tv ->
                            tv.setTextColor(textColor)
                            markwon.setParsedMarkdown(tv, spannable)
                        }
                    )
                }

                if (message.hasAttachments && message.attachments != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    message.attachments.forEach { attachment ->
                        if (attachment.isImageAttachment && sessionId != null) {
                            val imageUrl = buildRawFileUrl(
                                baseUrl = serverUrl,
                                sessionId = sessionId,
                                path = attachment.path ?: attachment.name
                            )
                            if (imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = attachment.displayName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 300.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    onState = { state ->
                                        if (state is AsyncImagePainter.State.Error) {
                                            // fallback: show filename if image fails to load
                                        }
                                    }
                                )
                            }
                        } else {
                            Text(
                                text = "📎 ${attachment.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isUser) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = {
                        copyToClipboard(context, message.displayContent)
                        showContextMenu = false
                    }
                )
                if (!isUser && onRegenerate != null) {
                    DropdownMenuItem(
                        text = { Text("Regenerate") },
                        onClick = {
                            onRegenerate()
                            showContextMenu = false
                        }
                    )
                }
            }
        }

        if (message.displayTimestamp > 0) {
            Text(
                text = message.displayTimestamp.toRelativeTime(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

internal fun addCodeBlockLabels(markdown: String): String {
    val codeBlockRegex = Regex("""```(\w[\w+#.-]*)\s*\n""")
    return codeBlockRegex.replace(markdown) { match ->
        val lang = match.groupValues[1]
        val label = lang.split(".").last()
        "> `$label`\n>\n```$label\n"
    }
}

private fun buildRawFileUrl(baseUrl: String?, sessionId: String, path: String?): String {
    val base = baseUrl?.trimEnd('/') ?: return ""
    val filePath = path ?: return ""
    return "$base/api/file/raw?session_id=${Uri.encode(sessionId)}&path=${Uri.encode(filePath)}"
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
}

@Composable
private fun ReasoningBlock(
    reasoning: String,
    isStreaming: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ReasoningBorder.copy(alpha = 0.1f))
            .padding(8.dp)
    ) {
        Text(
            text = if (isStreaming) "Thinking..." else "Thought",
            style = MaterialTheme.typography.labelSmall,
            color = ReasoningBorder
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = reasoning,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StreamingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "●  ●  ●",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Start
        )
    }
}
