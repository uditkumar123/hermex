package com.hermex.app.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hermex.app.ui.theme.HermexBlue

@Composable
fun ChatComposerView(
    isStreaming: Boolean,
    isSending: Boolean,
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
    onSteer: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val canSend = inputText.isNotBlank() && !isStreaming && !isSending
    val canSteer = inputText.isNotBlank() && isStreaming && onSteer != null

    Surface(
        modifier = modifier,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (isStreaming) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = HermexBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generating response...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            onCancel()
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (isStreaming) "Steer response..." else "Message...") },
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (canSend) {
                                onSend(inputText)
                                inputText = ""
                                focusManager.clearFocus()
                            } else if (canSteer) {
                                onSteer?.invoke(inputText)
                                inputText = ""
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    enabled = !isSending
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (canSend) {
                            onSend(inputText)
                            inputText = ""
                            focusManager.clearFocus()
                        } else if (canSteer) {
                            onSteer?.invoke(inputText)
                            inputText = ""
                            focusManager.clearFocus()
                        }
                    },
                    enabled = canSend || canSteer,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Bottom)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isStreaming) "Steer" else "Send",
                        tint = if (canSend || canSteer) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
