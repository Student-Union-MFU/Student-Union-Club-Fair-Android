package com.su.clubfair.ui.prizes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.components.Hairline
import com.su.clubfair.ui.components.SheetHeader
import com.su.clubfair.ui.components.glassSurface
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.su.clubfair.ui.model.FairProgress
import com.su.clubfair.ui.model.PreviewProgress
import com.su.clubfair.ui.model.PreviewStudent
import com.su.clubfair.ui.model.Student
import com.su.clubfair.ui.qr.StyledQr
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AppSans
import com.su.clubfair.ui.theme.AppTextWeight
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.Palette
import com.su.clubfair.ui.theme.SUClubFairTheme

private val CardRadius = Dimens.RadiusLg

/**
 * How much of the locked code is left on screen.
 *
 * Faint enough to read as a ghost of the thing rather than the thing, solid
 * enough that the square is recognisably a QR code and not an empty panel. The
 * value applies to the whole paper panel, not just the ink — dimming the ink
 * alone leaves a bright white card announcing itself as the loudest element on a
 * screen whose subject is how far the student still has to walk.
 */
private const val LockedQrAlpha = 0.10f

/**
 * What the locked square actually encodes.
 *
 * A placeholder rather than the student id at low opacity. Alpha is a drawing
 * instruction, not a redaction: a photograph of a 10%-opacity code with the
 * contrast pushed is still a readable code, and the whole claim of the locked
 * state is that there is nothing here to collect yet. Encoding the reward's own
 * name means the worst case for a student who defeats the dimming is that they
 * scanned the word MFU333.
 *
 * It doubles as the fallback for an account with no student id — [StyledQr]
 * draws nothing at all for content it cannot encode, and a lock floating over
 * blank paper reads as a rendering fault rather than as a locked reward.
 */
private const val PlaceholderQrContent = "MFU333"

/**
 * What a student gets for walking the fair, and how close they are to it.
 *
 * Titled **MFU333** — the reward itself, not the mechanism. The name is an app
 * string because it is the section's identity; the tiers inside it are still the
 * server's, from `clubfair_prize_tier` via `GET /clubfair/progress`, so the
 * thresholds, the descriptions and the reached/claimed flags all move without a
 * release. That split is the point: the Student Union can retarget a tier
 * mid-fair, and only a rename of the whole section needs a new build.
 *
 * The "prize list isn't final" banner that used to sit above the route is gone.
 * It was unconditional, so it would have gone on calling the reward provisional
 * long after MFU333 was decided — and a caveat that contradicts the heading
 * above it teaches a student less than no caveat at all.
 *
 * The MFU333 tile on Home used to open nothing at all: `onOpenPrizes` had no
 * caller in `AppShell`, so the button was decoration.
 */
@Composable
fun PrizesScreen(
    progress: FairProgress,
    // No default. This ends up encoded in a QR the Student Union desk scans, so
    // a caller that forgets to pass a student should not compile — the previous
    // default was `PreviewStudent`, which would have handed the desk a made-up
    // id from a @Preview fixture.
    student: Student,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    // The code takes the page.
    //
    // This screen is one card and it was sitting in the top two thirds with a
    // third of a phone empty under it — a page that looks like it failed to
    // finish loading. There is nothing else to put there, and the thing that *is*
    // there is a QR someone holds up at a desk, so it gets the room: centred in
    // what is left under the header, and as wide as the screen allows.
    //
    // `heightIn(min = maxHeight)` inside the scroll is what makes the weights
    // below work. A scrolling column is handed infinite height, so a `weight`
    // has nothing to divide; forcing a minimum of one viewport gives it real
    // spare room to share, while still letting the content grow and scroll on a
    // short screen. Same trick Home uses to sit its stack on the bottom edge.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .heightIn(min = maxHeight)
            .padding(horizontal = Dimens.ScreenPadding),
    ) {
        Spacer(Modifier.height(Dimens.Space))
        SheetHeader(
            title = stringResource(R.string.prizes_title),
            onBack = onBack,
            backDescription = stringResource(R.string.profile_back),
        )

        // Slightly less air above than below, so the card sits a touch high of
        // true centre — optically centred rather than measured, which is where
        // the eye expects a single object under a header.
        Spacer(Modifier.height(Dimens.SpaceLg))
        Mfu333Card(progress = progress, student = student)

        Spacer(Modifier.height(Dimens.Space))
        Mfu333Explainer(progress = progress)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(Dimens.NavBarClearance))
    }
    }
}

