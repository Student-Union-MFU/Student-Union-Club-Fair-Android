package com.su.clubfair.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.booths.BoothIcon
import com.su.clubfair.ui.components.GlassIconButton
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.model.Booth
import com.su.clubfair.ui.model.displayName
import com.su.clubfair.ui.model.Zone
import com.su.clubfair.ui.model.displayTitle
import com.su.clubfair.ui.qr.StyledQr
import com.su.clubfair.ui.theme.AppSans
import com.su.clubfair.ui.theme.AppTextWeight
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.Palette
import kotlinx.coroutines.delay

/**
 * Every booth's live check-in code, for an admin.
 *
 * The booth display on Home is one booth's own phone; this is the desk's copy of
 * all of them. It exists for the evening a booth's phone is flat, or was never
 * signed in, and a student is standing there wanting a stamp: an admin opens the
 * booth here and holds up their own screen.
 *
 * **One booth polls at a time.** The obvious build — a grid of twenty-eight live
 * QRs — is twenty-eight requests every ten seconds against a route that exists
 * to be polled by one display, and twenty-eight codes on one screen are too
 * small to scan anyway. So the list is a list, and opening a booth is what starts
 * a poll.
 */
@Composable
fun AdminCodesScreen(
    booths: List<Booth>,
    zones: List<Zone>,
    onCode: suspend (Int) -> String?,
    modifier: Modifier = Modifier,
) {
    var openBoothId by rememberSaveable { mutableStateOf<Int?>(null) }
    val open = remember(openBoothId, booths) { booths.firstOrNull { it.id == openBoothId } }

    // An open booth is a place the system's back must return from.
    //
    // Without this the gesture went straight past the app: the panel is state
    // inside a tab rather than a navigation destination, so Android had nothing
    // to pop and closed the activity instead. An admin who opened a code and
    // swiped back was dropped onto the home screen — and on the way back in, the
    // shell reopens on Home, so the way to the wall is three taps from a gesture
    // that should have cost one.
    //
    // Guarded on `open` and not on `openBoothId`: an id whose booth is not in
    // the list draws the list anyway, and a handler enabled over that would eat
    // the gesture and leave the admin unable to leave the tab at all.
    BackHandler(enabled = open != null) { openBoothId = null }

    if (open != null) {
        BoothCodePanel(booth = open, onCode = onCode, onBack = { openBoothId = null }, modifier = modifier)
        return
    }

    // Under the status bar, not behind it.
    //
    // This screen was drawing from the top of the *window* — every other full
    // screen in the app takes `safeDrawingPadding`, and without it the title sat
    // against the clock and the panel's back button was half under the notch.
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Dimens.ScreenPadding)
                .padding(top = Dimens.Space, bottom = Dimens.Space),
        ) {
            Text(
                text = stringResource(R.string.codes_title),
                modifier = Modifier.semantics { heading() },
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 26.sp,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.codes_subtitle),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 14.sp,
                color = Ink.Muted,
            )
        }

        // Grouped by area, in the zones' own order.
        //
        // An admin is sent to a *place* — "the code at B15 is dead" — and the
        // booth number carries the area in its first letter, so the list that
        // matches how the job arrives is one section per zone. Booths whose zone
        // the server has not set fall into a last group rather than vanishing;
        // an unassigned booth is exactly the one somebody will be asking about.
        val ordered = remember(zones) { zones.sortedBy { it.sortOrder } }
        val grouped = remember(booths, ordered) {
            val byZone = booths.groupBy { it.zoneCode }
            ordered.mapNotNull { zone ->
                byZone[zone.code]?.let { zone to it.sortedBy { booth -> booth.code ?: "" } }
            } + listOfNotNull(byZone[null]?.let { null to it })
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimens.ScreenPadding,
                end = Dimens.ScreenPadding,
                bottom = Dimens.NavBarClearance,
            ),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            grouped.forEach { (zone, inZone) ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "zone-${zone?.code ?: "none"}") {
                    ZoneHeading(zone = zone)
                }
                items(inZone, key = { it.id }) { booth ->
                    BoothTile(booth = booth, accent = zone?.accent, onClick = { openBoothId = booth.id })
                }
            }
        }
    }
}

