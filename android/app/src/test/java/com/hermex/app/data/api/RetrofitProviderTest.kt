package com.hermex.app.data.api

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RetrofitProviderTest {

    @Test
    fun `createOkHttpClient invokes unauthorized callback on 401`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()
        val previous = RetrofitProvider.onUnauthorized
        var wasCalled = false

        try {
            RetrofitProvider.onUnauthorized = { wasCalled = true }
            val client = RetrofitProvider.createOkHttpClient(
                context = ApplicationProvider.getApplicationContext(),
                readTimeoutSeconds = 10
            )
            val request = Request.Builder()
                .url(server.url("/api/stream"))
                .header("Accept", "text/event-stream")
                .build()

            client.newCall(request).execute().close()

            assertTrue(wasCalled)
            assertEquals("text/event-stream", server.takeRequest().getHeader("Accept"))
        } finally {
            RetrofitProvider.onUnauthorized = previous
            RetrofitProvider.invalidate()
            server.shutdown()
        }
    }

    @Test
    fun `login 401 does not invoke unauthorized callback`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401).setBody("{\"error\":\"Invalid password\"}"))
        server.start()
        val previous = RetrofitProvider.onUnauthorized
        var wasCalled = false

        try {
            RetrofitProvider.onUnauthorized = { wasCalled = true }
            val api = RetrofitProvider.createApi(
                server.url("/").toString(),
                ApplicationProvider.getApplicationContext()
            )

            runCatching { api.login(LoginRequest("wrong")) }

            assertEquals(false, wasCalled)
        } finally {
            RetrofitProvider.onUnauthorized = previous
            RetrofitProvider.invalidate()
            server.shutdown()
        }
    }
}
