package org.peek.app.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.peek.app.ui.theme.PeekTheme

class PeekTopAppBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsCompactBrandingAndAnAccessibleHistoryAction() {
        var historyRequested = false
        composeRule.setContent {
            PeekTheme {
                PeekTopAppBar(onShowHistory = { historyRequested = true })
            }
        }

        composeRule.onNodeWithText("Peek").assertIsDisplayed()
        composeRule.onNodeWithText("Home").assertDoesNotExist()
        composeRule.onNodeWithText("Streaming experiment").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Open history").performClick()
        composeRule.runOnIdle { assertTrue(historyRequested) }
    }
}
