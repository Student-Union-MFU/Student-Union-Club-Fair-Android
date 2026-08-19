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
import androidx.compose.foundation.border
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
import androidx.compose.runtime.derivedStateOf
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

/**
 * The tabs that live inside the bar itself, in display order.
 *
 * What every account sees. An admin gets one more — see [adminNavItems] — and
 * the bar takes its items as a parameter rather than reading this list directly,
 * so the two cannot drift into disagreeing about how many slots there are.
 */
val NavItems = listOf(
    NavItem(R.drawable.ic_home, R.string.nav_home),
    // The stage mask out of Home's Clubs tile — the one piece of that cluster
    // that is still legible at 22dp, so the tab and the tile share a hand
    // without the bar carrying a drawing it cannot render. Before it was a
    // triangle, a square and a circle, meaning "a variety of things": true, and
    // three unrelated specks. Before that a group-of-people glyph, which meant
    // the right thing but sat two tabs from the single-person Profile icon and
    // read as that same picture.
    NavItem(R.drawable.ic_booths, R.string.nav_booths),
    // Events took Profile's slot. Profile isn't a place you go back and forth to
    // during a fair — you open it to show your pass and close it again — so it
    // moved to the button in Home's top bar and the announcements channel, which
    // *does* get checked repeatedly, took the tab.
    NavItem(R.drawable.ic_bell, R.string.nav_events),
)

/**
 * The bar an admin gets: the same three, plus the wall of booth codes.
 *
 * A fourth *destination*, not a fourth action. Every booth's live QR is a place
 * an admin goes and looks — at the desk, when a booth's own phone has died — so
 * it belongs beside the other places rather than out on the round button, which
 * says "this does something".
 */
val adminNavItems = NavItems + NavItem(R.drawable.ic_layers, R.string.nav_codes)

/**
 * Scan's index: one past the last tab in the bar, because it isn't in the bar.
 *
 * A function of the list rather than a constant. It was 3, which was right while
 * there was one bar; an admin's bar has four slots and a hardcoded 3 would have
 * put the scanner and the code wall on the same index — the scanner winning,
 * silently, on the one account that needs both.
 */
