package com.hermex.app.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class SlashCommand(
    val name: String,
    val hint: String,
    val description: String,
    val category: String = "General"
)

val slashCommands = listOf(
    SlashCommand("help", "/help", "Show available commands"),
    SlashCommand("new", "/new [title]", "Start a new session"),
    SlashCommand("model", "/model <name>", "Switch model"),
    SlashCommand("workspace", "/workspace <path>", "Switch workspace"),
    SlashCommand("reasoning", "/reasoning <level>", "Set reasoning effort (low/medium/high)"),
    SlashCommand("title", "/title <text>", "Rename current session"),
    SlashCommand("personality", "/personality <name>", "Switch personality/profile"),
    SlashCommand("skills", "/skills [query]", "List available skills"),
    SlashCommand("queue", "/queue", "Show queued messages"),
    SlashCommand("steer", "/steer <text>", "Steer active response"),
    SlashCommand("interrupt", "/interrupt", "Interrupt current response"),
    SlashCommand("status", "/status", "Show session status"),
    SlashCommand("btw", "/btw <message>", "Send background message"),
    SlashCommand("background", "/background <message>", "Start background task", "Tasks"),
    SlashCommand("bg", "/bg <message>", "Shortcut for /background", "Tasks"),
    SlashCommand("branch", "/branch", "Branch current session"),
    SlashCommand("fork", "/fork", "Alias for /branch"),
    SlashCommand("undo", "/undo", "Undo last assistant message"),
    SlashCommand("retry", "/retry", "Retry last response"),
    SlashCommand("compress", "/compress", "Compress context window"),
    SlashCommand("compact", "/compact", "Alias for /compress"),
)

@Composable
fun SlashCommandDropdown(
    query: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!query.startsWith("/") || query.length < 2) return

    val filterText = query.drop(1).lowercase()
    val matches = slashCommands.filter {
        it.name.startsWith(filterText) || it.hint.contains(filterText, ignoreCase = true)
    }

    if (matches.isEmpty()) return

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        LazyColumn(
            modifier = Modifier.heightIn(max = 240.dp)
        ) {
            items(matches) { cmd ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = cmd.hint,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    supportingContent = { Text(cmd.description) },
                    modifier = Modifier.clickable {
                        onSelect(cmd.hint)
                    }
                )
            }
            if (matches.size > 5) {
                item {
                    Text(
                        text = "${matches.size} matches — keep typing to narrow",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

fun resolveSlashCommand(text: String): Pair<String, Map<String, String>>? {
    if (!text.startsWith("/")) return null
    val parts = text.trim().split("\\s+".toRegex(), 2)
    val command = parts[0].removePrefix("/").lowercase()
    val args = parts.getOrElse(1) { "" }
    return command to mapOf("command" to command, "args" to args)
}
