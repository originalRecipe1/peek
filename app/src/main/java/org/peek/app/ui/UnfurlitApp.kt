package org.peek.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.peek.app.ui.components.HistoryPager
import org.peek.app.ui.home.HomeScreen
import org.peek.app.ui.history.HistoryRoute
import org.peek.app.ui.history.HistoryViewModel
import org.peek.app.ui.theme.UnfurlitTheme
import org.peek.app.ui.viewer.ViewerRoute
import org.peek.app.ui.viewer.ViewerViewModel

@Composable
fun UnfurlitApp(
    unfurlitViewModel: UnfurlitViewModel,
    viewerViewModel: ViewerViewModel,
    historyViewModel: HistoryViewModel,
) {
    val destination by unfurlitViewModel.destination.collectAsStateWithLifecycle()
    val underlyingDestination = if (destination == UnfurlitDestination.History) {
        unfurlitViewModel.historyReturnDestination
    } else {
        destination
    }
    UnfurlitTheme {
        HistoryPager(
            historyVisible = destination == UnfurlitDestination.History,
            allowOpenSwipe = underlyingDestination == UnfurlitDestination.Home,
            onHistoryVisibilityChange = { visible ->
                if (visible && destination != UnfurlitDestination.History) unfurlitViewModel.showHistory()
                if (!visible && destination == UnfurlitDestination.History) unfurlitViewModel.leaveHistory()
            },
            history = {
                HistoryRoute(
                    viewModel = historyViewModel,
                    onBack = unfurlitViewModel::leaveHistory,
                    onOpen = { entry ->
                        viewerViewModel.open(entry.sourceUrl)
                        unfurlitViewModel.showViewer()
                    },
                )
            },
        ) { visible ->
            when (underlyingDestination) {
                UnfurlitDestination.Home -> HomeScreen(
                    onOpen = { url ->
                        viewerViewModel.open(url)
                        unfurlitViewModel.showViewer()
                    },
                    onShowHistory = unfurlitViewModel::showHistory,
                )

                UnfurlitDestination.Viewer -> if (visible) ViewerRoute(
                    viewModel = viewerViewModel,
                    onBack = {
                        viewerViewModel.cancel()
                        unfurlitViewModel.showHome()
                    },
                    onShowHistory = unfurlitViewModel::showHistory,
                )

                UnfurlitDestination.History -> Unit
            }
        }
    }
}
