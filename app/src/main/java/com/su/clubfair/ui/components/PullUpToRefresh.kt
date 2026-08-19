package com.su.clubfair.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.su.clubfair.ui.theme.LocalAccent
import com.su.clubfair.ui.theme.Palette
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pull **up** from the end of a list to refresh it.
 *
 * Material3 ships only the top-anchored gesture, and on a channel that is read
 * bottom-up it is anchored to the wrong end: the newest post is at the bottom,
 * which is where the reader already is, so a top pull is only in reach after
 * scrolling back through the whole history to the oldest post — the one place
 * nobody is standing when they want to know what just arrived.
 *
 * Everything else about it is the top gesture's behaviour, kept deliberately:
 * resistance on the drag so it feels rubbery rather than linear, a threshold the
 * pull has to pass before release counts, and an indicator that tracks the
 * finger on the way and then spins on its own.
 *
 * Nothing here is announcement-specific. It is in `components/` for the reason
 * [Hairline] and [ProgressTrack] are — the second list in this app that wants a
 * bottom pull should not invent a second answer to how far and how hard.
 */
@Stable
class PullUpToRefreshState internal constructor(internal val thresholdPx: Float) {

    /** How far the list has been dragged past its end, in pixels. Never negative. */
    var distance by mutableFloatStateOf(0f)
        internal set

    /** The pull as a fraction of the threshold, for the indicator's arc. */
    val progress: Float get() = (distance / thresholdPx).coerceIn(0f, 1f)
}

/**
 * [threshold] is how far the list has to be dragged past its end before letting
 * go triggers a refresh — the same 64dp Material uses at the top.
 */
@Composable
fun rememberPullUpToRefreshState(threshold: Dp = 64.dp): PullUpToRefreshState {
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    return remember(thresholdPx) { PullUpToRefreshState(thresholdPx) }
}

/**
 * Half. A drag that moved the indicator one-for-one with the finger felt loose
 * and made the threshold trivial to cross by accident at the end of an ordinary
 * scroll; resistance is what makes the gesture something you decide to do.
 */
private const val DragFactor = 0.5f

/**
 * The gesture, as a nested-scroll connection on whatever wraps the list.
 *
 * It reads the scroll the list itself could not use. A list with somewhere left
 * to go consumes the whole drag, so `available` is zero and nothing here fires —
 * which is what confines the gesture to the end of the list without this having
 * to ask the list where it is.
 */
@Composable
fun Modifier.pullUpToRefresh(
    isRefreshing: Boolean,
    state: PullUpToRefreshState,
    onRefresh: () -> Unit,
): Modifier {
    // The connection outlives a recomposition — rebuilding it on every frame
    // would drop the drag in progress — so the two things that do change are
    // read through the latest snapshot rather than captured once.
    val refreshing = rememberUpdatedState(isRefreshing)
    val refresh = rememberUpdatedState(onRefresh)
    val connection = remember(state) {
        object : NestedScrollConnection {

            /** Unwinds an existing pull before the list gets the drag back. */
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (refreshing.value || state.distance <= 0f) return Offset.Zero
                if (available.y <= 0f) return Offset.Zero
                val used = min(state.distance / DragFactor, available.y)
                state.distance -= used * DragFactor
                return Offset(0f, used)
            }

            /** Whatever the list could not use at its end is the pull. */
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (refreshing.value || available.y >= 0f) return Offset.Zero
                state.distance -= available.y * DragFactor
                return Offset(0f, available.y)
            }

            /**
             * Release. Fires before the spring rather than after it, so the
             * spinner is already turning while the indicator settles — waiting
             * for the animation would put a still, wound-up indicator on screen
             * for a quarter of a second looking like nothing had happened.
             */
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (state.distance <= 0f) return Velocity.Zero
                if (state.distance >= state.thresholdPx && !refreshing.value) refresh.value()
                animate(state.distance, 0f) { value, _ -> state.distance = value }
                return Velocity.Zero
            }
        }
    }
    return this.nestedScroll(connection)
}

/** Diameter of the disc, and the arc inside it. */
private val IndicatorSize = 36.dp
private val ArcSize = 18.dp

/**
 * The disc that rises out of the bottom edge as the list is pulled.
 *
 * Place it as the last child of the box carrying [pullUpToRefresh], aligned to
 * `BottomCenter`, and give that box `clipToBounds` — at rest this sits below the
 * edge, and the clip is what keeps it there rather than drawing over whatever
 * follows the list.
 *
 * Painted in the app's own panel and accent. Material's surface colours arrive
 * as a light grey disc on this backdrop, which is the same reason Home overrides
 * them on the indicator at its top.
 */
@Composable
fun PullUpIndicator(
    state: PullUpToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // Where it parks when nothing is happening: its own height plus a gap, below
    // the edge, so none of it is showing.
    val hidden = with(density) { (IndicatorSize + 12.dp).toPx() }

    // While refreshing it holds at the threshold regardless of the finger, which
    // is what makes it read as "working" rather than as still being dragged.
    // Capped a little past the threshold so a hard fling cannot throw it up the
    // screen.
    val travel = if (isRefreshing) {
        state.thresholdPx
    } else {
        state.distance.coerceAtMost(state.thresholdPx * 1.25f)
    }

    if (!isRefreshing && travel <= 0f) return

    Box(
        modifier = modifier
            .offset { IntOffset(0, (hidden - travel).roundToInt()) }
            .padding(bottom = 12.dp)
            .size(IndicatorSize)
            .clip(CircleShape)
            .background(Palette.Panel),
        contentAlignment = Alignment.Center,
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                color = LocalAccent.current,
                strokeWidth = 2.dp,
                modifier = Modifier.size(ArcSize),
            )
        } else {
            // Determinate on the way up: the arc closing is what tells the
            // reader how much further the pull has to go.
            CircularProgressIndicator(
                progress = { state.progress },
                color = LocalAccent.current,
                trackColor = Palette.Panel,
                strokeWidth = 2.dp,
                modifier = Modifier.size(ArcSize),
            )
        }
    }
}
