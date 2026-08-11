package com.su.clubfair.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink

data class StatEntry(val value: String, val label: String)

/**
 * A row of figures in one glass pane, split by hairlines.
 *
 * This started as separate chips per stat. At a third of the screen width each
 * one was cramped, and their icons were noise at that size — a row of small glass
 * boxes read as clutter beside full-width cards. One surface with dividers is
 * calmer and lets the numbers carry it. Home and the profile share it so the two
 * can't drift apart.
 */
@Composable
fun StatPane(
    entries: List<StatEntry>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = Dimens.RadiusLg)
            .height(66.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        entries.forEachIndexed { index, entry ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = entry.value,
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    lineHeight = 1.1.em,
                    color = Color.White,
                    maxLines = 1,
                )
                Spacer(Modifier.height(Dimens.SpaceXs / 2))
                Text(
                    text = entry.label,
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    lineHeight = 1.1.em,
                    color = Ink.Muted,
                )
            }
            if (index != entries.lastIndex) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(26.dp)
                        .background(Color.White.copy(alpha = 0.14f)),
                )
            }
        }
    }
}
