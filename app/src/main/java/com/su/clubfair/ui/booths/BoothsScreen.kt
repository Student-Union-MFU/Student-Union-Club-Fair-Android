package com.su.clubfair.ui.booths

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.components.GlassIconButton
import com.su.clubfair.ui.components.ProgressTrack
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.model.Booth
import com.su.clubfair.ui.model.PreviewZones
import com.su.clubfair.ui.model.Zone
import com.su.clubfair.ui.model.previewRoster
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.SUClubFairTheme

private val CardRadius = Dimens.RadiusLg

/**
 * Club icons sampled onto a zone card.
 *
 * Six, because that is what fits on the narrowest phone we target without the
 * row needing to wrap or scroll — and because a sample stops reading as a
 * sample once it is the whole set.
 */
private const val ZonePreviewIcons = 6

/** Material 3's emphasized curve — decisive arrival, matching the tab swipe. */
private val EnterEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * The Booths tab: three areas, then the wall of whichever one you pick.
 *
 * This was a 27-card grid over a scanned / remaining filter. Two things were
 * wrong with it. The filter answered a question the number at the top of the
 * page already answered, and made two thirds of the fair vanish to do it. And a
 * grid of 27 identical squares is a database listing — nine at a time, in an
 * area a student is actually standing in, is a different thing entirely.
 *
 * So: pick an area, get its booths. The areas are habitats, each with its own
 * colour, and each holds a wall of nine.
 */
@Composable
fun BoothsScreen(
    modifier: Modifier = Modifier,
    booths: List<Booth> = previewRoster(7),
    /**
     * The areas, from the server, in signage order.
     *
     * Passed in rather than derived from the booths: the zone carries its Thai and
     * English names and its running order, none of which a booth knows. An empty
     * list is the state before the first fetch, and the picker shows nothing rather
     * than inventing three headings.
     */
    zones: List<Zone> = PreviewZones,
) {
    val byZone = remember(booths) { booths.groupBy { it.zoneCode } }
    val visited = remember(booths) { booths.count { it.scanned } }

    // Saved, so a trip to another tab and back returns you to the map you were
    // reading rather than to the top of the list.
    var openZone by rememberSaveable { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Booth?>(null) }
    var query by rememberSaveable { mutableStateOf<String?>(null) }

    // Back closes the sheet first, then search, then the map. Separate handlers
    // rather than one with a branch: Compose enables the innermost enabled
    // handler, so the order falls out of which of them is on screen.
    BackHandler(enabled = selected != null) { selected = null }
    BackHandler(enabled = selected == null && query != null) { query = null }
    BackHandler(enabled = selected == null && query == null && openZone != null) {
        openZone = null
    }

    // The zone the student has opened, resolved from its code. Held as a code in
    // `rememberSaveable` rather than as a Zone: the object comes from the server
    // and a Bundle cannot carry it, while a one-letter code survives anything.
    val currentZone = remember(openZone, zones) { zones.firstOrNull { it.code == openZone } }

    Box(modifier = modifier.fillMaxSize()) {
        // The insets go on the content, not on this box. The dimming scrim
        // behind the booth panel is a sibling of this, and it has to reach the
        // status bar and the gesture bar — a scrim that stops at the safe area
        // leaves two lit strips across the top and bottom of a dimmed screen,
        // which reads as the overlay failing to load rather than as a panel.
        // Entering an area is a move *into* it: the card grows past the frame
        // and the map arrives already slightly too large, settling back. Going
        // out reverses it. This used to be a hard cut, which made the map read
        // as a different screen rather than as the inside of the card.
        // Search is its own layer over both the picker and a zone's wall: a club
        // you are looking for is somewhere in the fair, and having to remember
        // which of three areas holds it before you can search for it would be the
        // question search exists to avoid.
        val currentQuery = query
        if (currentQuery != null) {
            BoothSearch(
                query = currentQuery,
                booths = booths,
                onQueryChange = { query = it },
                onSelectBooth = { selected = it },
                onClose = { query = null },
            )
        } else {
            AnimatedContent(
                targetState = currentZone,
                transitionSpec = {
                    val entering = targetState != null
                    val zoomIn = if (entering) 0.88f else 1.10f
                    val zoomOut = if (entering) 1.12f else 0.92f
                    (fadeIn(tween(220)) + scaleIn(tween(340, easing = EnterEasing), zoomIn))
                        .togetherWith(
                            fadeOut(tween(180)) +
                                scaleOut(tween(340, easing = EnterEasing), zoomOut)
                        )
                },
                label = "zoneEnter",
            ) { zone ->
                if (zone == null) {
                    ZonePicker(
                        zones = zones,
                        byZone = byZone,
                        visited = visited,
                        total = booths.size,
                        onOpenZone = { openZone = it.code },
                        onOpenSearch = { query = "" },
                    )
                } else {
                    ZoneBoothWall(
                        zone = zone,
                        booths = byZone[zone.code].orEmpty(),
                        selected = selected,
                        onSelectBooth = { selected = it },
                        onBack = { openZone = null },
                    )
                }
            }
        }

        // Lags `selected` by one assignment: it only ever takes a non-null
        // value, so the sheet keeps rendering the booth it was opened with
        // while it animates out. Reading `selected` directly would blank the
        // panel's contents the instant it was dismissed and animate an empty
        // card off the screen.
        var lastShown by remember { mutableStateOf<Booth?>(null) }
        LaunchedEffect(selected) { selected?.let { lastShown = it } }

        AnimatedVisibility(
            visible = selected != null,
            enter = fadeIn(tween(160)) + slideInVertically(tween(240)) { it / 3 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(240)) { it / 3 },
        ) {
            lastShown?.let { booth ->
                BoothSheet(
                    booth = booth,
                    onDismiss = { selected = null },
                    modifier = Modifier.padding(bottom = Dimens.NavBarClearance),
                )
            }
        }
    }
}

