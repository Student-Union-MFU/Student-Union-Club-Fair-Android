package com.su.clubfair.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.lerp
import com.su.clubfair.R
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Palette
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class NavItem(
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
)

/** The tabs that live inside the bar itself, in display order. */
val NavItems = listOf(
    NavItem(R.drawable.ic_home, R.string.nav_home),
    // A triangle, a square and a circle — a variety of things, which is what a
    // fair of 27 clubs is. It was a group-of-people glyph, which meant the right
    // thing but sat two tabs away from the single-person Profile icon and read as
    // the same picture at 22dp.
    NavItem(R.drawable.ic_shapes, R.string.nav_booths),
    // Events took Profile's slot. Profile isn't a place you go back and forth to
    // during a fair — you open it to show your pass and close it again — so it
    // moved to the button in Home's top bar and the announcements channel, which
    // *does* get checked repeatedly, took the tab.
    NavItem(R.drawable.ic_bell, R.string.nav_events),
)

/** Scan's index. It sits past the end of [NavItems] because it isn't in the bar. */
const val ScanTab = 3

/**
 * The one surface with a real backdrop blur, and the only one that earns it.
 *
 * This bar floats over *scrolling* tabs, so cards, text and the checkpoint grid
 * pass underneath it — real structure, and softening it is the whole point of
 * the material. (The mesh behind them has structure too, but the tabs are what
 * actually move under this bar.)
 *
 * Scan is deliberately **not** in the bar. It's the one thing a student does
 * while standing at a booth, and the other three are places to look at — mixing
 * an action in with the destinations made it read as a fourth page. Splitting it
 * out as its own round button says "this does something" without a label.
 */
