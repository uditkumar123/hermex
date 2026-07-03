package com.hermex.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hermex.app.ui.auth.ConnectScreen
import com.hermex.app.ui.auth.OnboardingScreen
import com.hermex.app.ui.auth.SettingsScreen
import com.hermex.app.ui.chat.ChatScreen
import com.hermex.app.ui.sessionlist.SessionListScreen
import com.hermex.app.ui.workspace.FileBrowserScreen
import com.hermex.app.ui.workspace.MemoryScreen
import com.hermex.app.ui.workspace.SkillsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val CONNECT = "connect"
    const val SESSION_LIST = "session_list"
    const val CHAT = "chat/{sessionId}"
    const val FILE_BROWSER = "file_browser/{sessionId}"
    const val SKILLS = "skills"
    const val MEMORY = "memory"
    const val SETTINGS = "settings"

    fun chat(sessionId: String) = "chat/$sessionId"
    fun fileBrowser(sessionId: String) = "file_browser/$sessionId"
}

@Composable
fun HermexNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.ONBOARDING
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onConnectClick = {
                    navController.navigate(Routes.CONNECT)
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

        composable(Routes.SESSION_LIST) {
            SessionListScreen(
                onSessionClick = { sessionId ->
                    navController.navigate(Routes.chat(sessionId))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
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
