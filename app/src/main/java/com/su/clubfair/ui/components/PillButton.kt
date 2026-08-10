package com.su.clubfair.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Palette

/**
 * The full-width pill CTA used across the pre-login screens.
 *
 * It is the only solid, opaque thing on a screen of frosted panes, which is what
 * makes it read as the one button that finishes the step. It used to be a pale
 * near-white for exactly that reason — but with a single scheme there is now one
 * accent instead of four, and spending it on the primary action is what an
 * accent is for.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.Accent,
            contentColor = Palette.Ink,
        ),
    ) {
        Text(
            text = text,
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
        )
    }
}
