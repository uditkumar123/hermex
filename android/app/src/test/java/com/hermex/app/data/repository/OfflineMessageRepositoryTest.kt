package com.hermex.app.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hermex.app.data.model.ChatMessage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineMessageRepositoryTest {

    private lateinit var repository: OfflineMessageRepository

    private val sessionId = "offline-message-repository-test"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository = OfflineMessageRepository(context)
        runTest { repository.deleteMessages(sessionId) }
    }

    @Test
    fun `cacheMessages replaces prior snapshot and keeps latest fifty messages`() = runTest {
        val messages = (0 until 60).map { index ->
            ChatMessage(
                role = "assistant",
                content = "message-$index",
                timestamp = index.toDouble(),
                messageId = "message-$index",
                toolCalls = if (index == 59) listOf(JsonPrimitive("tool-call")) else null
            )
        }

        repository.cacheMessages(sessionId, messages)
        repository.cacheMessages(sessionId, messages)

        val cached = repository.getMessages(sessionId)

        assertEquals(50, cached.size)
        assertEquals("message-10", cached.first().messageId)
        assertEquals("message-59", cached.last().messageId)
        assertNotNull(cached.last().toolCalls)
        assertEquals(JsonPrimitive("tool-call"), cached.last().toolCalls!!.first())
    }
}
