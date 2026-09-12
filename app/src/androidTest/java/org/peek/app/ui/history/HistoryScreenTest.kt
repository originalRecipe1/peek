package org.peek.app.ui.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.peek.app.domain.model.HistoryEntry
import org.peek.app.domain.model.HistoryMediaKind

class HistoryScreenTest {
    @get:Rule val composeRule = createComposeRule()
    private val entry = HistoryEntry(7, "https://example.com/watch", "Example", "An afternoon outside", "A creator", HistoryMediaKind.Video, 1, 62, System.currentTimeMillis())

    @Test
    fun rowOpensMediaAndRemoveIconDeletesOnlyTheSelectedVisit() {
        var opened: Long? = null
        var removed: Long? = null
        composeRule.setContent {
            MaterialTheme { HistoryScreen(HistoryState.Ready(listOf(entry)), {}, { opened = it.id }, { removed = it }, {}) }
        }
        composeRule.onNodeWithText("Today").assertIsDisplayed()
        composeRule.onNodeWithText(entry.title!!).performClick()
        composeRule.runOnIdle { assertEquals(7L, opened) }
        composeRule.runOnIdle { opened = null }
        composeRule.onNodeWithContentDescription("Remove ${entry.title} from history").performClick()
        composeRule.runOnIdle { assertEquals(null, opened) }
        composeRule.runOnIdle { assertEquals(7L, removed) }
    }

    @Test
    fun clearRequiresConfirmationAndCancelPreservesHistory() {
        var cleared = 0
        composeRule.setContent {
            MaterialTheme { HistoryScreen(HistoryState.Ready(listOf(entry)), {}, {}, {}, { cleared++ }) }
        }
        composeRule.onNodeWithContentDescription("Clear history").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.runOnIdle { assertEquals(0, cleared) }
        composeRule.onNodeWithContentDescription("Clear history").performClick()
        composeRule.onNodeWithText("Clear history").performClick()
        composeRule.runOnIdle { assertEquals(1, cleared) }
    }

    @Test
    fun emptyHistoryOffersBackWithoutDestructiveActions() {
        var wentBack = false
        composeRule.setContent {
            MaterialTheme { HistoryScreen(HistoryState.Ready(emptyList()), { wentBack = true }, {}, {}, {}) }
        }
        composeRule.onNodeWithText("A little rewind").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear history").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.runOnIdle { assertEquals(true, wentBack) }
    }
}
