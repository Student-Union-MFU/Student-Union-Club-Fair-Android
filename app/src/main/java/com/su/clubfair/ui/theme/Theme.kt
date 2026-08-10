package com.su.clubfair.ui.theme

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * The Material scheme, derived from [Palette].
 *
 * There is no light variant and no `isSystemInDarkTheme` check. The app is a
 * translucent UI floating over a lit backdrop — the glass in `Glass.kt` is white
 * at 10% alpha, which is frost over the mesh and a smear over a white page. A
 * light mode isn't a colour swap here, it's a different app.
 */
private val Scheme = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Palette.Ink,
    secondary = Palette.GlowEmerald,
    onSecondary = Color.White,
    tertiary = Palette.GlowDeep,
    onTertiary = Color.White,
    background = Palette.Floor,
    onBackground = Color.White,
    // Surfaces are the glass, and the glass paints its own fill over the
    // backdrop. Anything Material draws a surface for underneath needs to be the
    // backdrop's own darkest tone or it shows as a rectangle.
    surface = Palette.Floor,
    onSurface = Color.White,
    surfaceVariant = Palette.Panel,
    onSurfaceVariant = Ink.Label,
    outline = Ink.Faint,
    outlineVariant = Ink.Faint,
)

/**
 * App theme.
 *
 * It takes no palette any more. It used to: each tab and each step of the
 * sign-in flow passed the "ecosystem" it stood in, and the whole scheme changed
 * underneath the content as you moved. One scheme means a screen can't be
 * rendered in the wrong world, and there is nothing to thread through.
 *
 * Material You dynamic colour used to be on here. It had to go: it hands the
 * entire scheme to whatever the user's wallpaper happens to be, which for an
 * event with a fixed identity means the branding is decided by someone's lock
 * screen. Worth knowing if it ever looks tempting again.
 */
@Composable
fun SUClubFairTheme(content: @Composable () -> Unit) {
    // No overscroll stretch, app-wide. The stretch grabs the whole scrolling
    // container and skews it — including the glass cards sitting on the mesh,
    // which is exactly the sort of surface that shows a distortion.
    // Provided here rather than per-scroller so a new screen can't miss it.
    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        MaterialTheme(
            colorScheme = Scheme,
            typography = Typography,
            content = content,
        )
    }
}
