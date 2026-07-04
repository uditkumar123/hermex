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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import java.net.URI
import java.net.SocketTimeoutException
import java.net.UnknownHostException

const val SessionExpiredMessage = "Session expired. Sign in again."

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

    private val errorJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val currentServerUrl: String?
        get() = when (val s = _state.value) {
            is AuthState.LoggedIn -> s.serverUrl
            is AuthState.LoggedOut -> s.serverUrl
            else -> null
        }

    init {
        RetrofitProvider.onUnauthorized = { handleSessionExpired() }
    }

    fun initialize() {
        ServerRegistry.initialize(context)
        if (_state.value is AuthState.Unconfigured) {
            val active = ServerRegistry.activeServer()
            if (active != null) {
                CustomHeaderStore.configure(active.urlString, context)
                _state.value = AuthState.LoggedOut(active.urlString)
            }
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
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful || response.code in 301..399) {
                        Result.success(HealthResponse())
                    } else {
                        Result.failure(APIError.Http(response.code))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Connection test failed")
                Result.failure(wrapError(e))
            }
        }
    }

    suspend fun checkAuthStatus(serverUrl: String): Result<AuthStatusResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedUrl = RetrofitProvider.normalizeUrl(serverUrl)
                val statusUrl = "${normalizedUrl}api/auth/status"
                val client = RetrofitProvider.createOkHttpClient(
                    context = context,
                    readTimeoutSeconds = 10,
                    notifyUnauthorized = false
                )
                val request = Request.Builder()
                    .url(statusUrl)
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val json = Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                                coerceInputValues = true
                            }
                            val status = json.decodeFromString<AuthStatusResponse>(body)
                            Result.success(status)
                        } else {
                            Result.failure(APIError.Decoding(Exception("Empty response body")))
                        }
                    } else if (response.code == 401) {
                        Result.success(passwordRequiredStatus())
                    } else {
                        Result.failure(APIError.Http(response.code, response.body?.string()))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Auth status check failed")
                Result.failure(wrapError(e))
            }
        }
    }

    private fun passwordRequiredStatus(): AuthStatusResponse {
        return AuthStatusResponse(
            authenticated = false,
            authEnabled = true,
            passwordRequired = true,
            passwordAuthEnabled = true
        )
    }

    suspend fun login(serverUrl: String, password: String): Result<LoginResponse> {
        _isLoading.value = true
        _error.value = null
        return try {
            RetrofitProvider.warmupSession(serverUrl, context)
            val api = RetrofitProvider.createApi(serverUrl, context)
            val response = api.login(LoginRequest(password))
            if (response.isSuccess) {
                ServerRegistry.addServer(serverUrl, null, context)
                CustomHeaderStore.configure(serverUrl, context)
                _state.value = AuthState.LoggedIn(serverUrl)
                _error.value = null
                Result.success(response)
            } else {
                _error.value = response.displayMessage
                Result.failure(Exception(response.displayMessage))
            }
        } catch (e: Exception) {
            val wrapped = wrapError(e)
            _error.value = loginFailureMessage(e, wrapped)
            Result.failure(wrapped)
        } finally {
            _isLoading.value = false
        }
    }

    private fun loginFailureMessage(e: Exception, wrapped: Exception): String {
        return when (wrapped) {
            is APIError.Unauthorized -> errorBodyMessage(e) ?: "Invalid password"
            else -> wrapped.message ?: "Unable to sign in."
        }
    }

    private fun errorBodyMessage(e: Exception): String? {
        val body = (e as? HttpException)?.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() }
            ?: return null
        return try {
            val json = errorJson.parseToJsonElement(body).jsonObject
            json["error"]?.jsonPrimitive?.contentOrNull
                ?: json["message"]?.jsonPrimitive?.contentOrNull
        } catch (_: Exception) {
            null
        }
    }

    suspend fun logout() {
        val url = currentServerUrl ?: return
        try {
            val api = RetrofitProvider.createApi(url, context)
            api.logout()
        } catch (e: Exception) {
            Timber.w(e, "Logout request failed (non-fatal)")
        }
        clearCookiesForServer(url)
        RetrofitProvider.invalidate()
        _state.value = AuthState.LoggedOut(url)
    }

    fun connectToServer(serverUrl: String) {
        ServerRegistry.addServer(serverUrl, null, context)
        CustomHeaderStore.configure(serverUrl, context)
        _state.value = AuthState.LoggedIn(serverUrl)
        _error.value = null
    }

    fun handleSessionExpired() {
        val url = currentServerUrl ?: return
        clearCookiesForServer(url)
        RetrofitProvider.invalidate()
        _state.value = AuthState.LoggedOut(url)
        _error.value = SessionExpiredMessage
    }

    fun handleAPIError(error: APIError) {
        when (error) {
            is APIError.Unauthorized -> {
                handleSessionExpired()
            }
            else -> {
                _error.value = error.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun clearCookiesForServer(serverUrl: String) {
        try {
            val host = URI(RetrofitProvider.normalizeUrl(serverUrl)).host ?: return
            RetrofitProvider.clearCookiesForHost(host)
        } catch (e: Exception) {
            Timber.w(e, "Failed to clear cookies for server")
        }
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
