package org.peek.app.ui.history

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.peek.app.R
import org.peek.app.domain.model.HistoryEntry
import org.peek.app.domain.model.HistoryMediaKind
import java.net.URI
import java.util.Calendar

@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onOpen: (HistoryEntry) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryScreen(state, onBack, onOpen, viewModel::remove, viewModel::clear)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreen(
    state: HistoryState,
    onBack: () -> Unit,
    onOpen: (HistoryEntry) -> Unit,
    onRemove: (Long) -> Unit,
    onClear: () -> Unit,
) {
    val entries = (state as? HistoryState.Ready)?.entries.orEmpty()
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val groups = entries.groupBy { entry ->
        val day = Calendar.getInstance().apply {
            timeInMillis = entry.viewedAtEpochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        day.timeInMillis
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), "Back")
                    }
                },
                actions = {
                    if (entries.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirmation = true }) {
                            Text("Clear all")
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        Box(Modifier.padding(contentPadding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when (state) {
                HistoryState.Loading -> HistoryMessage("Loading history…", showProgress = true)
                HistoryState.Failed -> HistoryMessage(
                    "History is unavailable",
                    "Your history could not be loaded. Try opening this page again.",
                )
                is HistoryState.Ready -> if (entries.isEmpty()) {
                    HistoryMessage("A little rewind", "Media you watch will appear here.\nOpen a link to start your collection.")
                } else {
                    LazyColumn(
                        modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        groups.forEach { (day, dayEntries) ->
                            item(key = "day-$day") {
                                val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                                val label = when {
                                    DateUtils.isToday(day) -> "Today"
                                    Calendar.getInstance().apply { timeInMillis = day }.let {
                                        it.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                                            it.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
                                    } -> "Yesterday"
                                    else -> DateUtils.formatDateTime(context, day, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
                                }
                                Text(
                                    label,
                                    modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 2.dp).semantics { heading() },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            items(dayEntries, key = HistoryEntry::id) { entry ->
                                HistoryRow(
                                    entry = entry,
                                    onOpen = { onOpen(entry) },
                                    onRemove = { onRemove(entry.id) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            icon = { Icon(painterResource(R.drawable.ic_delete), null) },
            title = { Text("Clear viewing history?") },
            text = { Text("This removes all visits and their saved thumbnails from this device.") },
            confirmButton = {
                TextButton(onClick = { showClearConfirmation = false; onClear() }) { Text("Clear history") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val title = entry.title ?: entry.sourceHost() ?: "Untitled media"
    val time = DateUtils.formatDateTime(context, entry.viewedAtEpochMillis, DateUtils.FORMAT_SHOW_TIME)
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            HistoryThumbnail(entry)
            Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    entry.platform ?: entry.sourceHost() ?: "Media",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                entry.author?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    "${entry.mediaDescription()} · $time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    painterResource(R.drawable.ic_delete),
                    "Remove $title from history",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HistoryThumbnail(entry: HistoryEntry) {
    val icon = when (entry.mediaKind) {
        HistoryMediaKind.Video -> R.drawable.ic_play
        HistoryMediaKind.Audio -> R.drawable.ic_audio
        HistoryMediaKind.Image -> R.drawable.ic_image
        HistoryMediaKind.Gallery, HistoryMediaKind.Mixed -> R.drawable.ic_gallery
    }
    Box(
        Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        entry.thumbnail?.let {
            AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (entry.thumbnail != null) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ) {
                Icon(painterResource(icon), null, Modifier.padding(3.dp).size(14.dp))
            }
        }
    }
}

@Composable
private fun HistoryMessage(title: String, description: String? = null, showProgress: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        if (showProgress) {
            CircularProgressIndicator()
        } else {
            Box(
                Modifier.size(96.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_history), null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        description?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
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
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

private fun HistoryEntry.sourceHost(): String? = runCatching { URI(sourceUrl).host }.getOrNull()
