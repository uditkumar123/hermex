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
class SkillsViewModelTest {

    private lateinit var viewModel: SkillsViewModel

    @Before
    fun setUp() {
        viewModel = SkillsViewModel(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `initial state has empty skills`() = runTest {
        val state = viewModel.uiState.first()
        assertTrue(state.skills.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.selectedSkill)
    }

    @Test
    fun `setSearchQuery updates query`() = runTest {
        viewModel.setSearchQuery("python")
        assertEquals("python", viewModel.uiState.first().searchQuery)
    }

    @Test
    fun `setSearchQuery with empty resets`() = runTest {
        viewModel.setSearchQuery("python")
        viewModel.setSearchQuery("")
        assertEquals("", viewModel.uiState.first().searchQuery)
    }

    @Test
    fun `selectSkill with null clears selection`() = runTest {
        viewModel.selectSkill(null)
        val state = viewModel.uiState.first()
        assertNull(state.selectedSkill)
        assertNull(state.skillContent)
    }

    @Test
    fun `selectSkill sets selected and loads content`() = runTest {
        val skill = SkillItem("test-skill", "A test skill")
        viewModel.selectSkill(skill)
        val state = viewModel.uiState.first()
        assertEquals(skill, state.selectedSkill)
    }

    @Test
    fun `selectSkill with new skill clears previous content`() = runTest {
        viewModel.selectSkill(SkillItem("first"))
        viewModel.selectSkill(SkillItem("second"))
        val state = viewModel.uiState.first()
        assertEquals("second", state.selectedSkill?.name)
    }

    @Test
    fun `clearError sets error to null`() = runTest {
        viewModel.clearError()
        assertNull(viewModel.uiState.first().errorMessage)
    }
}