/**
 * The MFU333 code, and how far off it is.
 *
 * One card in two states rather than a code that appears out of nowhere at
 * fifteen. A square is drawn either way — a student who cannot see the reward
 * cannot aim at it — and while it is locked it sits under a padlock at a tenth
 * of its opacity: present enough to be recognised as the thing being worked
 * toward, and plainly not yet a code anyone can hand over.
 *
 * The two states differ in what they encode, not only in how brightly it is
 * drawn. Locked, the square carries [PlaceholderQrContent]; only once the server
 * says the tier is reached does the student's own id go into it. That is the
 * difference between a code that is hidden and a code that is not there — see
 * the constant for why the opacity alone was not enough to claim the second.
 *
 * The count below it is the whole answer to "how much further", and it is
 * deliberately checkpoints-over-threshold rather than a percentage or a bar:
 * a student standing in the hall is counting booths, so the screen counts the
 * same thing they are.
 *
 * The threshold is the server's, from the tier itself, so the Student Union can
 * move fifteen to any number and this follows without a release.
 *
 * The unlocked payload is the student id — the same value the pass carries,
 * because the desk needs to know who is standing there and nothing else. It is
 * not a secret and is not treated as one: whether this student has earned MFU333
 * is a question for the server at the moment of collection, and `reached` here
 * only decides what the app draws.
 */
/**
 * What MFU333 is, how it is earned, and where it is collected.
 *
 * The page was a code and a fraction. That is enough for a student who already
 * knows the game and nothing at all for one who tapped a tile called MFU333 on
 * their first evening — the screen named the prize, showed a padlock, and never
 * said what was behind it or what to do about it.
 *
 * Three steps, because the thing being explained genuinely has three: scan at
 * booths, reach the threshold, show the code at the desk. Numbered rather than
 * bulleted, since the order is the whole point and step two is the only one that
 * takes any time.
 *
 * The tier's own name and description come from the server when it has them —
 * `clubfair_prize_tier` is a table the Student Union edits — so what this prize
 * actually *is* stays theirs to write, and the app supplies only the mechanics
 * around it. When they have written nothing, the mechanics stand on their own
 * rather than leaving a labelled gap.
 */
@Composable
private fun Mfu333Explainer(progress: FairProgress, modifier: Modifier = Modifier) {
    val threshold = progress.mfu333?.threshold ?: 0

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.prizes_about_title),
            modifier = Modifier.semantics { heading() },
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 16.sp,
            color = Color.White,
        )
        Spacer(Modifier.height(Dimens.SpaceXs))
        Text(
            // The app's copy, always — not the tier's own `description`.
            //
            // Preferring the server's words here was the obvious thing and the
            // wrong one: `clubfair_prize_tier.description` is a roadmap caption
            // ("15 booths visited"), written to sit under a node on the route
            // where the surrounding page supplies the context. Dropped into a
            // card headed "What is MFU333?" it answers a different question than
            // the one asked, and reads as a bug. The tier's words still lead the
            // roadmap; the explanation is this screen's own.
            text = stringResource(R.string.prizes_about_body, threshold),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 13.sp,
            lineHeight = 1.5.em,
            color = Ink.Muted,
        )

        Spacer(Modifier.height(Dimens.Space))
        Step(1, stringResource(R.string.prizes_step_scan))
        Step(2, stringResource(R.string.prizes_step_collect, threshold))
        Step(3, stringResource(R.string.prizes_step_claim))
    }
}

/** One numbered line of the explainer. */
@Composable
private fun Step(number: Int, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpaceXs),
        verticalAlignment = Alignment.Top,
    ) {
        // A disc rather than a bare numeral: at 13sp a lone "1" beside a
        // paragraph reads as a footnote marker, which is the opposite of a step
        // somebody is meant to follow.
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Palette.Accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 12.sp,
                color = Palette.Accent,
            )
        }
        Spacer(Modifier.size(Dimens.Space))
        Text(
            text = text,
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 13.sp,
            lineHeight = 1.5.em,
            color = Ink.Label,
        )
    }
}

