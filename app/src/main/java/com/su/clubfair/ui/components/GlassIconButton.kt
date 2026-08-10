package com.su.clubfair.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.su.clubfair.ui.theme.Dimens

/** One step down from the nav bar's round scan button. */
private val ButtonSize = 40.dp

/**
 * A round glass button with one glyph in it — Home's profile button, the
 * profile's back button, the back button on a zone.
 *
 * Three screens had their own copy of this same 40dp circle. Naming it is what
 * stops the fourth one being 44dp with an 18dp glyph, and it is the control that
 * tells a student "this is the way in and out" wherever it turns up.
 *
 * [contentDescription] is not optional: every caller so far is the only way out
 * of the screen it sits on.
 */
@Composable
fun GlassIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(ButtonSize)
            .glassSurface(cornerRadius = Dimens.RadiusPill)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}