/**
 * The three areas, as three cards dividing the page between them.
 *
 * Weighted rather than a fixed height each: the areas take whatever the screen
 * has left between the heading and the nav bar, so a tall phone gets three tall
 * cards instead of three short ones over a pool of dead space. The clearance
 * below is what keeps the last one from sliding under the floating bar.
 *
 * What the cards no longer have is a hole in the middle. The stretch used to
 * open up between the title and the progress bar with nothing in it; the row of
 * club icons now sits in that gap, so the extra height a tall phone gives the
 * card is spent on something to look at.
 */
@Composable
private fun ZonePicker(
    zones: List<Zone>,
    byZone: Map<String?, List<Booth>>,
    visited: Int,
    total: Int,
    onOpenZone: (Zone) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = Dimens.ScreenPadding),
    ) {
        Spacer(Modifier.height(Dimens.Space))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.booths_title),
                    modifier = Modifier.semantics { heading() },
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.booths_progress,
                        total,
                        visited,
                        total,
                    ),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Ink.Muted,
                )
            }
            // 27 clubs across three areas is past the point where browsing is
            // the only way in — a student who has been told to find the Coding
            // Club should not have to guess which habitat it was filed under.
            GlassIconButton(
                icon = R.drawable.ic_search,
                contentDescription = stringResource(R.string.booths_search),
                onClick = onOpenSearch,
            )
        }

        Spacer(Modifier.height(Dimens.SpaceLg))
        zones.forEachIndexed { index, zone ->
            ZoneCard(
                zone = zone,
                booths = byZone[zone.code].orEmpty(),
                onClick = { onOpenZone(zone) },
                modifier = Modifier.weight(1f),
            )
            if (index != zones.lastIndex) Spacer(Modifier.height(Dimens.Space))
        }

        // Clearance so the last card stops above the floating nav bar rather
        // than running under it.
        Spacer(Modifier.height(Dimens.NavBarClearance))
    }
}

/**
 * One area, as the same card the rest of the app uses.
 *
 * The colour of the place is carried by the letter tile and the progress bar —
 * the two things a student is actually reading — rather than by a wash across
 * the whole card. It used to be a 34%-to-10% gradient of the zone's accent over
 * the full face, which made this the only screen in the app with large fields of
 * colour on it, and is most of why the tab looked like it came from somewhere
 * else. `Palette.kt` sets the rule the wash was overshooting: one accent, spent
 * on ticks, progress and CTAs.
 *
 * Nothing tints the face at all: a 5% wash was tried in place of the 34% one and
 * still read as a haze over the glass rather than as a colour, which is a worse
 * result than either extreme. The tile and the bar are the colour.
 *
 * The row of club icons is what the old card's row of animals was for — a
 * sample of what is inside, so the choice is between three *places* rather than
 * three names. It is also what gives the card its height: three content-sized
 * cards on a title alone left the bottom half of the screen empty, and the
 * honest way to fill a card is to put something in it.
 */
@Composable
private fun ZoneCard(
    zone: Zone,
    booths: List<Booth>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scanned = booths.count { it.scanned }
    val total = booths.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = CardRadius)
            .clip(RoundedCornerShape(CardRadius))
            .clickable(onClick = onClick)
            .padding(Dimens.CardPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The same 40dp accent tile the profile's pass row uses, holding the
            // letter that is on the signage the student is standing under.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                    .background(zone.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = zone.letter,
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = zone.accent,
                )
            }

            Spacer(Modifier.size(Dimens.CardPadding))
            Column(Modifier.weight(1f)) {
                Text(
                    text = zone.title,
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Bold,
                    // 20sp, like every other titled card. It was 24sp, a size
                    // that exists nowhere else in the app.
                    fontSize = 20.sp,
                    color = Color.White,
                )
                Text(
                    // The area's own English name where there is one, and the
                    // Thai title is already the headline above.
                    text = listOfNotNull(zone.titleEn, zone.subtitle).joinToString("  ·  "),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Ink.Muted,
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = Ink.Faint,
                modifier = Modifier.size(18.dp),
            )
        }

        // The two weights are what absorb the card's stretch, one either side of
        // the icons, so a tall phone spreads the slack around the row instead of
        // opening a single gap above the progress bar.
        Spacer(Modifier.height(Dimens.SpaceLg))
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space)) {
            // Only booths that have a glyph. Six of the 28 have none, and a zone
            // card sampling those would show its own booth codes back at itself
            // where a picture belongs.
            booths.filter { it.icon != null }.take(ZonePreviewIcons).forEach { booth ->
                Icon(
                    painter = painterResource(booth.icon!!),
                    contentDescription = null,
                    tint = Ink.Faint,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))

        Spacer(Modifier.height(Dimens.SpaceLg))
        Text(
            text = stringResource(R.string.booths_zone_progress, scanned, total),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Color.White,
        )
        Spacer(Modifier.height(Dimens.SpaceSm))
        ProgressTrack(fraction = if (total == 0) 0f else scanned.toFloat() / total, accent = zone.accent)
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun BoothsPreview() {
    SUClubFairTheme {
        // The shell supplies the backdrop in the app; stand one in for the preview.
        Box {
            MeshBackground()
            BoothsScreen()
        }
    }
}
