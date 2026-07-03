package com.hermex.app.data.auth

import android.content.Context
import com.hermex.app.data.api.LoginRequest
import com.hermex.app.data.api.RetrofitProvider
import com.hermex.app.data.model.APIError
import com.hermex.app.data.model.AuthStatusResponse
import com.hermex.app.data.model.HealthResponse
import com.hermex.app.data.model.LoginResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

sealed class AuthState {
    data object Unconfigured : AuthState()
    data class LoggedOut(val serverUrl: String) : AuthState()
    data class LoggedIn(val serverUrl: String) : AuthState()
}

class AuthManager(private val context: Context) {

    private val _state = MutableStateFlow<AuthState>(AuthState.Unconfigured)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val currentServerUrl: String?
        get() = when (val s = _state.value) {
            is AuthState.LoggedIn -> s.serverUrl
            is AuthState.LoggedOut -> s.serverUrl
            else -> null
        }

    fun initialize() {
        ServerRegistry.initialize(context)
        val active = ServerRegistry.activeServer()
        if (active != null) {
            CustomHeaderStore.configure(active.urlString, context)
            _state.value = AuthState.LoggedOut(active.urlString)
        }
    }

    suspend fun testConnection(serverUrl: String): Result<HealthResponse> {
        return try {
            val api = RetrofitProvider.createApi(serverUrl)
            val response = api.health()
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Connection test failed")
            Result.failure(wrapError(e))
        }
    }

    suspend fun checkAuthStatus(serverUrl: String): Result<AuthStatusResponse> {
        return try {
            val api = RetrofitProvider.createApi(serverUrl)
            val response = api.authStatus()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(wrapError(e))
        }
    }

    suspend fun login(serverUrl: String, password: String): Result<LoginResponse> {
        _isLoading.value = true
        _error.value = null
        return try {
            val api = RetrofitProvider.createApi(serverUrl)
            val response = api.login(LoginRequest(password))
            if (response.isSuccess) {
                ServerRegistry.addServer(serverUrl, null, context)
                CustomHeaderStore.configure(serverUrl, context)
                _state.value = AuthState.LoggedIn(serverUrl)
                Result.success(response)
            } else {
                _error.value = response.displayMessage
                Result.failure(Exception(response.displayMessage))
            }
        } catch (e: Exception) {
            _error.value = e.message
            Result.failure(wrapError(e))
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun logout() {
        val url = currentServerUrl ?: return
        try {
            val api = RetrofitProvider.createApi(url)
            api.logout()
        } catch (e: Exception) {
            Timber.w(e, "Logout request failed (non-fatal)")
        }
        _state.value = AuthState.LoggedOut(url)
    }

    fun connectToServer(serverUrl: String) {
        ServerRegistry.addServer(serverUrl, null, context)
        CustomHeaderStore.configure(serverUrl, context)
        _state.value = AuthState.LoggedIn(serverUrl)
    }

    fun handleAPIError(error: APIError) {
        when (error) {
            is APIError.Unauthorized -> {
                val url = currentServerUrl ?: return
                _state.value = AuthState.LoggedOut(url)
            }
            else -> {
                _error.value = error.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun wrapError(e: Exception): Exception {
        return when {
            e.message?.contains("401") == true -> APIError.Unauthorized
            e.message?.contains("404") == true -> APIError.Http(404)
            e.message?.contains("timeout", ignoreCase = true) == true ->
                APIError.Network(e)
            else -> APIError.Network(e)
        }
    }
}
