package com.hermex.app.ui.navigation

import com.hermex.app.data.auth.SessionExpiredMessage
import com.hermex.app.data.model.AuthStatusResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class StartupRouteResolverTest {

    @Test
    fun `missing saved server routes to onboarding`() {
        assertEquals(
            StartupDestination.Onboarding,
            StartupRouteResolver.resolve(activeServerUrl = null, authStatus = null)
        )
    }

    @Test
    fun `authenticated saved server routes to session list`() {
        assertEquals(
            StartupDestination.SessionList,
            StartupRouteResolver.resolve(
                activeServerUrl = "https://example.com",
                authStatus = AuthStatusResponse(authenticated = true, authEnabled = true)
            )
        )
    }

    @Test
    fun `auth disabled saved server routes to session list`() {
        assertEquals(
            StartupDestination.SessionList,
            StartupRouteResolver.resolve(
                activeServerUrl = "https://example.com",
                authStatus = AuthStatusResponse(authenticated = false, authEnabled = false)
            )
        )
    }

    @Test
    fun `unauthenticated saved server routes to password prompt`() {
        assertEquals(
            StartupDestination.Password("https://example.com", SessionExpiredMessage),
            StartupRouteResolver.resolve(
                activeServerUrl = "https://example.com",
                authStatus = AuthStatusResponse(authenticated = false, authEnabled = true)
            )
        )
    }

    @Test
    fun `auth status failure routes to retry`() {
        assertEquals(
            StartupDestination.Retry("https://example.com", "timeout"),
            StartupRouteResolver.resolve(
                activeServerUrl = "https://example.com",
                authStatus = null,
                failureMessage = "timeout"
            )
        )
    }
}
