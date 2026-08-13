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
import com.su.clubfair.ui.components.ProgressTrack
import com.su.clubfair.ui.components.SectionLabel
import com.su.clubfair.ui.components.SheetHeader
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.model.FairProgress
import com.su.clubfair.ui.model.PreviewProgress
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
 * **The prize list is not final.** `clubfair_prize_tier` holds two rows — Prize 1
 * at fifteen booths, Prize 2 at twenty-eight — and those are targets, not a
 * description of what is in the bag. The banner at the top says so in as many
 * words, because a student who reads a name on this screen as a promise and
 * turns up to claim it has been misled by this screen rather than by the Student
 * Union. It is also why the tiers are numbered rather than named: "Prize 1"
 * cannot be mistaken for a description of something nobody has bought yet.
 *
 * Everything except that banner is real: the thresholds, the descriptions and
 * the reached/claimed flags all come from `GET /clubfair/progress`, so the day
 * the Union rewrites those rows this screen is correct with no release.
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
            Spacer(Modifier.height(Dimens.SpaceXl))
            SectionLabel(stringResource(R.string.prizes_route_section))
            Spacer(Modifier.height(Dimens.SpaceXs))
            Text(
                text = stringResource(R.string.prizes_route_hint),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 1.45.em,
                color = Ink.Muted,
            )
            Spacer(Modifier.height(Dimens.SpaceLg))
            PrizeRoadmap(progress = progress)
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

/** Past the first prize and claimed it, with the full sweep still to go. */
@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun PrizesRoutePreview() {
    SUClubFairTheme {
        Box(Modifier.fillMaxSize()) {
            MeshBackground()
            PrizesScreen(
                progress = PreviewProgress.copy(
                    visited = 16,
                    prizes = PreviewProgress.prizes.mapIndexed { index, tier ->
                        tier.copy(reached = index == 0, claimed = index == 0)
                    },
                ),
            )
        }
    }
}
