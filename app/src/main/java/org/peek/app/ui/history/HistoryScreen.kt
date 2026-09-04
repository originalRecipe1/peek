package org.peek.app.ui.history

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.peek.app.domain.model.HistoryEntry
import org.peek.app.domain.model.HistoryMediaKind
import java.net.URI

@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onOpen: (HistoryEntry) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    HistoryScreen(
        state = state,
        onBack = onBack,
        onOpen = onOpen,
        onRemove = viewModel::remove,
        onClear = viewModel::clear,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(
    state: HistoryState,
    onBack: () -> Unit,
    onOpen: (HistoryEntry) -> Unit,
    onRemove: (Long) -> Unit,
    onClear: () -> Unit,
) {
    val entries = (state as? HistoryState.Ready)?.entries.orEmpty()
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.sizeIn(minHeight = 48.dp),
                    ) {
                        Text("Back")
                    }
                },
                actions = {
                    if (entries.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearConfirmation = true },
                            modifier = Modifier.sizeIn(minHeight = 48.dp),
                        ) {
                            Text("Clear")
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        when (state) {
            HistoryState.Loading -> HistoryMessage(
                message = "Loading history…",
                showProgress = true,
                modifier = Modifier.padding(contentPadding),
            )

            HistoryState.Failed -> HistoryMessage(
                message = "History could not be loaded.",
                modifier = Modifier.padding(contentPadding),
            )

            is HistoryState.Ready -> if (state.entries.isEmpty()) {
                HistoryMessage(
                    message = "Media you watch will appear here.",
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(
                        items = state.entries,
                        key = HistoryEntry::id,
                    ) { entry ->
                        HistoryRow(
                            entry = entry,
                            onOpen = { onOpen(entry) },
                            onRemove = { onRemove(entry.id) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear viewing history?") },
            text = { Text("This removes every locally stored history entry.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClear()
                    },
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val viewedAt = remember(entry.viewedAtEpochMillis) {
        DateUtils.getRelativeDateTimeString(
            context,
            entry.viewedAtEpochMillis,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            DateUtils.FORMAT_SHOW_TIME,
        ).toString()
    }
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        overlineContent = {
            Text(
                text = entry.platform ?: "Media",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        headlineContent = {
            Text(
                text = entry.title ?: entry.sourceHost() ?: "Untitled media",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                entry.author?.let { author ->
                    Text(
                        text = author,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "${entry.mediaDescription()} · $viewedAt",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            TextButton(
                onClick = onRemove,
                modifier = Modifier.sizeIn(minHeight = 48.dp),
            ) {
                Text("Remove")
            }
        },
    )
}

@Composable
private fun HistoryMessage(
    message: String,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (showProgress) CircularProgressIndicator()
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun HistoryEntry.mediaDescription(): String {
    val media = when {
        mediaCount > 1 -> "$mediaCount items"
        mediaKind == HistoryMediaKind.Video -> "Video"
        mediaKind == HistoryMediaKind.Image -> "Image"
        mediaKind == HistoryMediaKind.Audio -> "Audio"
        mediaKind == HistoryMediaKind.Gallery -> "Gallery"
        else -> "Media"
    }
    return durationSeconds?.let { "$media · ${it.formattedDuration()}" } ?: media
}

private fun Long.formattedDuration(): String {
    val hours = this / 3_600
    val minutes = (this % 3_600) / 60
    val seconds = this % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun HistoryEntry.sourceHost(): String? =
    runCatching { URI(sourceUrl).host }.getOrNull()
