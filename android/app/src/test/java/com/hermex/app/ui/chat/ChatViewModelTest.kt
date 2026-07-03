package com.hermex.app.ui.chat

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatViewModelTest {

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        viewModel = ChatViewModel(
            ApplicationProvider.getApplicationContext(),
            "test-session-id"
        )
    }

    @Test
    fun `initial state has empty messages`() = runTest {
        val state = viewModel.uiState.first()
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isStreaming)
        assertFalse(state.isSending)
    }

    @Test
    fun `initial title is Chat`() = runTest {
        assertEquals("Chat", viewModel.uiState.first().title)
    }

    @Test
    fun `initial error is null`() = runTest {
        assertNull(viewModel.uiState.first().errorMessage)
    }

    @Test
    fun `clearError sets error to null`() = runTest {
        viewModel.clearError()
        assertNull(viewModel.uiState.first().errorMessage)
    }

    @Test
    fun `dismissApproval removes approval pending`() = runTest {
        viewModel.dismissApproval()
        assertNull(viewModel.uiState.first().approvalPending)
    }

    @Test
    fun `dismissClarification removes clarification pending`() = runTest {
        viewModel.dismissClarification()
        assertNull(viewModel.uiState.first().clarificationPending)
    }

    @Test
    fun `initial liveReasoningText is empty`() = runTest {
        assertEquals("", viewModel.uiState.first().liveReasoningText)
    }

    @Test
    fun `initial liveToolCalls is empty`() = runTest {
        assertTrue(viewModel.uiState.first().liveToolCalls.isEmpty())
    }

    @Test
    fun `initial activeStreamId is null`() = runTest {
        assertNull(viewModel.uiState.first().activeStreamId)
    }

    @Test
    fun `initial contextWindow is null`() = runTest {
        assertNull(viewModel.uiState.first().contextWindow)
    }

    @Test
    fun `initial approvalPending is null`() = runTest {
        assertNull(viewModel.uiState.first().approvalPending)
    }

    @Test
    fun `initial clarificationPending is null`() = runTest {
        assertNull(viewModel.uiState.first().clarificationPending)
    }

    @Test
    fun `cancelStream on no active stream does nothing`() = runTest {
        viewModel.cancelStream()
    }

    @Test
    fun `steerChat with no active stream does nothing`() = runTest {
        viewModel.steerChat("steer text")
    }
}
