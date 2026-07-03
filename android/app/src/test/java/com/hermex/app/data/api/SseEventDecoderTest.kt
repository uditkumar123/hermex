package com.hermex.app.data.api

import com.hermex.app.data.model.SSEEvent
import org.junit.Assert.*
import org.junit.Test

class SseEventDecoderTest {

    @Test
    fun `token event decodes text correctly`() {
        val event = SseEventDecoder.decode("token", """{"text": "Hello, world!"}""")
        assertTrue(event is SSEEvent.Token)
        assertEquals("Hello, world!", (event as SSEEvent.Token).text)
    }

    @Test
    fun `token event with empty data returns empty text`() {
        val event = SseEventDecoder.decode("token", "{}")
        assertTrue(event is SSEEvent.Token)
        assertEquals("", (event as SSEEvent.Token).text)
    }

    @Test
    fun `reasoning event decodes text correctly`() {
        val event = SseEventDecoder.decode("reasoning", """{"text": "Let me think..."}""")
        assertTrue(event is SSEEvent.Reasoning)
        assertEquals("Let me think...", (event as SSEEvent.Reasoning).text)
    }

    @Test
    fun `title event decodes sessionId and title`() {
        val event = SseEventDecoder.decode("title", """{"session_id": "abc123", "title": "My Chat"}""")
        assertTrue(event is SSEEvent.Title)
        assertEquals("abc123", (event as SSEEvent.Title).sessionId)
        assertEquals("My Chat", event.title)
    }

    @Test
    fun `done event decodes usage and session`() {
        val event = SseEventDecoder.decode(
            "done",
            """{"usage": {"context_length": 8192, "last_prompt_tokens": 1024}}"""
        )
        assertTrue(event is SSEEvent.Done)
        assertEquals(8192, (event as SSEEvent.Done).usage?.contextLength)
        assertEquals(1024, event.usage?.lastPromptTokens)
    }

    @Test
    fun `done event with empty data returns transport error`() {
        val event = SseEventDecoder.decode("done", "")
        assertTrue(event is SSEEvent.TransportError)
    }

    @Test
    fun `done event with malformed json returns transport error`() {
        val event = SseEventDecoder.decode("done", "not valid json")
        assertTrue(event is SSEEvent.TransportError)
    }

    @Test
    fun `stream_end event returns StreamEnd`() {
        val event = SseEventDecoder.decode("stream_end", "")
        assertTrue(event is SSEEvent.StreamEnd)
    }

    @Test
    fun `cancel event returns Cancelled`() {
        val event = SseEventDecoder.decode("cancel", "")
        assertTrue(event is SSEEvent.Cancelled)
    }

    @Test
    fun `error event decodes error message`() {
        val event = SseEventDecoder.decode("error", """{"error": "Something went wrong"}""")
        assertTrue(event is SSEEvent.Error)
        assertEquals("Something went wrong", (event as SSEEvent.Error).message)
    }

    @Test
    fun `error event with message field works`() {
        val event = SseEventDecoder.decode("error", """{"message": "An error occurred"}""")
        assertTrue(event is SSEEvent.Error)
        assertEquals("An error occurred", (event as SSEEvent.Error).message)
    }

    @Test
    fun `apperror event decodes as Error`() {
        val event = SseEventDecoder.decode("apperror", """{"error": "App crashed"}""")
        assertTrue(event is SSEEvent.Error)
        assertEquals("App crashed", (event as SSEEvent.Error).message)
    }

    @Test
    fun `approval event decodes correctly`() {
        val json = """{"pending": {"description": "Run this command?"}}"""
        val event = SseEventDecoder.decode("approval", json)
        assertTrue(event is SSEEvent.ApprovalPending)
        assertEquals("Run this command?", (event as SSEEvent.ApprovalPending).response.pending?.displayDescription)
    }

    @Test
    fun `clarify event decodes correctly`() {
        val json = """{"pending": {"question": "Which file?"}}"""
        val event = SseEventDecoder.decode("clarify", json)
        assertTrue(event is SSEEvent.ClarificationPending)
        assertEquals("Which file?", (event as SSEEvent.ClarificationPending).response.pending?.displayQuestion)
    }

