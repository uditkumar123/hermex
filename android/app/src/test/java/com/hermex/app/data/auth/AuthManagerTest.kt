package com.hermex.app.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hermex.app.data.model.APIError
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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

    @Suppress("UNCHECKED_CAST")
    private fun invokeWrapError(exception: Exception): Exception {
        val method = AuthManager::class.java.getDeclaredMethod("wrapError", Exception::class.java)
        method.isAccessible = true
        return method.invoke(authManager, exception) as Exception
    }
}
