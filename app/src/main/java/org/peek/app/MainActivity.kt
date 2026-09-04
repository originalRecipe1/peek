package org.peek.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.peek.app.intents.IntentUrlResolver
import org.peek.app.ui.PeekApp
import org.peek.app.ui.PeekViewModel
import org.peek.app.ui.history.HistoryViewModel
import org.peek.app.ui.viewer.ViewerViewModel

class MainActivity : ComponentActivity() {
    private val peekViewModel: PeekViewModel by viewModels()
    private val viewerViewModel: ViewerViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            PeekApp(
                peekViewModel = peekViewModel,
                viewerViewModel = viewerViewModel,
                historyViewModel = historyViewModel,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        IntentUrlResolver.resolve(intent)?.let { url ->
            viewerViewModel.open(url)
            peekViewModel.showViewer()
        }
    }
}
