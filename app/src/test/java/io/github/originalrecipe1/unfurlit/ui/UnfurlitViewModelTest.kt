package io.github.originalrecipe1.unfurlit.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UnfurlitViewModelTest {
    @Test
    fun repeatedHistoryNotificationsPreserveTheViewerReturnDestination() {
        val model = UnfurlitViewModel()
        model.showViewer()
        model.showHistory()
        model.showHistory()
        model.leaveHistory()
        assertEquals(UnfurlitDestination.Viewer, model.destination.value)
    }

    @Test
    fun historyOpenedFromHomeReturnsHomeAfterAnEarlierViewerVisit() {
        val model = UnfurlitViewModel()
        model.showViewer()
        model.showHistory()
        model.leaveHistory()
        model.showHome()
        model.showHistory()
        model.leaveHistory()
        assertEquals(UnfurlitDestination.Home, model.destination.value)
    }
}
