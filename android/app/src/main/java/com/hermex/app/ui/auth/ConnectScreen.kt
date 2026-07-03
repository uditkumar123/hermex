package com.hermex.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hermex.app.data.api.RetrofitProvider
import com.hermex.app.data.auth.AuthManager
import com.hermex.app.ui.theme.HermexBlue
import com.hermex.app.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    onConnected: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var serverUrl by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(ConnectStep.ENTER_URL) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var authRequired by remember { mutableStateOf(false) }
    var serverVersion by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect to Server") },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (step) {
                ConnectStep.ENTER_URL -> {
                    Text(
                        text = "Enter your hermes-webui server URL.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = {
                            serverUrl = it
                            errorMessage = null
                        },
                        label = { Text("Server URL") },
                        placeholder = { Text("https://your-server.com:8787") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (serverUrl.isNotBlank()) {
                                    step = ConnectStep.TESTING
                                    errorMessage = null
                                    scope.launch {
                                        testConnection(authManager, serverUrl) { result ->
                                            result.fold(
                                                onSuccess = { (authReq, version) ->
                                                    authRequired = authReq
                                                    serverVersion = version
                                                    step = if (authReq) ConnectStep.ENTER_PASSWORD else ConnectStep.CONNECTING
                                                    if (!authReq) {
                                                        scope.launch {
                                                            connectDirectly(authManager, serverUrl) { success ->
                                                                if (success) onConnected()
                                                            }
                                                        }
                                                    }
                                                },
                                                onFailure = { e ->
                                                    errorMessage = e.message
                                                    step = ConnectStep.ENTER_URL
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        ),
                        enabled = step == ConnectStep.ENTER_URL
                    )

                    AnimatedVisibility(visible = step == ConnectStep.TESTING) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Testing connection...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                ConnectStep.ENTER_PASSWORD -> {
                    serverVersion?.let { version ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = SuccessGreen.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Server reachable${if (version.isNotBlank()) " (v$version)" else ""}",
                                    color = SuccessGreen,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Text(
                        text = "This server requires a password.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (password.isNotBlank()) {
                                    step = ConnectStep.CONNECTING
                                    errorMessage = null
                                    scope.launch {
                                        connectWithPassword(authManager, serverUrl, password) { success ->
                                            if (success) onConnected()
                                            else step = ConnectStep.ENTER_PASSWORD
                                        }
                                    }
                                }
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        }
                    )
                }

                ConnectStep.TESTING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Testing connection...")
                    }
                }

                ConnectStep.CONNECTING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Connecting...")
                    }
                }
            }

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (step == ConnectStep.ENTER_PASSWORD) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (password.isNotBlank()) {
                            step = ConnectStep.CONNECTING
                            errorMessage = null
                            scope.launch {
                                connectWithPassword(authManager, serverUrl, password) { success ->
                                    if (success) onConnected()
                                    else step = ConnectStep.ENTER_PASSWORD
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = HermexBlue)
                ) {
                    Text("Connect")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Make sure your server is running and accessible.\nIf local, use Tailscale or a Cloudflare Tunnel.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private enum class ConnectStep {
    ENTER_URL, TESTING, ENTER_PASSWORD, CONNECTING
}

private suspend fun testConnection(
    authManager: AuthManager,
    serverUrl: String,
    onResult: (Result<Pair<Boolean, String?>>) -> Unit
) {
    val healthResult = authManager.testConnection(serverUrl)
    healthResult.fold(
        onSuccess = { health ->
            val authStatusResult = authManager.checkAuthStatus(serverUrl)
            authStatusResult.fold(
                onSuccess = { status ->
                    onResult(Result.success(
                        Pair(
                            status.isAuthRequired,
                            health.uptimeSeconds?.let { "up" } ?: null
                        )
                    ))
                },
                onFailure = { e ->
                    onResult(Result.failure(e))
                }
            )
        },
        onFailure = { e ->
            onResult(Result.failure(e))
        }
    )
}

private suspend fun connectDirectly(
    authManager: AuthManager,
    serverUrl: String,
    onResult: (Boolean) -> Unit
) {
    authManager.connectToServer(serverUrl)
    onResult(true)
}

private suspend fun connectWithPassword(
    authManager: AuthManager,
    serverUrl: String,
    password: String,
    onResult: (Boolean) -> Unit
) {
    val result = authManager.login(serverUrl, password)
    result.fold(
        onSuccess = { onResult(true) },
        onFailure = { onResult(false) }
    )
}
