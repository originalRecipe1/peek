package org.peek.app.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnfurlitViewModel : ViewModel() {
    private val _destination = MutableStateFlow(UnfurlitDestination.Home)
    val destination: StateFlow<UnfurlitDestination> = _destination.asStateFlow()
    var historyReturnDestination = UnfurlitDestination.Home
        private set

    fun showHome() {
        _destination.value = UnfurlitDestination.Home
    }

    fun showViewer() {
        _destination.value = UnfurlitDestination.Viewer
    }

    fun showHistory() {
        if (_destination.value == UnfurlitDestination.History) return
        historyReturnDestination = when (_destination.value) {
            UnfurlitDestination.Viewer -> UnfurlitDestination.Viewer
            else -> UnfurlitDestination.Home
        }
        _destination.value = UnfurlitDestination.History
    }

    fun leaveHistory() {
        _destination.value = historyReturnDestination
    }
}

enum class UnfurlitDestination {
    Home,
    Viewer,
    History,
}
