package com.su.clubfair.ui.booths

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.model.Booth
import com.su.clubfair.ui.model.accent
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.Palette

/**
 * One booth, as a panel over the zone it stands in.
 *
 * Opaque, and this is the second surface in the app that has to be — the same
 * reason as the register form's dropdown. It was tried as glass, on the
 * principle that every other surface here is frosted: the booth cards on the
 * track behind it stayed perfectly legible *through* the sheet, so two clubs'
 * names and blurbs overlapped the one you had actually tapped. A scrim does not
 * fix that; a scrim dims the backdrop evenly and the text underneath dims with
 * it. Glass works when what is behind it is a gradient, and fails when what is
 * behind it is more text.
 *
 * The top half of it used to be a spinning 3D market stall — the same model for
 * all 27 clubs, a live Filament scene, 150dp of panel spent on a picture that
 * said nothing about which booth you had tapped. It is gone, and with it the
 * mascot emoji and the renderer.
 *
 * Three things, in the order a student needs them: which booth this is, whether
 * they have already scanned it, and what the club does. That is the whole
 * panel.
 *
 * It briefly had more — an "at the stall" section, a row of tag chips, the
 * meeting time and the campus venue. Every one of them was defensible on its
 * own and together they turned a panel you glance at into a page you read. The
 * meeting time and venue are facts about a club you have already joined; the
 * rest was detail competing with the one line that actually decides whether a
 * student walks over.
 */
@Composable
fun BoothSheet(
    booth: Booth,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = booth.accent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(Dimens.Space)
                .clip(RoundedCornerShape(Dimens.RadiusLg))
                .background(Palette.Panel)
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(Dimens.RadiusLg),
                )
                // Taps inside the panel are the panel's own; without this they
                // fall through to the scrim behind it and shut the thing you
                // are reading.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(Dimens.CardPadding),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The club's own icon at panel size — the same tile the track's
                // card carries, so the thing you tapped is the thing that opens.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(Dimens.RadiusMd))
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    BoothIcon(booth = booth, size = 24.dp)
                }

                Spacer(Modifier.width(Dimens.Space))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = booth.name,
                        fontFamily = AlanSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        color = Color.White,
                    )
                    // The code printed on the stall, then the English name where
                    // there is one. A member count used to share this line and was
                    // invented per club — the Student Union has never supplied any,
                    // so it is gone rather than guessed.
                    Text(
                        text = listOfNotNull(booth.displayCode, booth.nameEn)
                            .joinToString("  ·  "),
                        fontFamily = AlanSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = accent,
                    )
                }
            }

            Spacer(Modifier.height(Dimens.Space))
            StatusBand(scanned = booth.scanned, accent = accent)

            Spacer(Modifier.height(Dimens.Space))
            Text(
                text = booth.about.orEmpty(),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Ink.Label,
            )
        }
    }
}

/**
 * Scanned or not, said in words.
 *
 * This is the whole reason the band exists. The state used to be a 20dp tick
 * that appeared beside the club's name when a booth was done and left nothing
 * at all when it wasn't — so "not scanned" was rendered as blank space, which
 * is indistinguishable from "this panel has no tick in it". A student cannot
 * tell an absent mark from a mark they missed.
 *
 * Both states now draw the same full-width band in the same place, and both say
 * which one they are. Done is the area's colour under dark ink, the app's
 * loudest positive pairing; not done is an empty ring on faint glass with the
 * instruction for fixing that on the second line.
 */
@Composable
private fun StatusBand(scanned: Boolean, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(if (scanned) accent else Color.White.copy(alpha = 0.06f))
            .then(
                if (scanned) Modifier
                else Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(Dimens.RadiusMd),
                ),
            )
            .padding(horizontal = Dimens.Space, vertical = Dimens.SpaceSm + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (scanned) Palette.Ink.copy(alpha = 0.12f) else Color.Transparent)
                .then(
                    if (scanned) Modifier
                    // An empty ring, the same size as the tick's disc. The
                    // shape of the thing that is missing is what makes it read
                    // as "not yet" rather than as nothing.
                    else Modifier.border(width = 2.dp, color = Ink.Faint, shape = CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (scanned) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = Palette.Ink,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.width(Dimens.SpaceSm))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    if (scanned) R.string.booth_status_scanned
                    else R.string.booth_status_unscanned
                ),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (scanned) Palette.Ink else Color.White,
            )
            if (!scanned) {
                Text(
                    text = stringResource(R.string.booth_status_unscanned_hint),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Ink.Muted,
                )
            }
        }
    }
}

