package io.github.originalrecipe1.unfurlit.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import io.github.originalrecipe1.unfurlit.ui.theme.UnfurlitTheme

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun startsIdleAndOpensTheFirstPublicUrl() {
        var openedUrl: String? = null
        composeRule.setContent {
            UnfurlitTheme {
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
            UnfurlitTheme {
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
            UnfurlitTheme {
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
