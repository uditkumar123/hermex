package com.hermex.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String? = null,
    val sessions: Int? = null,
    @SerialName("active_streams") val activeStreams: Int? = null,
    @SerialName("uptime_seconds") val uptimeSeconds: Double? = null
)

@Serializable
data class AuthStatusResponse(
    @SerialName("authenticated") val authenticated: Boolean? = null,
    @SerialName("password_required") val passwordRequired: Boolean? = null,
    @SerialName("auth_enabled") val authEnabled: Boolean? = null,
    @SerialName("logged_in") val loggedIn: Boolean? = null,
    @SerialName("password_auth_enabled") val passwordAuthEnabled: Boolean? = null,
    @SerialName("passkeys_enabled") val passkeysEnabled: Boolean? = null,
    @SerialName("passwordless_enabled") val passwordlessEnabled: Boolean? = null
) {
    val isAuthRequired: Boolean
        get() = when {
            authenticated != null -> !authenticated
            authEnabled != null -> authEnabled && loggedIn != true
            else -> false
        }

    val isPasswordOnly: Boolean
        get() = when {
            passwordRequired != null -> passwordRequired
            else -> passwordAuthEnabled == true && passkeysEnabled != true
        }
}

@Serializable
data class LoginResponse(
    val ok: Boolean? = null,
    val message: String? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = ok == true
    val displayMessage: String get() = message ?: error ?: "Unknown response"
}
