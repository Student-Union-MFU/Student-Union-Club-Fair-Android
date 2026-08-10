package com.su.clubfair.ui.onboarding

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.components.PillButton
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.SUClubFairTheme

/** Half the hero's vertical travel — it drifts between -[HeroDrift] and +[HeroDrift]. */
private val HeroDrift = 10.dp

/** One leg of the drift; a full down-and-back cycle is twice this. */
private const val HeroDriftMillis = 2600

/** Keeps the hero clear of the screen edges without boxing it in like the text does. */
private val HeroPadding = 8.dp
private val TextPadding = 24.dp

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onContinue: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        MeshBackground()

        val drift = rememberHeroDrift()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(vertical = 20.dp),
        ) {
            // The hero takes every pixel the text and CTA don't need, so it scales
            // with the screen instead of being pinned to a fixed box.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = HeroPadding),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.onboard_hero),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        // Cap it so tablets don't get a wall-sized illustration.
                        .sizeIn(maxWidth = 460.dp, maxHeight = 460.dp)
                        .fillMaxSize()
                        .graphicsLayer { translationY = HeroDrift.toPx() * drift },
                )
            }

            // Shrinks a step at a time so the four-line headline never clips.
            BasicText(
                text = stringResource(R.string.onboarding_headline),
                style = TextStyle(
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 1.15.em,
                ),
                maxLines = 4,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 24.sp,
                    maxFontSize = 36.sp,
                    stepSize = 0.5.sp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TextPadding),
            )

            Spacer(Modifier.height(24.dp))
            PillButton(
                text = stringResource(R.string.onboarding_cta),
                onClick = onContinue,
                modifier = Modifier.padding(horizontal = TextPadding),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * A -1..1 ease-in-out ramp that reverses forever, for the hero's idle float.
 *
 * Returns a flat 0 when the system has animations switched off — that respects
 * the accessibility setting, and it keeps the Compose test clock reaching idle
 * (an endless animation never lets `waitForIdle` settle).
 */
@Composable
private fun rememberHeroDrift(): Float {
    val animate = remember {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
    }
    if (!animate) return 0f

    val transition = rememberInfiniteTransition(label = "hero")
    val drift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(HeroDriftMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroDrift",
    )
    return drift
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun OnboardingScreenPreview() {
    SUClubFairTheme {
        OnboardingScreen()
    }
}
