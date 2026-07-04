package com.hermex.app.ui.workspace

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MemoryViewModelTest {

    private lateinit var viewModel: MemoryViewModel

    @Before
    fun setUp() {
        viewModel = MemoryViewModel(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `initial state has null fields`() = runTest {
        val state = viewModel.uiState.first()
        assertNull(state.notes)
        assertNull(state.profile)
        assertNull(state.sessionNotes)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(0, state.selectedTab)
    }

    @Test
    fun `selectTab updates selected tab`() = runTest {
        viewModel.selectTab(1)
        assertEquals(1, viewModel.uiState.first().selectedTab)
    }

    @Test
    fun `selectTab switches to different tabs`() = runTest {
        viewModel.selectTab(0)
        assertEquals(0, viewModel.uiState.first().selectedTab)
        viewModel.selectTab(2)
        assertEquals(2, viewModel.uiState.first().selectedTab)
        viewModel.selectTab(1)
        assertEquals(1, viewModel.uiState.first().selectedTab)
    }

    @Test
    fun `clearError sets error to null`() = runTest {
        viewModel.clearError()
        assertNull(viewModel.uiState.first().errorMessage)
    }
}