    @Test
    fun `tool event with partial data decodes correctly`() {
        val json = """{"call_id": "tool-1", "name": "read_file"}"""
        val event = SseEventDecoder.decode("tool", json)
        assertTrue(event is SSEEvent.ToolStarted)
        assertEquals("tool-1", (event as SSEEvent.ToolStarted).event.stableId)
    }

    @Test
    fun `tool_complete event decodes correctly`() {
        val json = """{"call_id": "tool-1", "name": "read_file", "duration": 2.5}"""
        val event = SseEventDecoder.decode("tool_complete", json)
        assertTrue(event is SSEEvent.ToolCompleted)
        assertEquals(2.5, (event as SSEEvent.ToolCompleted).event.duration)
    }

    @Test
    fun `tool event with empty data returns Ignored`() {
        val event = SseEventDecoder.decode("tool", "")
        assertTrue(event is SSEEvent.Ignored)
    }

    @Test
    fun `unknown event type returns Ignored`() {
        val event = SseEventDecoder.decode("nonexistent_event", """{"data": "test"}""")
        assertTrue(event is SSEEvent.Ignored)
    }

    @Test
    fun `malformed json returns Token with empty text`() {
        val event = SseEventDecoder.decode("token", "not valid json at all {{{")
        assertTrue(event is SSEEvent.Token)
        assertEquals("", (event as SSEEvent.Token).text)
    }

    @Test
    fun `interim_assistant event decodes text`() {
        val event = SseEventDecoder.decode("interim_assistant", """{"text": "Streaming content..."}""")
        assertTrue(event is SSEEvent.InterimAssistant)
        assertEquals("Streaming content...", (event as SSEEvent.InterimAssistant).text)
    }

    @Test
    fun `metering event returns Ignored`() {
        val event = SseEventDecoder.decode("metering", """{"tokens": 1234, "cost": 0.05}""")
        assertTrue(event is SSEEvent.Ignored)
    }

    @Test
    fun `compressed event returns Ignored`() {
        val event = SseEventDecoder.decode("compressed", """{"session_id": "abc"}""")
        assertTrue(event is SSEEvent.Ignored)
    }

    @Test
    fun `compressing event returns Ignored`() {
        val event = SseEventDecoder.decode("compressing", """{"status": "in_progress"}""")
        assertTrue(event is SSEEvent.Ignored)
    }

    @Test
    fun `warning event returns Ignored`() {
        val event = SseEventDecoder.decode("warning", """{"message": "Low memory"}""")
        assertTrue(event is SSEEvent.Ignored)
    }

    @Test
    fun `context_status event returns Ignored`() {
        val event = SseEventDecoder.decode("context_status", """{"context_length": 4096}""")
        assertTrue(event is SSEEvent.Ignored)
    }

    @Test
    fun `todo_state event returns Ignored`() {
        val event = SseEventDecoder.decode("todo_state", """{"todos": ["task1"]}""")
        assertTrue(event is SSEEvent.Ignored)
    }

    @Test
    fun `goal event returns Ignored`() {
        val event = SseEventDecoder.decode("goal", """{"goal": "Complete the task"}""")
        assertTrue(event is SSEEvent.Ignored)
    }

    @Test
    fun `goal_continue event returns Ignored`() {
        val event = SseEventDecoder.decode("goal_continue", """{"goal": "Continue", "status": "in_progress"}""")
        assertTrue(event is SSEEvent.Ignored)
    }

    @Test
    fun `pending_steer_leftover event decodes text`() {
        val event = SseEventDecoder.decode("pending_steer_leftover", """{"text": "leftover steer"}""")
        assertTrue(event is SSEEvent.PendingSteerLeftover)
        assertEquals("leftover steer", (event as SSEEvent.PendingSteerLeftover).text)
    }

    @Test
    fun `token event with null text returns empty string`() {
        val event = SseEventDecoder.decode("token", "{}")
        assertTrue(event is SSEEvent.Token)
        assertEquals("", (event as SSEEvent.Token).text)
    }
}
