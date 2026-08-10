package com.su.clubfair.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.su.clubfair.ui.theme.LocalAccent

/**
 * How far along something is, as a bar.
 *
 * Home and the Booths tab each had their own — 8dp with a gradient on one, 5dp
 * and flat on the other — which is two answers to "what does progress look like
 * in this app" sitting one swipe apart.
 *
 * The fill is flat. Home's version ramped from near-white at the start to full
 * accent at the head, on the theory that it gave the bar a direction; across the
 * width of a zone card it just read as a glowing streak, and a bar that draws
 * more attention than the number above it is the wrong way round.
 *
 * [accent] defaults to whatever the subtree provides, so Home gets the app's lime
 * and a zone card gets its own colour without either having to say so.
 */
@Composable
fun ProgressTrack(
    fraction: Float,
    modifier: Modifier = Modifier,
    accent: Color = LocalAccent.current,
    height: Dp = 8.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(accent),
        )
    }
}
