package org.peek.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PeekViewModelTest {
    @Test
    fun repeatedHistoryNotificationsPreserveTheViewerReturnDestination() {
        val model = PeekViewModel()
        model.showViewer()
        model.showHistory()
        model.showHistory()
        model.leaveHistory()
        assertEquals(PeekDestination.Viewer, model.destination.value)
    }

    @Test
    fun historyOpenedFromHomeReturnsHomeAfterAnEarlierViewerVisit() {
        val model = PeekViewModel()
        model.showViewer()
        model.showHistory()
        model.leaveHistory()
        model.showHome()
        model.showHistory()
        model.leaveHistory()
        assertEquals(PeekDestination.Home, model.destination.value)
    }
}
