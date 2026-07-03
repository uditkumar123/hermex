package com.hermex.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hermex.app.data.auth.AuthManager
import com.hermex.app.data.auth.AuthState
import com.hermex.app.data.auth.ServerRegistry
import com.hermex.app.data.auth.SessionExpiredMessage
import com.hermex.app.ui.auth.ConnectScreen
import com.hermex.app.ui.auth.OnboardingScreen
import com.hermex.app.ui.auth.SettingsScreen
import com.hermex.app.ui.chat.ChatScreen
import com.hermex.app.ui.sessionlist.SessionListScreen
import com.hermex.app.ui.workspace.FileBrowserScreen
import com.hermex.app.ui.workspace.MemoryScreen
import com.hermex.app.ui.workspace.SkillsScreen

object Routes {
    const val AUTH_GATE = "auth_gate"
    const val ONBOARDING = "onboarding"
    const val CONNECT = "connect"
    const val CONNECT_WITH_ARGS = "connect?serverUrl={serverUrl}&message={message}"
    const val SESSION_LIST = "session_list"
    const val CHAT = "chat/{sessionId}"
    const val FILE_BROWSER = "file_browser/{sessionId}"
    const val SKILLS = "skills"
    const val MEMORY = "memory"
    const val SETTINGS = "settings"

    fun chat(sessionId: String) = "chat/$sessionId"
    fun fileBrowser(sessionId: String) = "file_browser/$sessionId"
    fun connect(serverUrl: String? = null, message: String? = null): String {
        if (serverUrl.isNullOrBlank()) return CONNECT
        return "connect?serverUrl=${Uri.encode(serverUrl)}&message=${Uri.encode(message.orEmpty())}"
    }
}

@Composable
fun HermexNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authManager = remember { AuthManager.getInstance(context) }
    val authState by authManager.state.collectAsState()
    val authError by authManager.error.collectAsState()

    LaunchedEffect(authState, authError) {
        val loggedOut = authState as? AuthState.LoggedOut
        if (loggedOut != null && authError == SessionExpiredMessage) {
            navController.navigate(Routes.connect(loggedOut.serverUrl, SessionExpiredMessage)) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH_GATE
    ) {
        composable(Routes.AUTH_GATE) {
            AuthGateScreen { destination ->
                when (destination) {
                    StartupDestination.Onboarding -> navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.AUTH_GATE) { inclusive = true }
                    }
                    StartupDestination.SessionList -> navController.navigate(Routes.SESSION_LIST) {
                        popUpTo(Routes.AUTH_GATE) { inclusive = true }
                    }
                    is StartupDestination.Password -> navController.navigate(
                        Routes.connect(destination.serverUrl, destination.message)
                    ) {
                        popUpTo(Routes.AUTH_GATE) { inclusive = true }
                    }
                    is StartupDestination.Retry -> navController.navigate(
                        Routes.connect(destination.serverUrl, destination.message)
                    ) {
                        popUpTo(Routes.AUTH_GATE) { inclusive = true }
                    }
                }
            }
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onConnectClick = {
                    navController.navigate(Routes.connect())
                }
            )
        }

        composable(Routes.CONNECT) {
            ConnectScreen(
                onConnected = {
                    navController.navigate(Routes.SESSION_LIST) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.CONNECT_WITH_ARGS,
            arguments = listOf(
                navArgument("serverUrl") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("message") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            ConnectScreen(
                initialServerUrl = backStackEntry.arguments?.getString("serverUrl"),
                initialMessage = backStackEntry.arguments?.getString("message")?.ifBlank { null },
                onConnected = {
                    navController.navigate(Routes.SESSION_LIST) {
                        popUpTo(Routes.AUTH_GATE) { inclusive = true }
                    }
                },
                onBack = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SESSION_LIST) {
            SessionListScreen(
                onSessionClick = { sessionId ->
                    navController.navigate(Routes.chat(sessionId))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                },
                onSkillsClick = {
                    navController.navigate(Routes.SKILLS)
                },
                onMemoryClick = {
                    navController.navigate(Routes.MEMORY)
                }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            ChatScreen(
                sessionId = sessionId,
                onBack = {
                    navController.popBackStack()
                },
                onSkillsClick = {
                    navController.navigate(Routes.SKILLS)
                },
                onMemoryClick = {
                    navController.navigate(Routes.MEMORY)
                }
            )
        }

        composable(
            route = Routes.FILE_BROWSER,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            FileBrowserScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
                onClose = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.SKILLS) {
            SkillsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.MEMORY) {
            MemoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
private fun AuthGateScreen(onDestination: (StartupDestination) -> Unit) {
    val context = LocalContext.current
    val authManager = remember { AuthManager.getInstance(context) }

    LaunchedEffect(Unit) {
        authManager.initialize()
        val serverUrl = ServerRegistry.activeServer()?.urlString
        if (serverUrl.isNullOrBlank()) {
            onDestination(StartupDestination.Onboarding)
            return@LaunchedEffect
        }

        authManager.checkAuthStatus(serverUrl).fold(
            onSuccess = { status ->
                val destination = StartupRouteResolver.resolve(serverUrl, status)
                if (destination == StartupDestination.SessionList) {
                    authManager.connectToServer(serverUrl)
                }
                onDestination(destination)
            },
            onFailure = { error ->
                onDestination(
                    StartupRouteResolver.resolve(
                        activeServerUrl = serverUrl,
                        authStatus = null,
                        failureMessage = error.message ?: "Unable to verify saved server session."
                    )
                )
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
            Text(
                text = "Checking saved session...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
