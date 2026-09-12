package io.github.originalrecipe1.unfurlit.ui.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/** History is the page to the right; Android retains ownership of screen-edge gestures. */
@Composable
fun HistoryPager(
    historyVisible: Boolean,
    allowOpenSwipe: Boolean,
    onHistoryVisibilityChange: (Boolean) -> Unit,
    history: @Composable () -> Unit,
    content: @Composable (visible: Boolean) -> Unit,
) {
    val pager = rememberPagerState(initialPage = if (historyVisible) 0 else 1) { 2 }
    val visibilityChanged by rememberUpdatedState(onHistoryVisibilityChange)
    var predictingBack by remember { mutableStateOf(false) }
    var backFromLeft by remember { mutableStateOf(false) }
    val rightToLeftLayout = LocalLayoutDirection.current == LayoutDirection.Rtl

    LaunchedEffect(historyVisible) {
        pager.animateScrollToPage(
            if (historyVisible) 0 else 1,
            animationSpec = tween(280, easing = FastOutSlowInEasing),
        )
    }
    LaunchedEffect(pager) {
        snapshotFlow {
            if (pager.isScrollInProgress || predictingBack) null else pager.settledPage
        }.filterNotNull().distinctUntilChanged().drop(1).collect { page ->
            visibilityChanged(page == 0)
        }
    }

    HorizontalPager(
        state = pager,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer),
        // Keep Home's typed link and scroll position while looking at History.
        beyondViewportPageCount = 1,
        // In-app paging puts History to the right, matching its toolbar button. System Back follows its chosen edge.
        reverseLayout = rightToLeftLayout xor (!predictingBack || backFromLeft),
        userScrollEnabled = !predictingBack && (allowOpenSwipe || historyVisible),
        flingBehavior = PagerDefaults.flingBehavior(pager, snapPositionalThreshold = 0.12f),
        pageSpacing = 12.dp,
    ) { page ->
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction)
                    .absoluteValue.coerceIn(0f, 1f)
                scaleX = 1f - 0.04f * offset
                scaleY = 1f - 0.04f * offset
                shape = RoundedCornerShape(28.dp * (offset * 2f).coerceAtMost(1f))
                clip = true
                shadowElevation = 8.dp.toPx() * offset
            },
        ) {
            if (page == 0) {
                history()
            } else {
                // Offscreen media must release its player, even though Home stays composed.
                content(pager.currentPage + pager.currentPageOffsetFraction > 0f)
            }
        }
    }

    // Registered after page content so this takes precedence over the viewer's BackHandler.
    PredictiveBackHandler(enabled = historyVisible) { events ->
        predictingBack = true
        try {
            events.collect { event ->
                backFromLeft = event.swipeEdge == BackEventCompat.EDGE_LEFT
                val progress = event.progress.coerceIn(0f, 1f)
                val nearestPage = progress.roundToInt()
                pager.scrollToPage(nearestPage, progress - nearestPage)
            }
            pager.animateScrollToPage(1, animationSpec = tween(220))
            visibilityChanged(false)
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                pager.animateScrollToPage(0, animationSpec = tween(180))
            }
            throw cancelled
        } finally {
            predictingBack = false
        }
    }
}