@Composable
fun GlassNavBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(Dimens.NavBarHeight)
                .liquidGlass(backdrop, PillShape),
        ) {
            val slotWidthPx = constraints.maxWidth.toFloat() / NavItems.size
            val slotWidth = maxWidth / NavItems.size

            // Where the indicator actually is, in pixels from the bar's left
            // edge. An Animatable rather than a plain animated value because the
            // drag needs to *snap* it to the finger while the release needs to
            // *spring* it to a slot — one value, two ways of being moved.
            val indicator = remember { Animatable(0f) }
            var dragging by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            // The gesture below is keyed on the slot width, so its coroutine is
            // *not* restarted when the selection changes — which means anything
            // it closes over is frozen at the value it had when the finger went
            // down. Read through these instead.
            //
            // Getting this wrong is what broke dragging back to the first tab:
            // a drag starting on Home captured `selected == 0`, so on the way
            // back the `index != selected` guard compared 0 against a stale 0
            // and swallowed the call. Every tab was reachable except the one you
            // started from.
            val currentSelected by rememberUpdatedState(selected)
            val currentOnSelect by rememberUpdatedState(onSelect)

            // Settles to the selected slot whenever a drag isn't driving it.
            // Keyed on `dragging` too, so letting go hands control straight back.
            LaunchedEffect(selected, slotWidthPx, dragging) {
                if (!dragging) {
                    indicator.animateTo(
                        targetValue = selected * slotWidthPx,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(indicator.value.roundToInt(), 0) }
                    .width(slotWidth)
                    .fillMaxHeight()
                    .padding(IndicatorInset)
                    .clip(PillShape)
                    .background(Color.White.copy(alpha = 0.16f)),
            )

            Row(
                // Slide across the bar to move between tabs, the way Telegram
                // lets you drag along its bar rather than only tapping.
                //
                // On the container, not the items: a drag that started on Home
                // has to keep being delivered while the finger is over Booths,
                // and a per-item detector only ever sees its own bounds. The
                // items keep their own `clickable` for taps — `clickable` gives
                // movement up once it passes touch slop, so the two coexist.
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(slotWidthPx) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragging = true },
                            onDragEnd = { dragging = false },
                            onDragCancel = { dragging = false },
                        ) { change, _ ->
                            // The indicator follows the finger continuously
                            // rather than hopping slot to slot. Without this the
                            // drag worked but showed nothing until it crossed a
                            // boundary, which reads as the gesture not being
                            // picked up at all.
                            val travel = slotWidthPx * (NavItems.size - 1)
                            val x = (change.position.x - slotWidthPx / 2f)
                                .coerceIn(0f, travel)
                            scope.launch { indicator.snapTo(x) }

                            val index = (change.position.x / slotWidthPx).toInt()
                                .coerceIn(0, NavItems.lastIndex)
                            if (index != currentSelected) currentOnSelect(index)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavItems.forEachIndexed { index, item ->
                    NavBarItem(
                        item = item,
                        selected = index == selected,
                        onClick = { onSelect(index) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }

        Spacer()
        ScanButton(
            selected = selected == ScanTab,
            onClick = { onSelect(ScanTab) },
            backdrop = backdrop,
        )
    }
}

private val PillShape = RoundedCornerShape(50)

/** How far the sliding indicator sits inside the bar, so it reads as within the glass. */
private val IndicatorInset = 6.dp

/**
 * The tap bounce, Telegram-style: a quick squash up, then a spring back that
 * overshoots slightly before settling.
 *
 * Two stages rather than one spring from rest, because a spring alone eases *out*
 * of the tap — it starts slowest exactly when the finger lands, which is when the
 * feedback has to arrive. A short tween up gets the icon moving immediately and
 * the spring underneath it does the part the eye reads as physical.
 */
private const val BouncePeak = 1.28f
private const val BounceRiseMillis = 110

/**
 * Play the bounce.
 *
 * Fires on **every** tap, including on the tab you are already on — the bounce is
 * the acknowledgement that the tap landed, and swallowing it when nothing changes
 * is what makes a bar feel unresponsive.
 *
 * `snapTo` first so a rapid second tap restarts the bounce from rest instead of
 * springing from wherever the last one had got to.
 */
private suspend fun Animatable<Float, *>.bounce() {
    snapTo(1f)
    animateTo(BouncePeak, tween(BounceRiseMillis, easing = FastOutSlowInEasing))
    animateTo(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
    )
}

/** The gap that separates the action from the destinations. */
@Composable
private fun Spacer() {
    Box(modifier = Modifier.width(Dimens.Space))
}

@Composable
private fun NavBarItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emphasis by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "navEmphasis",
    )
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val label = stringResource(item.label)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusPill))
            .clickable {
                scope.launch { scale.bounce() }
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = label,
            // Lifted from 0.55 at rest. The liquid glass refracts and blurs
            // whatever is behind it up into the pane, so an unselected icon at
            // the old alpha had bright, moving content competing with it from
            // underneath — the frosted version had a flat fill and did not.
            tint = Color.White.copy(alpha = 0.78f + 0.22f * emphasis),
            // graphicsLayer, not a size change: scaling the layer costs no
            // relayout, so the bounce can't shove the neighbouring tabs around
            // while it runs.
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
    }
}

/**
 * Scan, as a round glass button beside the bar.
 *
 * It fills with the accent as it becomes selected rather than just brightening
 * its icon — it's the primary action, so it should look pressed-in when you're
 * on it, not merely lit.
 */
@Composable
private fun ScanButton(
    selected: Boolean,
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val emphasis by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "scanEmphasis",
    )
    val accent = Palette.Accent
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .size(Dimens.NavBarHeight)
            .liquidGlass(backdrop, CircleShape)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.22f * emphasis))
            .clickable {
                scope.launch { scale.bounce() }
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_scan),
            contentDescription = stringResource(R.string.nav_scan),
            tint = lerp(Color.White.copy(alpha = 0.92f), accent, emphasis),
            // The glyph bounces, not the button. Scaling the whole pill would
            // pump the blur behind it, which reads as the glass wobbling.
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
    }
}
