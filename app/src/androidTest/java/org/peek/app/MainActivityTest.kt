package org.peek.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun coldLaunchStaysOnTheIdleHomeScreen() {
        composeRule.onNodeWithText("Open social media").assertIsDisplayed()
        composeRule.onNodeWithText("Extracting stream information…").assertDoesNotExist()
        composeRule.onNodeWithText("Big Buck Bunny", substring = true).assertDoesNotExist()
    }
}
