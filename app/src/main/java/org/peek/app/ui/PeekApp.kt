package org.peek.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.peek.app.ui.components.HistoryPager
import org.peek.app.ui.home.HomeScreen
import org.peek.app.ui.history.HistoryRoute
import org.peek.app.ui.history.HistoryViewModel
import org.peek.app.ui.theme.PeekTheme
import org.peek.app.ui.viewer.ViewerRoute
import org.peek.app.ui.viewer.ViewerViewModel

@Composable
fun PeekApp(
    peekViewModel: PeekViewModel,
    viewerViewModel: ViewerViewModel,
    historyViewModel: HistoryViewModel,
) {
    val destination by peekViewModel.destination.collectAsStateWithLifecycle()
    val underlyingDestination = if (destination == PeekDestination.History) {
        peekViewModel.historyReturnDestination
    } else {
        destination
    }
    PeekTheme {
        HistoryPager(
            historyVisible = destination == PeekDestination.History,
            allowOpenSwipe = underlyingDestination == PeekDestination.Home,
            onHistoryVisibilityChange = { visible ->
                if (visible && destination != PeekDestination.History) peekViewModel.showHistory()
                if (!visible && destination == PeekDestination.History) peekViewModel.leaveHistory()
            },
            history = {
                HistoryRoute(
                    viewModel = historyViewModel,
                    onBack = peekViewModel::leaveHistory,
                    onOpen = { entry ->
                        viewerViewModel.open(entry.sourceUrl)
                        peekViewModel.showViewer()
                    },
                )
            },
        ) { visible ->
            when (underlyingDestination) {
                PeekDestination.Home -> HomeScreen(
                    onOpen = { url ->
                        viewerViewModel.open(url)
                        peekViewModel.showViewer()
                    },
                    onShowHistory = peekViewModel::showHistory,
                )

                PeekDestination.Viewer -> if (visible) ViewerRoute(
                    viewModel = viewerViewModel,
                    onBack = {
                        viewerViewModel.cancel()
                        peekViewModel.showHome()
                    },
                    onShowHistory = peekViewModel::showHistory,
                )

                PeekDestination.History -> Unit
            }
        }
    }
}
