package org.peek.app.ui.viewer

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import org.peek.app.BuildConfig
import org.peek.app.domain.model.userMessage

@Composable
fun ViewerRoute(
    viewModel: ViewerViewModel,
    onBack: () -> Unit,
    onShowHistory: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    ViewerScreen(
        state = state,
        onRetry = viewModel::retry,
        onViewed = viewModel::recordView,
        onBack = onBack,
        onShowHistory = onShowHistory,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun ViewerScreen(
    state: ViewerState,
    onRetry: () -> Unit,
    onViewed: (org.peek.app.domain.model.ExtractionResult) -> Unit,
    onBack: () -> Unit,
    onShowHistory: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.sizeIn(minHeight = 48.dp),
                    ) {
                        Text("Home")
                    }
                },
                title = {
                    Column {
                        Text("Peek")
                        Text(
                            text = "Streaming experiment",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onShowHistory,
                        modifier = Modifier.sizeIn(minHeight = 48.dp),
                    ) {
                        Text("History")
                    }
                },
            )
        },
    ) { contentPadding ->
        when (state) {
            ViewerState.Idle -> IdleContent(
                onBack = onBack,
                modifier = Modifier.padding(contentPadding),
            )

            is ViewerState.Loading -> LoadingContent(
                modifier = Modifier.padding(contentPadding),
            )

            is ViewerState.Failed -> FailureContent(
                message = state.error.userMessage,
                engineVersion = BuildConfig.YT_DLP_ENGINE_VERSION,
                onRetry = onRetry,
                onOpenOriginal = {
                    context.openOriginal(state.sourceUrl)
                },
                modifier = Modifier.padding(contentPadding),
            )

            is ViewerState.Ready -> Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                MediaViewer(
                    extraction = state.extraction,
                    onRetry = onRetry,
                    onViewed = { onViewed(state.extraction) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = state.extraction.platform ?: "Media",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = state.extraction.title ?: "Untitled media",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    state.extraction.author?.let { author ->
                        Text(
                            text = author,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.extraction.description?.let { description ->
                        Text(
                            text = description,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = {
                            context.openOriginal(state.sourceUrl)
                        },
                        modifier = Modifier.sizeIn(minHeight = 48.dp),
                    ) {
                        Text("Open original")
                    }
                }
            }
        }
    }
}

private fun Context.openOriginal(url: String) {
    val browserIntent = Intent.makeMainSelectorActivity(
        Intent.ACTION_MAIN,
        Intent.CATEGORY_APP_BROWSER,
    ).apply {
        data = url.toUri()
    }
    runCatching { startActivity(browserIntent) }
}

@Composable
private fun IdleContent(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Choose a link from Home to open media.",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.sizeIn(minHeight = 48.dp),
            ) {
                Text("Go home")
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Extracting stream information…",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "No media file is being saved",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun FailureContent(
    message: String,
    engineVersion: String,
    onRetry: () -> Unit,
    onOpenOriginal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "yt-dlp $engineVersion",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                ) {
                    Text("Retry")
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = onOpenOriginal,
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                ) {
                    Text("Open original")
                }
            }
        }
    }
}
