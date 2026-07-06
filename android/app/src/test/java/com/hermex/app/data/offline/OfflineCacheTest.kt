package com.hermex.app.data.offline

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hermex.app.data.model.SessionSummary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineCacheTest {

    private lateinit var cache: OfflineCache

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        cache = OfflineCache(context)
        runTest { cache.clearCache() }
    }

    @Test
    fun `cached sessions are scoped to matching server`() = runTest {
        cache.cacheSessions(
            listOf(SessionSummary(sessionId = "session-1", title = "Cached")),
            "https://server-a.example.com/"
        )

        val matching = cache.getCachedSessionsForServer("https://server-a.example.com")
        val other = cache.getCachedSessionsForServer("https://server-b.example.com")

        assertEquals(1, matching.size)
        assertEquals("session-1", matching.first().sessionId)
        assertTrue(other.isEmpty())
    }
}
