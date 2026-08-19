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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.model.Booth
import com.su.clubfair.ui.model.accent
import com.su.clubfair.ui.model.blurb
import com.su.clubfair.ui.model.categoryName
import com.su.clubfair.ui.model.displayName
import com.su.clubfair.ui.theme.AppSans
import com.su.clubfair.ui.theme.AppTextWeight
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
            // A gradient, not a flat 45% black.
            //
            // The flat version dimmed the top of the screen as hard as the
            // bottom, which turned the whole window into a dark slab with a
            // darker rectangle sliding up it — the wall behind was too dim to
            // read as the page you were still on, and not dim enough to read as
            // deliberate. Weighted to the bottom instead: the zone you tapped
            // from stays legible at the top, the shade deepens towards the
            // panel, and the panel arrives out of it rather than on top of it.
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.06f),
                    0.4f to Color.Black.copy(alpha = 0.34f),
                    1f to Color.Black.copy(alpha = 0.78f),
                ),
            )
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
                // Opaque still — see the note above on why glass fails here —
                // but lit from the top with the zone's own colour instead of
                // being one flat near-black. Ten percent is enough to tie the
                // fill to the accent border around it and to the tile the panel
                // came from; more and the text on it starts losing contrast.
                .background(
                    Brush.verticalGradient(
                        listOf(lerp(Palette.Panel, accent, 0.10f), Palette.Panel),
                    ),
                )
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
                        text = booth.displayName(),
                        fontFamily = AppSans,
                        fontWeight = AppTextWeight,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        color = Color.White,
                    )
                    // The code printed on the stall, then what kind of club it
                    // is. This briefly carried the club's name in the other
                    // language, which put a line of Thai under the heading of a
                    // panel someone had asked to be in English. A member count
                    // was here before that and was invented per club — the
                    // Student Union has never supplied any, so it is gone
                    // rather than guessed.
                    Text(
                        text = listOfNotNull(booth.displayCode, booth.categoryName())
                            .joinToString("  ·  "),
                        fontFamily = AppSans,
                        fontWeight = AppTextWeight,
                        fontSize = 12.sp,
                        color = accent,
                    )
                }
            }

            Spacer(Modifier.height(Dimens.Space))
            StatusBand(scanned = booth.scanned, accent = accent)

            Spacer(Modifier.height(Dimens.Space))
            Text(
                text = booth.blurb(),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Ink.Label,
            )
        }
    }
}

/**
 * Scanned or not, said in words — as a chip, not a banner.
 *
 * The reason it exists at all still holds: the state used to be a 20dp tick
 * beside the club's name that simply wasn't there when a booth was undone, and
 * blank space is indistinguishable from a mark you missed. Both states have to
 * draw something, in the same place, and say which one they are.
 *
 * What was wrong was the weight. It was a full-width bordered box with a 24dp
 * ring and two lines of type — about seventy dp spent on one bit of state, in a
 * panel whose whole job is three short things. It read as the most important
 * element on the sheet, above the club's own name.
 *
 * So: one line, sized to its text, sitting under the header like the label it
 * is. Done is the area's colour under dark ink, the app's loudest positive
 * pairing, and undone is an empty ring on faint glass — the shape of the thing
 * that is missing is what makes it read as "not yet" rather than as nothing.
 *
 * The second line went with the box. "Scan the QR at this booth" was an
 * instruction repeated on all 28 panels, next to a nav bar with a scan button
 * in it, under a heading that already says the booth has not been scanned.
 */
@Composable
private fun StatusBand(scanned: Boolean, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusPill))
            .background(if (scanned) accent else Color.White.copy(alpha = 0.06f))
            .then(
                if (scanned) Modifier
                else Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(Dimens.RadiusPill),
                ),
            )
            .padding(horizontal = Dimens.SpaceSm + 2.dp, vertical = Dimens.SpaceXs + 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (scanned) Palette.Ink.copy(alpha = 0.14f) else Color.Transparent)
                .then(
                    if (scanned) Modifier
                    else Modifier.border(width = 1.5.dp, color = Ink.Faint, shape = CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (scanned) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = Palette.Ink,
                    modifier = Modifier.size(11.dp),
                )
            }
        }

        Spacer(Modifier.width(Dimens.SpaceXs + 2.dp))
        Text(
            text = stringResource(
                if (scanned) R.string.booth_status_scanned
                else R.string.booth_status_unscanned
            ),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 13.sp,
            color = if (scanned) Palette.Ink else Color.White,
        )
    }
}

