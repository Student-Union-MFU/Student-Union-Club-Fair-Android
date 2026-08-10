package com.su.clubfair.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.su.clubfair.ui.booths.BoothsScreen
import com.su.clubfair.ui.components.GlassNavBar
import com.su.clubfair.ui.components.ScanTab
import com.su.clubfair.ui.events.EventsScreen
import com.su.clubfair.ui.home.HomeScreen
import com.su.clubfair.ui.model.PlaceholderStudent
import com.su.clubfair.ui.profile.ProfileScreen
import com.su.clubfair.ui.qr.QrTicketScreen
import com.su.clubfair.ui.scan.ScanScreen
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.Dimens
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/** Tab swipe. Long enough to read as a move between places, not a cut. */
private const val TabTransitionMillis = 380

/** How far the pass slides up from the bottom. */
private const val PassTransitionMillis = 320

/**
 * The profile sheet's rise, and the crossfade that carries it.
 *
 * The fade is the shorter of the two so the sheet is solid before it finishes
 * arriving — a fade that runs the whole distance leaves the cards translucent
 * over their own backdrop for most of the move, which looks like a rendering
 * fault rather than a transition. The rise is a *twelfth* of the screen, not a
 * full sheet height: this is a page appearing, and it only needs enough travel
 * to have a direction.
 */
private const val ProfileTransitionMillis = 300
private const val ProfileFadeMillis = 180
private const val ProfileRiseFraction = 12

/**
 * Material 3's emphasized curve: eases out of the gate, then arrives decisively.
 * The old FastOutSlowIn is symmetric, which reads as mechanical over a long move.
 */
private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * The signed-in shell: the tab content, the backdrop it moves over, and the
 * floating nav bar.
 *
 * The backdrop lives **here**, not in the tabs. Each tab used to paint its own
 * copy, which meant four full-screen gradients composited for one visible
 * screen; one behind the whole [AnimatedContent] is both cheaper and the reason
 * a swipe reads as content sliding *over* something rather than two slabs
 * cutting.
 *
 * The tabs no longer change it. They used to each stand in a different
 * ecosystem, and the shell panned a continuous world between them as you swiped
 * — pretty, but it meant the app's colour depended on which tab you were on. See
 * [MeshBackground].
 */
@Composable
fun AppShell(
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit = {},
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var passOpen by rememberSaveable { mutableStateOf(false) }
    var profileOpen by rememberSaveable { mutableStateOf(false) }

    // What the nav bar's liquid glass refracts. The backdrop is captured into a
    // graphics layer by `layerBackdrop` below and sampled by `drawBackdrop` in
    // the bar — so the layer has to wrap everything the bar should see through
    // itself, and nothing it shouldn't. The bar is deliberately outside it.
    val backdrop = rememberLayerBackdrop()

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            MeshBackground()

            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    val slide = tween<IntOffset>(TabTransitionMillis, easing = Emphasized)
                    // Exactly one screen width each way, so the outgoing and
                    // incoming tabs tile against each other. The backdrop is
                    // underneath and doesn't move, so nothing shows through the
                    // half-second where both are on screen.
                    val enter = slideInHorizontally(slide) { direction * it } +
                        fadeIn(tween(TabTransitionMillis / 2))
                    val exit = slideOutHorizontally(slide) { -direction * it } +
                        fadeOut(tween(TabTransitionMillis / 2))
                    (enter togetherWith exit).using(SizeTransform(clip = false))
                },
                label = "tab",
            ) { target ->
                when (target) {
                    0 -> HomeScreen(
                        onOpenClubs = { tab = 1 },
                        onOpenProfile = { profileOpen = true },
                    )

                    1 -> BoothsScreen()
                    2 -> EventsScreen(isAdmin = PlaceholderStudent.isAdmin)
                    else -> ScanScreen()
                }
            }
        }

        GlassNavBar(
            selected = tab,
            onSelect = { tab = it },
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space),
        )

        // Profile is a sheet for the same reason the pass is, and it stopped
        // being a tab when Events took the slot: it's where you go to check a
        // detail or sign out, not somewhere you move between while walking the
        // fair. Home's top-right button opens it; back closes it.
        // Fades up rather than sliding in from the side. The sheet carries its
        // own copy of the backdrop, so a horizontal slide dragged the mesh across
        // the screen with it — and the backdrop is the one thing in this app that
        // never moves. Two gradients travelling past each other read as the
        // wallpaper coming loose. A short rise under a crossfade keeps them
        // aligned: the mesh sits still and only the cards arrive.
        BackHandler(enabled = profileOpen) { profileOpen = false }
        AnimatedVisibility(
            visible = profileOpen,
            enter = fadeIn(tween(ProfileFadeMillis, easing = Emphasized)) +
                slideInVertically(
                    animationSpec = tween(ProfileTransitionMillis, easing = Emphasized),
                    initialOffsetY = { it / ProfileRiseFraction },
                ),
            exit = fadeOut(tween(ProfileFadeMillis, easing = Emphasized)) +
                slideOutVertically(
                    animationSpec = tween(ProfileTransitionMillis, easing = Emphasized),
                    targetOffsetY = { it / ProfileRiseFraction },
                ),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // The sheet covers the shell's backdrop as well as the nav bar,
                // so it has to bring its own or it would open onto bare black.
                MeshBackground()
                ProfileScreen(
                    onBack = { profileOpen = false },
                    onOpenPass = { passOpen = true },
                    onSignOut = onSignOut,
                )
            }
        }

        // The pass is a sheet over the shell rather than a destination: it's
        // something you hold up for ten seconds and dismiss, and giving it a tab
        // would put a dead end in the nav bar.
        BackHandler(enabled = passOpen) { passOpen = false }
        AnimatedVisibility(
            visible = passOpen,
            enter = slideInVertically(
                animationSpec = tween(PassTransitionMillis, easing = Emphasized),
                initialOffsetY = { it },
            ),
            exit = slideOutVertically(
                animationSpec = tween(PassTransitionMillis, easing = Emphasized),
                targetOffsetY = { it },
            ),
        ) {
            QrTicketScreen(onClose = { passOpen = false })
        }
    }
}
