package com.hermex.app.data.auth

import android.content.Context
import com.hermex.app.data.api.LoginRequest
import com.hermex.app.data.api.RetrofitProvider
import com.hermex.app.data.model.APIError
import com.hermex.app.data.model.AuthStatusResponse
import com.hermex.app.data.model.HealthResponse
import com.hermex.app.data.model.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import okhttp3.Request
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class AuthState {
    data object Unconfigured : AuthState()
    data class LoggedOut(val serverUrl: String) : AuthState()
    data class LoggedIn(val serverUrl: String) : AuthState()
}

class AuthManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }

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
        return withContext(Dispatchers.IO) {
            try {
                val normalizedUrl = RetrofitProvider.normalizeUrl(serverUrl)
                val healthUrl = if (normalizedUrl.endsWith("/")) "${normalizedUrl}health" else "$normalizedUrl/health"
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .followRedirects(false)
                    .build()
                val request = Request.Builder()
                    .url(healthUrl)
                    .head()
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful || response.code in 301..399) {
                    Result.success(HealthResponse())
                } else {
                    Result.failure(APIError.Http(response.code))
                }
            } catch (e: Exception) {
                Timber.e(e, "Connection test failed")
                Result.failure(wrapError(e))
            }
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
            RetrofitProvider.warmupSession(serverUrl, context)
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
        return when (e) {
            is HttpException -> when (e.code()) {
                401 -> APIError.Unauthorized
                403 -> APIError.Http(403)
                404 -> APIError.Http(404)
                500 -> APIError.Http(500)
                502, 503, 504 -> APIError.Http(e.code())
                else -> APIError.Http(e.code())
            }
            is SerializationException -> APIError.Decoding(e)
            is SocketTimeoutException -> APIError.Network(e)
            is ConnectException -> APIError.Network(e)
            is UnknownHostException -> APIError.Network(e)
            is APIError -> e
            else -> APIError.Network(e)
        }
    }
}