/** The area's letter and name, above its booths. */
@Composable
private fun ZoneHeading(zone: Zone?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Dimens.Space, bottom = Dimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = zone?.letter ?: "—",
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 15.sp,
            // The area's own colour, which is the same one its zone card and its
            // booth wall use. An admin who has been sent to "the blue area" is
            // being given the same cue here as on the floor.
            color = zone?.accent ?: Ink.Muted,
        )
        Spacer(Modifier.size(Dimens.SpaceSm))
        Text(
            text = zone?.displayTitle() ?: stringResource(R.string.codes_no_zone),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 15.sp,
            color = Ink.Label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * One booth, as a tile.
 *
 * Three passes to get here, and the two dead ends are worth recording. A square
 * with the number at the top and the name pushed to the bottom left a hole
 * through the middle of every tile, sized by whatever the club's name did not
 * use. Stacking the pair at the top moved the hole to the foot without closing
 * it: the square was demanding height that two short lines of text will never
 * fill.
 *
 * So the square goes. The tile is a fixed height that fits its content — one
 * uniform row height keeps the grid tidy without anyone having to guess what a
 * name will do — and the club's own glyph takes the space the type does not,
 * which is the same move the booth wall makes with the same asset.
 *
 * Still only the number and the name. The glyph is not a description; it is the
 * mark already standing on that stall, and an admin looking for the drum club
 * finds a drum faster than they read "Drum Major Club".
 */
@Composable
private fun BoothTile(
    booth: Booth,
    accent: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = accent ?: Palette.Accent
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(BoothTileHeight)
            .glassSurface(cornerRadius = Dimens.RadiusLg)
            .clip(RoundedCornerShape(Dimens.RadiusLg))
            .clickable(onClick = onClick)
            .padding(Dimens.CardPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The number is the tile's subject, so it gets the type — but on a
            // plate, so it reads as the label printed on a stall rather than as
            // a heading someone set in green.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                    .background(ink.copy(alpha = 0.16f))
                    .padding(horizontal = Dimens.SpaceSm, vertical = 3.dp),
            ) {
                Text(
                    text = booth.code ?: "—",
                    fontFamily = AppSans,
                    fontWeight = AppTextWeight,
                    fontSize = 18.sp,
                    lineHeight = 1.em,
                    color = ink,
                )
            }
            Spacer(Modifier.weight(1f))
            BoothIcon(
                booth = booth,
                size = 26.dp,
                tint = Color.White.copy(alpha = 0.55f),
            )
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = booth.displayName(),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 14.sp,
            lineHeight = 1.3.em,
            color = Color.White,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * One height for every tile.
 *
 * Enough for a three-line club name at 14sp under the number row, which is the
 * longest any of the twenty-eight runs to. Fixed rather than intrinsic because a
 * grid whose rows change height with the length of a club's name reads as
 * broken, and the alternative — every tile as tall as the worst case in its own
 * row — is the same problem measured differently.
 */
private val BoothTileHeight = 132.dp

/**
 * One booth's code, large.
 *
 * Same ten-second poll and same failure behaviour as the booth owner's own card:
 * a code that cannot be refreshed stays up while the server will still take it,
 * and says out loud that it is not being refreshed. An admin holding a stale QR
 * at a booth queue is the exact situation this screen exists to rescue.
 */
@Composable
private fun BoothCodePanel(
    booth: Booth,
    onCode: suspend (Int) -> String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val payload by produceState<String?>(initialValue = null, booth.id) {
        while (true) {
            value = onCode(booth.id) ?: value
            delay(10_000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        // The back button keeps the corner; everything else moves to the middle.
        //
        // A booth's code is held *up* — an admin turns the phone to face a
        // student across a table — and on a screen held that way a name pinned to
        // the top-left corner is the one thing nobody can read. The name, the
        // number and the code all sit on the phone's centre line now, and only
        // the control the admin uses stays where a thumb expects it.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding)
                .padding(top = Dimens.Space),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(
                icon = R.drawable.ic_arrow_back,
                contentDescription = stringResource(R.string.codes_back),
                onClick = onBack,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = booth.displayName(),
                modifier = Modifier.semantics { heading() },
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 22.sp,
                lineHeight = 1.2.em,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            booth.code?.let { code ->
                Spacer(Modifier.height(Dimens.SpaceXs))
                Text(
                    text = code,
                    fontFamily = AppSans,
                    fontWeight = AppTextWeight,
                    fontSize = 15.sp,
                    color = Palette.Accent,
                )
            }

            Spacer(Modifier.height(Dimens.SpaceLg))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(Dimens.RadiusLg))
                    .background(Palette.Paper)
                    .padding(Dimens.Space),
                contentAlignment = Alignment.Center,
            ) {
                val code = payload
                if (code != null) {
                    StyledQr(content = code, modifier = Modifier.fillMaxSize())
                } else {
                    Text(
                        text = stringResource(R.string.codes_waiting),
                        fontFamily = AppSans,
                        fontWeight = AppTextWeight,
                        fontSize = 14.sp,
                        lineHeight = 1.4.em,
                        color = Color(0xFF5A6470),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(Dimens.Space))
            Text(
                text = stringResource(R.string.codes_hint),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 13.sp,
                lineHeight = 1.4.em,
                color = Ink.Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Dimens.NavBarClearance))
        }
    }
}
