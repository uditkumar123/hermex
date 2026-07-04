package com.hermex.app.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hermex.app.data.api.RetrofitProvider
import com.hermex.app.data.model.APIError
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
class AuthManagerTest {

    private lateinit var context: Context
    private lateinit var authManager: AuthManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        authManager = AuthManager.getInstance(context)
        authManager.clearError()
        RetrofitProvider.invalidate()
    }

    @Test
    fun `state starts as a valid AuthState`() = runTest {
        val state = authManager.state.first()
        assertTrue(state is AuthState)
    }

    @Test
    fun `isLoading starts as false`() = runTest {
        assertFalse(authManager.isLoading.first())
    }

    @Test
    fun `error starts as null`() = runTest {
        assertNull(authManager.error.first())
    }

    @Test
    fun `currentServerUrl returns valid state`() {
        authManager.connectToServer("https://test.com")
        assertEquals("https://test.com", authManager.currentServerUrl)
    }

    @Test
    fun `getInstance returns same instance`() {
        val instance1 = AuthManager.getInstance(context)
        val instance2 = AuthManager.getInstance(context)
        assertSame(instance1, instance2)
    }

    @Test
    fun `handleAPIError with Unauthorized demotes to LoggedOut`() = runTest {
        authManager.connectToServer("https://example.com")
        assertEquals(AuthState.LoggedIn("https://example.com"), authManager.state.first())

        authManager.handleAPIError(APIError.Unauthorized)
        assertEquals(AuthState.LoggedOut("https://example.com"), authManager.state.first())
    }

    @Test
    fun `handleAPIError with non-unauthorized sets error message`() = runTest {
        authManager.connectToServer("https://example.com")
        authManager.handleAPIError(APIError.Http(500))
        assertTrue(authManager.error.first()!!.contains("500"))
    }

    @Test
    fun `clearError resets error to null`() = runTest {
        authManager.connectToServer("https://example.com")
        authManager.handleAPIError(APIError.Http(500))
        assertNotNull(authManager.error.first())

        authManager.clearError()
        assertNull(authManager.error.first())
    }

    @Test
    fun `connectToServer transitions to LoggedIn`() = runTest {
        authManager.connectToServer("https://my-server.com")
        assertEquals(AuthState.LoggedIn("https://my-server.com"), authManager.state.first())
        assertEquals("https://my-server.com", authManager.currentServerUrl)
    }

    @Test
    fun `wrapError correctly identifies HttpException 401`() {
        val exception = mockk<HttpException>(relaxed = true)
        every { exception.code() } returns 401
        every { exception.message() } returns "HTTP 401"
        val result = invokeWrapError(exception)
        assertTrue(result is APIError.Unauthorized)
    }

    @Test
    fun `wrapError correctly identifies SocketTimeoutException`() {
        val exception = SocketTimeoutException("timeout")
        val result = invokeWrapError(exception)
        assertTrue(result is APIError.Network)
    }

    @Test
    fun `wrapError correctly identifies ConnectException`() {
        val exception = ConnectException("connection refused")
        val result = invokeWrapError(exception)
        assertTrue(result is APIError.Network)
    }

    @Test
    fun `wrapError correctly identifies UnknownHostException`() {
        val exception = UnknownHostException("unknown host")
        val result = invokeWrapError(exception)
        assertTrue(result is APIError.Network)
    }

    @Test
    fun `wrapError handles generic exception as Network`() {
        val exception = RuntimeException("generic error")
        val result = invokeWrapError(exception)
        assertTrue(result is APIError.Network)
    }

    @Test
    fun `wrapError correctly identifies SerializationException as Decoding`() {
        val exception = SerializationException("Unexpected json token at offset 0: expected start of the object, but had <")
        val result = invokeWrapError(exception)
        assertTrue(result is APIError.Decoding)
        assertTrue(result.message!!.contains("unexpected response"))
    }

    @Test
    fun `initialize does not overwrite LoggedIn state`() = runTest {
        authManager.connectToServer("https://active-server.com")
        assertEquals(AuthState.LoggedIn("https://active-server.com"), authManager.state.first())

        authManager.initialize()

        assertEquals(AuthState.LoggedIn("https://active-server.com"), authManager.state.first())
    }

    @Test
    fun `initialize does not overwrite LoggedOut state`() = runTest {
        authManager.connectToServer("https://active-server.com")
        authManager.handleSessionExpired()
        assertEquals(AuthState.LoggedOut("https://active-server.com"), authManager.state.first())

        authManager.initialize()

        assertEquals(AuthState.LoggedOut("https://active-server.com"), authManager.state.first())
    }

    @Test
    fun `testConnection probes health with GET`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.start()

        try {
            val result = authManager.testConnection(server.url("/").toString())
            val request = server.takeRequest()

            assertTrue(result.isSuccess)
            assertEquals("GET", request.method)
            assertEquals("/health", request.path)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `checkAuthStatus treats 401 as password required`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()

        try {
            val result = authManager.checkAuthStatus(server.url("/").toString())
            val request = server.takeRequest()

            assertTrue(result.isSuccess)
            assertEquals("GET", request.method)
            assertEquals("/api/auth/status", request.path)
            assertTrue(result.getOrThrow().isAuthRequired)
            assertTrue(result.getOrThrow().isPasswordOnly)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `login surfaces invalid password response`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"Invalid password\"}")
        )
        server.start()

        try {
            val result = authManager.login(server.url("/").toString(), "wrong")

            assertTrue(result.isFailure)
            assertEquals("Invalid password", authManager.error.first())
        } finally {
            server.shutdown()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun invokeWrapError(exception: Exception): Exception {
        val method = AuthManager::class.java.getDeclaredMethod("wrapError", Exception::class.java)
        method.isAccessible = true
        return method.invoke(authManager, exception) as Exception
    }
}
