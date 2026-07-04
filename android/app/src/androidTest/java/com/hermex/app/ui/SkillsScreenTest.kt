package com.hermex.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.hermex.app.ui.workspace.SkillsScreen
import org.junit.Rule
import org.junit.Test

class SkillsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun skillsScreen_showsTitleAndSearch() {
        composeTestRule.setContent {
            SkillsScreen(onBack = {})
        }

        composeTestRule.onNodeWithText("Skills").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search skills...").assertIsDisplayed()
    }
}
