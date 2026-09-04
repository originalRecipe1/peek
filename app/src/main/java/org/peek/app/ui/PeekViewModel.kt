package org.peek.app.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PeekViewModel : ViewModel() {
    private val _destination = MutableStateFlow(PeekDestination.Home)
    val destination: StateFlow<PeekDestination> = _destination.asStateFlow()
    private var historyReturnDestination = PeekDestination.Home

    fun showHome() {
        _destination.value = PeekDestination.Home
    }

    fun showViewer() {
        _destination.value = PeekDestination.Viewer
    }

    fun showHistory() {
        historyReturnDestination = when (_destination.value) {
            PeekDestination.Viewer -> PeekDestination.Viewer
            else -> PeekDestination.Home
        }
        _destination.value = PeekDestination.History
    }

    fun leaveHistory() {
        _destination.value = historyReturnDestination
    }
}

enum class PeekDestination {
    Home,
    Viewer,
    History,
}
