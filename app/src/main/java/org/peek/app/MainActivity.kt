package org.peek.app

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.peek.app.intents.IntentUrlResolver
import org.peek.app.ui.UnfurlitApp
import org.peek.app.ui.UnfurlitViewModel
import org.peek.app.ui.history.HistoryViewModel
import org.peek.app.ui.viewer.ViewerViewModel

class MainActivity : ComponentActivity() {
    private val unfurlitViewModel: UnfurlitViewModel by viewModels()
    private val viewerViewModel: ViewerViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            UnfurlitApp(
                unfurlitViewModel = unfurlitViewModel,
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // uiMode is handled by this activity so playback is not interrupted.
        // Reapply edge-to-edge to update system-bar icon contrast immediately.
        enableEdgeToEdge()
    }

    private fun handleIntent(intent: Intent?) {
        IntentUrlResolver.resolve(intent)?.let { url ->
            viewerViewModel.open(url)
            unfurlitViewModel.showViewer()
        }
    }
}
