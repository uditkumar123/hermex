package com.hermex.app.ui.navigation

import com.hermex.app.data.model.AuthStatusResponse
import com.hermex.app.data.auth.SessionExpiredMessage

sealed class StartupDestination {
    data object Onboarding : StartupDestination()
    data object SessionList : StartupDestination()
    data class Password(val serverUrl: String, val message: String? = null) : StartupDestination()
    data class Retry(val serverUrl: String, val message: String) : StartupDestination()
}

object StartupRouteResolver {
    fun resolve(activeServerUrl: String?, authStatus: AuthStatusResponse?, failureMessage: String? = null): StartupDestination {
        val serverUrl = activeServerUrl?.trim().orEmpty()
        if (serverUrl.isEmpty()) {
            return StartupDestination.Onboarding
        }

        if (failureMessage != null) {
            return StartupDestination.Retry(serverUrl, failureMessage)
        }

        if (authStatus == null || authStatus.isAuthRequired) {
            return StartupDestination.Password(serverUrl, SessionExpiredMessage)
        }

        return StartupDestination.SessionList
    }
}
