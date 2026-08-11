package com.su.clubfair.ui.prizes

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.components.Hairline
import com.su.clubfair.ui.components.ProgressTrack
import com.su.clubfair.ui.components.SectionLabel
import com.su.clubfair.ui.components.SheetHeader
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.model.FairProgress
import com.su.clubfair.ui.model.PreviewProgress
import com.su.clubfair.ui.model.PrizeTier
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.Palette
import com.su.clubfair.ui.theme.SUClubFairTheme

private val CardRadius = Dimens.RadiusLg

/**
 * What a student gets for walking the fair, and how close they are to it.
 *
 * **The prize list is not final.** `clubfair_prize_tier` holds three seeded rows
 * — Halfway, Prize draw, Full sweep — written to get the thresholds working, not
 * by whoever is actually buying the prizes. The banner at the top says so in as
 * many words, because a student who reads "Prize draw" as a promise and turns up
 * to claim one has been misled by this screen rather than by the Student Union.
 *
 * Everything except that banner is real: the thresholds, the descriptions and
 * the reached/claimed flags all come from `GET /clubfair/progress`, so the day
 * the Union rewrites those three rows this screen is correct with no release.
 * That is the whole reason it renders the server's tiers rather than a hardcoded
 * "coming soon" — a placeholder that shows nothing teaches a student nothing,
 * and this one already answers "how many booths until something happens".
 *
 * The Prizes tile on Home used to open nothing at all: `onOpenPrizes` had no
 * caller in `AppShell`, so the button was decoration.
 */
@Composable
fun PrizesScreen(
    progress: FairProgress,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPadding),
    ) {
        Spacer(Modifier.height(Dimens.Space))
        SheetHeader(
            title = stringResource(R.string.prizes_title),
            onBack = onBack,
            backDescription = stringResource(R.string.profile_back),
        )

        Spacer(Modifier.height(Dimens.SpaceLg))
        EarnedSummary(progress = progress)

        Spacer(Modifier.height(Dimens.Space))
        PlaceholderNotice()

        if (progress.prizes.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.SpaceLg))
            SectionLabel(stringResource(R.string.prizes_tiers_section))
            Spacer(Modifier.height(Dimens.SpaceSm))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassSurface(cornerRadius = CardRadius)
                    .padding(horizontal = Dimens.CardPadding),
            ) {
                progress.prizes.forEachIndexed { index, tier ->
                    TierRow(tier = tier, visited = progress.visited)
                    if (index != progress.prizes.lastIndex) Hairline()
                }
            }
        }

        Spacer(Modifier.height(Dimens.SpaceXl))
        Spacer(Modifier.height(Dimens.NavBarClearance))
    }
}

/** How many tiers are already earned, at the size Home shows the same number. */
@Composable
private fun EarnedSummary(progress: FairProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = CardRadius)
            .padding(Dimens.CardPadding),
    ) {
        Text(
            text = "${progress.prizesEarned}",
            fontFamily = AlanSans,
            fontWeight = FontWeight.Bold,
            fontSize = 44.sp,
            lineHeight = 1.1.em,
            color = Palette.Accent,
        )
        Text(
            text = stringResource(R.string.prizes_earned_of, progress.prizes.size),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color.White,
        )

        Spacer(Modifier.height(Dimens.Space))
        ProgressTrack(fraction = progress.progress)
        Spacer(Modifier.height(Dimens.SpaceXs))
        Text(
            text = stringResource(
                R.string.prizes_booths_progress,
                progress.visited,
                progress.total,
            ),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = Ink.Muted,
        )
    }
}

/**
 * The one thing on this screen that is not the server's.
 *
 * Amber rather than accent, and above the list rather than under it: it
 * qualifies everything below, and a caveat a student reaches after reading the
 * prizes has arrived too late to do its job.
 */
@Composable
private fun PlaceholderNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(Palette.Alert.copy(alpha = 0.12f))
            .padding(Dimens.Space),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_alert_circle),
            contentDescription = null,
            tint = Palette.Alert,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(Dimens.SpaceSm))
        Text(
            text = stringResource(R.string.prizes_placeholder_notice),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 1.45.em,
            color = Ink.Label,
        )
    }
}

/**
 * One threshold, and how far off it is.
 *
 * The number of booths still to go is the useful part and is computed here
 * rather than sent: the server knows both figures and so does the client, and a
 * "3 more booths" field on the wire would be a third copy of a subtraction.
 */
@Composable
private fun TierRow(tier: PrizeTier, visited: Int) {
    val remaining = (tier.threshold - visited).coerceAtLeast(0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.Space),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The threshold in its own disc — reached tiers fill it, the rest are
        // outlined. The state is in the shape as well as the colour, so it does
        // not depend on a student telling lime from grey-green.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (tier.reached) Palette.Accent else Color.White.copy(alpha = 0.10f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (tier.reached) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = Palette.Ink,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    text = "${tier.threshold}",
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.size(Dimens.Space))
        Column(Modifier.weight(1f)) {
            Text(
                text = tier.name,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = Color.White,
            )
            Text(
                text = when {
                    tier.claimed -> stringResource(R.string.prizes_claimed)
                    tier.reached -> stringResource(R.string.prizes_ready)
                    else -> stringResource(R.string.prizes_remaining, remaining)
                },
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 1.4.em,
                color = if (tier.reached) Palette.Accent else Ink.Muted,
            )
            tier.description?.let { description ->
                Text(
                    text = description,
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 1.4.em,
                    color = Ink.Muted,
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun PrizesScreenPreview() {
    SUClubFairTheme {
        Box(Modifier.fillMaxSize()) {
            MeshBackground()
            PrizesScreen(progress = PreviewProgress)
        }
    }
}
