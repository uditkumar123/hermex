package com.hermex.app.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hermex.app.data.api.RetrofitProvider
import com.hermex.app.data.auth.AuthManager
import com.hermex.app.data.auth.AuthManager.Companion.getInstance
import com.hermex.app.data.auth.CustomHeaderStore
import com.hermex.app.data.auth.ServerRegistry
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authManager = getInstance(context)
    val coroutineScope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Server Section
            Text(
                text = "Server",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)
            )

            val activeServer = ServerRegistry.activeServer()
            if (activeServer != null) {
                ListItem(
                    headlineContent = { Text(activeServer.displayName ?: activeServer.urlString) },
                    supportingContent = { Text(activeServer.urlString) },
                    leadingContent = {
                        Icon(Icons.Default.Dns, contentDescription = null)
                    }
                )
            }

            // Multi-server management
            ServerRegistry.servers.forEach { server ->
                val isActive = server.id == ServerRegistry.activeServerId
                ListItem(
                    headlineContent = {
                        Text(
                            text = server.displayName ?: server.urlString,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    supportingContent = { Text(server.urlString) },
                    leadingContent = {
                        if (isActive) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable {
                        if (!isActive) {
                            ServerRegistry.switchToServer(server.id, context)
                            CustomHeaderStore.configure(server.urlString, context)
                            RetrofitProvider.invalidate()
                        }
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Account Section
            Text(
                text = "Account",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )

            ListItem(
                headlineContent = { Text("Sign Out") },
                leadingContent = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable { showLogoutDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // About Section
            Text(
                text = "About",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )

            ListItem(
                headlineContent = { Text("Version") },
                supportingContent = { Text("1.0.0") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
            )

            ListItem(
                headlineContent = { Text("Servers configured") },
                supportingContent = { Text("${ServerRegistry.servers.size}") },
                leadingContent = { Icon(Icons.Default.Dns, contentDescription = null) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Support Section
            Text(
                text = "Support",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )

            // TODO: Set up donation link (Ko-fi, GitHub Sponsors, etc.)
            ListItem(
                headlineContent = { Text("Donate") },
                supportingContent = { Text("Coming soon - support Hermex development") },
                leadingContent = {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable {
                    // TODO: Add donation URL when ready
                }
            )

            ListItem(
                headlineContent = { Text("Report a Bug") },
                supportingContent = { Text("Report issues or suggest features") },
                leadingContent = {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hermes-webui/hermes/issues/new"))
                    context.startActivity(intent)
                }
            )

            ListItem(
                headlineContent = { Text("Documentation") },
                supportingContent = { Text("View API docs and guides") },
                leadingContent = { Icon(Icons.Default.Description, contentDescription = null) },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://get-hermes.ai/api-docs/"))
                    context.startActivity(intent)
                }
            )
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out? You will need to reconnect to your server.") },
            confirmButton = {
                    TextButton(
                    onClick = {
                        showLogoutDialog = false
                        coroutineScope.launch {
                            authManager.logout()
                            RetrofitProvider.invalidate()
                            onLogout()
                        }
                    }
                ) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
