package com.su.clubfair.ui.loading

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AppSans
import com.su.clubfair.ui.theme.AppTextWeight
import com.su.clubfair.ui.theme.Bitcount
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.Palette
import com.su.clubfair.ui.theme.SUClubFairTheme

/**
 * The wait between signing in and the fair being on screen.
 *
 * Shown for one window only — `AppPhase.Preparing`, which lasts exactly as long
 * as the first fetch made with a new token. There is no minimum display time and
 * no artificial delay: if the fetch is quick this screen is brief, and the
 * crossfade either side of it is what keeps that from reading as a flicker. A
 * loading screen that outlives its loading is a worse lie than a plain one.
 *
 * The wordmark rather than a bare spinner, because this is the first thing a
 * student sees as *their* account rather than as the sign-in flow, and the
 * backdrop is the same mesh every other screen stands on — so the arrival reads
 * as one surface being furnished rather than as three separate screens.
 */
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.loading_fair)

    Box(modifier = modifier.fillMaxSize()) {
        MeshBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Same mark and the same font as Welcome, a third of the size: this
            // is the app introducing itself again, not announcing itself.
            BasicText(
                text = stringResource(R.string.welcome_title),
                style = TextStyle(
                    fontFamily = Bitcount,
                    fontWeight = AppTextWeight,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 0.9.em,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.None,
                    ),
                ),
                maxLines = 2,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 28.sp,
                    maxFontSize = 46.sp,
                    stepSize = 1.sp,
                ),
            )

            SweepingTrack(
                modifier = Modifier.padding(top = Dimens.SpaceXl),
            )

            Text(
                text = label,
                modifier = Modifier.padding(top = Dimens.Space),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 14.sp,
                color = Ink.Muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * An indeterminate bar, built to match [com.su.clubfair.ui.components.ProgressTrack].
 *
 * Hand-drawn rather than a `LinearProgressIndicator` for one reason: the app
 * already has an answer to what progress looks like — a flat accent fill in a
 * 14%-white rounded track — and a Material bar three dp thinner with its own
 * easing sitting next to it would be a second answer to a settled question.
 *
 * The segment travels its own length past each end before wrapping, so it leaves
 * and enters cleanly instead of appearing to bounce off the edges.
 */
@Composable
private fun SweepingTrack(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loading")
    val head by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // Eased rather than linear: a constant-speed segment reads as a
            // machine ticking, and this one is telling a student to wait a
            // moment rather than reporting a rate.
            animation = tween(durationMillis = 1150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "head",
    )

    Canvas(
        modifier = modifier
            .width(TrackWidth)
            .height(TrackHeight)
            // One announcement for the whole screen, from the label below. A bar
            // that also spoke would say "loading" twice, and it has no reading
            // to give — it is indeterminate.
            .clearAndSetSemantics { },
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = Color.White.copy(alpha = 0.14f), cornerRadius = radius)

        val segment = size.width * SegmentFraction
        val travel = size.width + segment
        clipRect {
            drawRoundRect(
                color = Palette.Accent,
                topLeft = Offset(head * travel - segment, 0f),
                size = Size(segment, size.height),
                cornerRadius = radius,
            )
        }
    }
}

private val TrackWidth = 148.dp
private val TrackHeight = 6.dp
private const val SegmentFraction = 0.4f

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun LoadingScreenPreview() {
    SUClubFairTheme {
        LoadingScreen()
    }
}