fun scanTabFor(items: List<NavItem>): Int = items.size

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
    items: List<NavItem> = NavItems,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(Dimens.NavBarHeight)
                // No shader rim: `Highlight.Default` ramps in brightness around
                // the outline, and on a shape this wide the ramp reads as a
                // gradient painted onto the bar rather than as light catching an
                // edge — brighter along one flank, gone along the other. An even
                // hairline instead, which is what `Glass.kt` settled on for the
                // frosted panes for the same reason, and which is also the only
                // edge an API 24 phone can draw.
                .liquidGlass(backdrop, PillShape, highlight = null)
                .border(1.dp, EdgeColor, PillShape),
        ) {
            val slotWidthPx = constraints.maxWidth.toFloat() / items.size
            val slotWidth = maxWidth / items.size

            // Where the indicator rests, in pixels from the bar's left edge. An
            // Animatable because the release has to *spring* to a slot.
            val indicator = remember { Animatable(0f) }

            // Where the finger is, while it is down. Null the rest of the time,
            // and that null is what hands the indicator back to the spring.
            //
            // A plain state rather than driving the Animatable through the drag:
            // `snapTo` is a suspend function, so following the finger with it
            // meant `scope.launch` per pointer event — a coroutine allocated,
            // dispatched and immediately superseded a hundred-odd times a second,
            // which is most of what made the gesture feel like it was catching.
            // Nothing about following a finger needs an animation loop; the
            // finger *is* the animation.
            var dragX by remember { mutableStateOf<Float?>(null) }

            // Where it was let go, so the settle springs from under the finger
            // instead of jumping back to wherever the indicator sat when the
            // drag began. Consumed by the effect below on the frame after.
            var releasedAt by remember { mutableStateOf<Float?>(null) }

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

            // Which tab the icons light up for: the one under the finger while
            // dragging, the real selection otherwise.
            //
            // `derivedStateOf` is doing real work here — [dragX] changes every
            // frame of a drag, and reading it straight would recompose this
            // whole subtree at pointer rate. The *slot* it lands in changes twice
            // in a typical gesture, and that is all this notifies on.
            val highlighted by remember(slotWidthPx) {
                derivedStateOf {
                    dragX?.let { slotIndexAt(it, slotWidthPx, items.size) } ?: currentSelected
                }
            }

            // Whether a finger is down, as its own derived state.
            //
            // It looks like `dragX != null` written out longhand, and the
            // difference is where the read happens. Passing `dragX != null`
            // straight to the effect below evaluates it *during composition*,
            // which subscribes this whole composable to [dragX] and recomposes
            // it on every pointer event — the exact cost the drag was rewritten
            // to avoid, reintroduced by a key. Derived, it notifies twice a
            // gesture: finger down, finger up.
            val dragging by remember { derivedStateOf { dragX != null } }

            // Settles to the selected slot whenever a drag isn't driving it.
            // Keyed on `dragging` too, so letting go hands control straight back.
            LaunchedEffect(selected, slotWidthPx, dragging) {
                if (dragging) return@LaunchedEffect
                releasedAt?.let {
                    indicator.snapTo(it)
                    releasedAt = null
                }
                indicator.animateTo(
                    targetValue = selected * slotWidthPx,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }

            Box(
                // Both reads happen inside the lambda, which is layout, not
                // composition — the indicator tracks the finger for free.
                modifier = Modifier
                    .offset { IntOffset((dragX ?: indicator.value).roundToInt(), 0) }
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
                            onDragStart = { dragX = indicatorXAt(it.x, slotWidthPx, items.size) },
                            // The page changes here, on release — not slot by
                            // slot on the way across.
                            //
                            // It used to switch live, and that is what made the
                            // gesture feel like it was dragging something heavy:
                            // a drag from Home to Events fired two tab changes,
                            // each starting a 380ms slide the next one
                            // interrupted, and every frame of those slides
                            // reuploads the whole screen into the backdrop layer
                            // the glass above it samples through — so the bar was
                            // re-running its blur and lens shaders over a
                            // full-screen source that was never still, while the
                            // incoming screen was composing for the first time.
                            // Three tabs' worth of that arrives inside one
                            // gesture.
                            //
                            // Committing once means one transition, uninterrupted,
                            // and it costs nothing in feedback: the indicator is
                            // under the finger the whole way and the icons light
                            // up as it passes them. The pages you skimmed past
                            // were never places you were going anyway.
                            onDragEnd = {
                                val x = dragX ?: return@detectHorizontalDragGestures
                                releasedAt = x
                                dragX = null
                                val index = slotIndexAt(x, slotWidthPx, items.size)
                                if (index != currentSelected) currentOnSelect(index)
                            },
                            // Not a choice, so it settles back to where it was
                            // rather than committing whatever the finger last
                            // happened to be over.
                            onDragCancel = {
                                releasedAt = dragX
                                dragX = null
                            },
                        ) { change, _ ->
                            // The indicator follows the finger continuously
                            // rather than hopping slot to slot. Without this the
                            // drag worked but showed nothing until it crossed a
                            // boundary, which reads as the gesture not being
                            // picked up at all.
                            dragX = indicatorXAt(change.position.x, slotWidthPx, items.size)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    NavBarItem(
                        item = item,
                        selected = index == highlighted,
                        onClick = { onSelect(index) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }

        Spacer()
        val scanTab = scanTabFor(items)
        ScanButton(
            selected = selected == scanTab,
            onClick = { onSelect(scanTab) },
            backdrop = backdrop,
        )
    }
}

/**
 * The indicator's left edge for a finger at [touchX], clamped to the bar.
 *
 * Half a slot behind the finger, because the indicator is a slot wide and is
 * being centred under the touch, and clamped so it stops at the first and last
 * tabs instead of sliding out from under the glass.
 */
private fun indicatorXAt(touchX: Float, slotWidth: Float, slots: Int): Float =
    (touchX - slotWidth / 2f).coerceIn(0f, slotWidth * (slots - 1))

/** Which slot an indicator sitting at [x] is nearest to. */
private fun slotIndexAt(x: Float, slotWidth: Float, slots: Int): Int =
    ((x + slotWidth / 2f) / slotWidth).toInt().coerceIn(0, slots - 1)

private val PillShape = RoundedCornerShape(50)

/**
 * The hairline round the bar and the Scan button.
 *
 * Deliberately dimmer than the 0.22 the frosted cards use. Those sit still on
 * the mesh; this one floats over content that scrolls underneath it, and an edge
 * bright enough to read as a drawn outline turns the bar into a box sitting on
 * the page instead of a pane the page passes behind. This is the alpha where the
 * shape is defined and you don't notice the line doing it.
 */
private val EdgeColor = Color.White.copy(alpha = 0.12f)

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
            tint = Color.White,
            // graphicsLayer, not a size change: scaling the layer costs no
            // relayout, so the bounce can't shove the neighbouring tabs around
            // while it runs.
            //
            // The fade rides in the same layer rather than in the tint, for the
            // same reason. A tint is a parameter, so animating it recomposes
            // this icon on every frame of the 220ms ramp; alpha set here is read
            // in the draw phase and skips composition and layout entirely.
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    alpha = 0.78f + 0.22f * emphasis
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
            // Matched to the bar beside it — two pieces of the same material
            // sitting a Space apart, edged two different ways, is worse than
            // either choice made consistently.
            .liquidGlass(backdrop, CircleShape, highlight = null)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.22f * emphasis))
            // After the fill, not before: the selected state washes accent
            // across the whole face, and a hairline underneath it comes out
            // tinted on the selected tab and white everywhere else.
            .border(1.dp, EdgeColor, CircleShape)
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
