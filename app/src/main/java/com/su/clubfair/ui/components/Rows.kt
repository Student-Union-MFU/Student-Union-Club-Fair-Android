package com.su.clubfair.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.LocalAccent
import com.su.clubfair.ui.theme.Palette

/**
 * The back button and title that every full-screen sheet opens with.
 *
 * Profile and Settings were spelling out the same row; the title is marked as a
 * `heading` so TalkBack's heading navigation can jump between sheets' titles
 * rather than reading from the top every time.
 */
@Composable
fun SheetHeader(
    title: String,
    onBack: () -> Unit,
    backDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(
            icon = R.drawable.ic_arrow_back,
            contentDescription = backDescription,
            onClick = onBack,
        )
        Spacer(Modifier.size(Dimens.Space))
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            fontFamily = AlanSans,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White,
        )
    }
}

/** A group label above a card of rows. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        fontFamily = AlanSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        color = Ink.Muted,
    )
}

/**
 * The icon plate that fronts every row in the app.
 *
 * 40dp, the zone card's and the pass row's size, so a settings row and a profile
 * row read as the same furniture.
 */
@Composable
private fun RowIcon(
    @DrawableRes icon: Int,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(Dimens.RadiusSm))
            .background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * A tappable row: icon, title, supporting line, chevron.
 *
 * `heightIn(min = 56.dp)` rather than padding alone, so a row whose text happens
 * to be one short line is still a 56dp target — comfortably over the 48dp
 * minimum, and the same height whatever it contains.
 *
 * The `external` flag this used to carry is gone with the two rows that set it:
 * the policies are pages of this app now rather than addresses out of it, so
 * every row here goes somewhere in the app and the chevron can say so without a
 * qualifier. See `LegalScreen`.
 *
 * [enabled] dims the row and stops it answering, rather than the row being
 * removed while it cannot be used. Sync is the case it exists for — a control
 * that vanishes for the two seconds it is working reads as a mis-tap.
 */
@Composable
fun ActionRow(
    @DrawableRes icon: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tint: Color = LocalAccent.current,
    enabled: Boolean = true,
) {
    // One value, applied to the row as a whole rather than to each colour, so an
    // unavailable row dims evenly instead of by however many shades it contains.
    val alpha = if (enabled) 1f else 0.45f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick, role = Role.Button, enabled = enabled)
            .alpha(alpha)
            .padding(vertical = Dimens.Space),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon = icon, tint = tint)
        Spacer(Modifier.size(Dimens.Space))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = if (tint == Palette.Alert) Palette.Alert else Color.White,
            )
            if (subtitle != null) {
                Spacer(Modifier.size(1.dp))
                Text(
                    text = subtitle,
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Ink.Muted,
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = Ink.Faint,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * A row with a switch on the end.
 *
 * The whole row toggles, not just the switch — a 40dp control on the far edge of
 * a phone is the hardest thing on the page to hit one-handed, and the row is
 * already a 56dp target that says exactly what the switch does.
 */
@Composable
fun ToggleRow(
    @DrawableRes icon: Int,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            // `toggleable` rather than `clickable` so TalkBack announces this as
            // a switch with a state, and the row and the control are one node
            // instead of two that both claim the same tap.
            .clickable(
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(vertical = Dimens.Space),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon = icon, tint = LocalAccent.current)
        Spacer(Modifier.size(Dimens.Space))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = Color.White,
            )
            if (subtitle != null) {
                Spacer(Modifier.size(1.dp))
                Text(
                    text = subtitle,
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Ink.Muted,
                )
            }
        }
        Switch(
            checked = checked,
            // Null: the row above owns the gesture, and a switch with its own
            // handler would be a second focusable node saying the same thing.
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Palette.Ink,
                checkedTrackColor = LocalAccent.current,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Ink.Label,
                uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                uncheckedBorderColor = Ink.Faint,
            ),
        )
    }
}

/** A read-only row: a label and its value, no affordance. */
@Composable
fun InfoRow(
    @DrawableRes icon: Int,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = Dimens.Space),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon = icon, tint = LocalAccent.current)
        Spacer(Modifier.size(Dimens.Space))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = Color.White,
        )
        Text(
            text = value,
            fontFamily = AlanSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = Ink.Muted,
        )
    }
}
