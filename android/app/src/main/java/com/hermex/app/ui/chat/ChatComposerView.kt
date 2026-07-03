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
    currentModel: String? = null,
    currentProvider: String? = null,
    onModelChange: ((model: String?, provider: String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var showModelPicker by remember { mutableStateOf(false) }

    val canSend = inputText.isNotBlank() && !isStreaming && !isSending
    val canSteer = inputText.isNotBlank() && isStreaming && onSteer != null

    Surface(
        modifier = modifier,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (onModelChange != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = showModelPicker,
                        onClick = { showModelPicker = !showModelPicker },
                        label = {
                            Text(
                                text = currentModel?.take(20) ?: "Model",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                    if (currentProvider != null) {
                        Text(
                            text = currentProvider,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (showModelPicker) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = currentModel ?: "",
                            onValueChange = { newModel ->
                                onModelChange(newModel.ifBlank { null }, currentProvider)
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Model name") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = currentProvider ?: "",
                            onValueChange = { newProvider ->
                                onModelChange(currentModel, newProvider.ifBlank { null })
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Provider") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

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
