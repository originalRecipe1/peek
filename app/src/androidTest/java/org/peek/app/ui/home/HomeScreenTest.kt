package org.peek.app.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.peek.app.ui.theme.PeekTheme

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rightSwipeOpensHistoryOnce() {
        var historyRequests = 0
        composeRule.setContent {
            PeekTheme {
                HomeScreen(onOpen = {}, onShowHistory = { historyRequests++ })
            }
        }

        composeRule.onRoot().performTouchInput {
            swipe(Offset(width * 0.2f, height * 0.8f), Offset(width * 0.8f, height * 0.8f))
        }

        composeRule.runOnIdle { assertEquals(1, historyRequests) }
    }

    @Test
    fun otherDirectionsAndRepeatedShortSwipesDoNotOpenHistory() {
        var historyRequests = 0
        composeRule.setContent {
            PeekTheme {
                HomeScreen(onOpen = {}, onShowHistory = { historyRequests++ })
            }
        }

        composeRule.onRoot().performTouchInput {
            swipe(Offset(width * 0.8f, height * 0.8f), Offset(width * 0.2f, height * 0.8f))
            swipe(Offset(width * 0.5f, height * 0.9f), Offset(width * 0.5f, height * 0.7f))
            repeat(5) {
                swipe(Offset(width * 0.3f, height * 0.8f), Offset(width * 0.35f, height * 0.8f))
            }
        }

        composeRule.runOnIdle { assertEquals(0, historyRequests) }
    }

    @Test
    fun cancelledRightSwipeDoesNotOpenHistory() {
        var historyRequests = 0
        composeRule.setContent {
            PeekTheme {
                HomeScreen(onOpen = {}, onShowHistory = { historyRequests++ })
            }
        }

        composeRule.onRoot().performTouchInput {
            down(Offset(width * 0.2f, height * 0.8f))
            moveTo(Offset(width * 0.8f, height * 0.8f))
            cancel()
        }

        composeRule.runOnIdle { assertEquals(0, historyRequests) }
    }

    @Test
    fun startsIdleAndOpensTheFirstPublicUrl() {
        var openedUrl: String? = null
        composeRule.setContent {
            PeekTheme {
                HomeScreen(
                    onOpen = { openedUrl = it },
                    onShowHistory = {},
                )
            }
        }

        composeRule.onNodeWithText("Ready when\nyou are.").assertIsDisplayed()
        composeRule.onNodeWithText("Open").assertIsNotEnabled()
        composeRule.onNode(hasSetTextAction()).performTextInput(
            "A message with https://example.com/media?item=1 inside",
        )
        composeRule.onNodeWithText("Open").performClick()

        composeRule.runOnIdle {
            assertEquals("https://example.com/media?item=1", openedUrl)
        }
    }

    @Test
    fun reportsAnInvalidSchemeWithoutOpeningIt() {
        var opened = false
        composeRule.setContent {
            PeekTheme {
                HomeScreen(
                    onOpen = { opened = true },
                    onShowHistory = {},
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("file:///sdcard/media.mp4")
        composeRule.onNodeWithText("Open").performClick()

        composeRule.onNodeWithText("Enter a valid public HTTP or HTTPS URL.")
            .assertIsDisplayed()
        composeRule.runOnIdle { assertTrue(!opened) }
    }

    @Test
    fun opensHistoryOnlyAfterTheUserRequestsIt() {
        var historyRequested = false
        composeRule.setContent {
            PeekTheme {
                HomeScreen(
                    onOpen = {},
                    onShowHistory = { historyRequested = true },
                )
            }
        }

        composeRule.runOnIdle { assertTrue(!historyRequested) }
        composeRule.onNodeWithContentDescription("Open history").performClick()
        composeRule.runOnIdle { assertTrue(historyRequested) }
    }
}