@Composable
private fun Mfu333Card(progress: FairProgress, student: Student) {
    val unlocked = progress.mfu333Unlocked
    val threshold = progress.mfu333?.threshold ?: 0

    // No card around this any more.
    //
    // The glass panel was wrapping the code *and* the figure *and* the hint, so
    // the lock read as a lock on the whole block — a page that looked switched
    // off rather than a code that is not ready yet. The paper square is the only
    // object here that needs a surface of its own (dark ink has to sit on light
    // ground to be scannable), and everything under it is just type on the page.
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // No title. The sheet header above already reads "MFU333", and a
        // card inside it captioned "MFU333 is locked" is the same word three
        // times on one screen. The seal on the code says locked, and the line
        // under the figure says what to do about it.
        Box(contentAlignment = Alignment.Center) {
            // The paper panel is not decoration and it is not optional. The code
            // is drawn in dark ink — see StyledQr for why it has to be that way
            // round rather than light-on-dark — so on this card's glass it would
            // be dark ink on a near-black ground, which is a code no scanner at
            // the desk can read. The pass solves it the same way.
            //
            // The square comes from `aspectRatio`, and that is what makes the
            // code exist at all: StyledQr fills the space it is given, and the
            // width-only modifier it used to carry left it inside a
            // wrap-height Column with no height to fill.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .alpha(if (unlocked) 1f else LockedQrAlpha)
                    .clip(RoundedCornerShape(Dimens.RadiusMd))
                    .background(Palette.Paper)
                    .padding(Dimens.Space),
            ) {
                StyledQr(
                    content = if (unlocked) {
                        student.studentId?.takeIf { it.isNotBlank() } ?: PlaceholderQrContent
                    } else {
                        PlaceholderQrContent
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Outside the alpha above, so the dimming does not take the padlock
            // down with the thing it is locking.
            //
            // A paper disc with dark ink in it, rather than the bare white glyph
            // this used to be. Over a square faded to a tenth there is nothing
            // for a white line drawing to sit against, and it read as an icon
            // that had come loose; the disc is the same two colours the code
            // itself uses, so it looks like a seal placed on the panel rather
            // than an overlay floating above it.
            if (!unlocked) LockSeal()
        }

        Spacer(Modifier.height(Dimens.SpaceLg))
        // The figure and its unit as one block, not two lines with air between:
        // "7/15" and "checkpoints" are one sentence and the tight leading is
        // what makes them read as one.
        Text(
            text = stringResource(
                R.string.prizes_qr_checkpoints,
                progress.visited,
                threshold,
            ),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 34.sp,
            lineHeight = 1.05.em,
            color = if (unlocked) Palette.Accent else Color.White,
        )
        Spacer(Modifier.height(Dimens.SpaceXs / 2))
        Text(
            text = stringResource(R.string.prizes_qr_checkpoints_label),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 11.sp,
            lineHeight = 1.1.em,
            color = Ink.Muted,
        )

        // The rule went with the card. It was separating two halves of one
        // surface; on the open page the space does that on its own, and a
        // hairline across nothing is a seam in a sheet that is not there.
        Spacer(Modifier.height(Dimens.Space))
        Text(
            text = if (unlocked) {
                stringResource(R.string.prizes_qr_hint)
            } else {
                pluralStringResource(
                    R.plurals.prizes_qr_locked,
                    progress.boothsToMfu333,
                    progress.boothsToMfu333,
                )
            },
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 13.sp,
            lineHeight = 1.45.em,
            color = Ink.Muted,
            textAlign = TextAlign.Center,
        )
    }
}

/** Diameter of the seal, and the glyph inside it. */
private val LockSealSize = 56.dp
private val LockGlyphSize = 24.dp

/**
 * The mark that says this code is not yours yet.
 *
 * Its own composable because inline it was four nested modifiers sitting in the
 * middle of the card's layout, between the code and the figure under it.
 */
@Composable
private fun LockSeal(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(LockSealSize)
            .clip(CircleShape)
            .background(Palette.Paper),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = Palette.Ink,
            modifier = Modifier.size(LockGlyphSize),
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun PrizesScreenPreview() {
    SUClubFairTheme {
        Box(Modifier.fillMaxSize()) {
            MeshBackground()
            PrizesScreen(progress = PreviewProgress, student = PreviewStudent)
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
                student = PreviewStudent,
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
