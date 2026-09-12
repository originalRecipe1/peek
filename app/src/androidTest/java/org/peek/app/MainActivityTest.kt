package org.peek.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun coldLaunchStaysOnTheIdleHomeScreen() {
        composeRule.onNodeWithText("Ready when\nyou are.").assertIsDisplayed()
        composeRule.onNodeWithText("Extracting stream information…").assertDoesNotExist()
        composeRule.onNodeWithText("Big Buck Bunny", substring = true).assertDoesNotExist()
    }

    @Test
    fun rightSwipeNavigatesFromHomeToHistory() {
        composeRule.onNodeWithText("Ready when\nyou are.").assertIsDisplayed()
        composeRule.onRoot().performTouchInput {
            swipe(Offset(width * 0.2f, height * 0.8f), Offset(width * 0.8f, height * 0.8f))
        }
        composeRule.onNodeWithText("History").assertIsDisplayed()
    }

    @Test
    fun shortRightSwipeAcrossTheTopBarOpensHistory() {
        val start = composeRule.onNodeWithText("Unfurlit").fetchSemanticsNode().boundsInRoot.center
        val distance = 64f * composeRule.activity.resources.displayMetrics.density
        composeRule.onRoot().performTouchInput {
            swipe(start, start + Offset(distance, 0f))
        }
        composeRule.onNodeWithText("History").assertIsDisplayed()
    }

    @Test
    fun shortDiagonalRightSwipeOpensHistoryWhenHomeFitsOnScreen() {
        val density = composeRule.activity.resources.displayMetrics.density
        composeRule.onRoot().performTouchInput {
            val start = Offset(width * 0.3f, height * 0.8f)
            down(start)
            // A thumb can initially drift vertically before moving right.
            moveTo(start + Offset(3f, 24f) * density, delayMillis = 80)
            moveTo(start + Offset(68f, 30f) * density, delayMillis = 160)
            up()
        }
        composeRule.onNodeWithText("History").assertIsDisplayed()
    }
}
