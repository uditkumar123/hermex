package com.hermex.app.ui.sessionlist

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionListViewModelTest {

    private lateinit var viewModel: SessionListViewModel

    @Before
    fun setUp() {
        viewModel = SessionListViewModel(
            ApplicationProvider.getApplicationContext()
        )
    }

    @Test
    fun `initial state has empty sessions`() = runTest {
        val state = viewModel.uiState.first()
        assertTrue(state.sessions.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `search updates search query`() = runTest {
        viewModel.search("test query")
        assertEquals("test query", viewModel.uiState.first().searchQuery)
    }

    @Test
    fun `search with blank resets`() = runTest {
        viewModel.search("initial")
        viewModel.search("")
        assertEquals("", viewModel.uiState.first().searchQuery)
    }

    @Test
    fun `clearError sets error to null`() = runTest {
        viewModel.clearError()
        assertNull(viewModel.uiState.first().errorMessage)
    }

    @Test
    fun `filterByProject sets project ID`() = runTest {
        viewModel.filterByProject("project-123")
        assertEquals("project-123", viewModel.uiState.first().selectedProjectId)
    }

    @Test
    fun `filterByProject with null clears filter`() = runTest {
        viewModel.filterByProject("project-123")
        viewModel.filterByProject(null)
        assertNull(viewModel.uiState.first().selectedProjectId)
    }

    @Test
    fun `filteredSessions returns empty when sessions empty`() {
        assertEquals(0, viewModel.filteredSessions().size)
    }

    @Test
    fun `sectionedSessions returns empty when no sessions`() {
        assertEquals(0, viewModel.sectionedSessions().size)
    }

    @Test
    fun `initial profiles are empty`() = runTest {
        assertTrue(viewModel.uiState.first().profiles.isEmpty())
    }

    @Test
    fun `initial projects are empty`() = runTest {
        assertTrue(viewModel.uiState.first().projects.isEmpty())
    }

    @Test
    fun `initial offline mode is false`() = runTest {
        assertFalse(viewModel.uiState.first().isOfflineMode)
    }

    @Test
    fun `initial isCreatingSession is false`() = runTest {
        assertFalse(viewModel.uiState.first().isCreatingSession)
    }

    @Test
    fun `initial search query is empty`() = runTest {
        assertEquals("", viewModel.uiState.first().searchQuery)
    }

    @Test
    fun `initial selectedProjectId is null`() = runTest {
        assertNull(viewModel.uiState.first().selectedProjectId)
    }

    @Test
    fun `initial activeProfile is null`() = runTest {
        assertNull(viewModel.uiState.first().activeProfile)
    }
}
