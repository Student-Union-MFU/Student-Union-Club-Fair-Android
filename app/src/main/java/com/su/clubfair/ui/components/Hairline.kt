package com.su.clubfair.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The one divider weight in the app: 1dp of white at a tenth.
 *
 * Spelled out by hand under the announcements header and between the profile's
 * detail rows, which is how a second alpha eventually gets picked. [StatPane]
 * keeps its own vertical rule at 0.14 — a short 26dp stroke needs the extra to
 * read at all, where a full-width line at that alpha would be a stripe.
 */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.10f)),
    )
}
