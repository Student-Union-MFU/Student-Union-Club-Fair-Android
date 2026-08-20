package com.su.clubfair.ui.scan

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.components.Hairline
import com.su.clubfair.ui.components.InitialsAvatar
import com.su.clubfair.ui.components.PillButton
import com.su.clubfair.ui.components.SheetHeader
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.model.AccountRole
import com.su.clubfair.ui.model.Participant
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AppSans
import com.su.clubfair.ui.theme.AppTextWeight
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.LocalAccent
import com.su.clubfair.ui.theme.Palette
import com.su.clubfair.ui.theme.SUClubFairTheme

private val CardRadius = Dimens.RadiusLg
private val ChipRadius = 999.dp

/**
 * Who the scanned pass belongs to, as a page.
 *
 * It used to be a card at the foot of the live scanner, and that was the wrong
 * shape for what the admin is doing. Checking a person against their pass is a
 * moment of *reading* — the name, the id, the role, whether the account is
 * flagged — and the card put four lines of that over a camera still hunting for
 * codes, with the person's face competing against a moving viewfinder for the
 * same screen. So the scanner steps aside: the camera unbinds, the page takes
 * the display, and the way back to scanning is an explicit action rather than
 * something that happens the moment another code drifts past the lens.
 *
 * Still deliberately not everything the roster holds. su-server returns a phone
 * number and an email with every participant, and neither belongs on a screen
 * held up at a booth in front of a queue — an admin checking that the person in
 * front of them is who the pass says needs a name, an id, and how far along they
 * are. Contact details are the dashboard's job, on a desk, behind a login. The
 * page has room for them now; that is not the same as a reason to show them.
 */
@Composable
fun ParticipantScreen(
    participant: Participant,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    // `heightIn(min = maxHeight)` inside the scroll is what lets the button sit
    // on the bottom edge. A scrolling column is handed infinite height, so a
    // `weight` has nothing to divide; forcing a minimum of one viewport gives it
    // real spare room to share, while still letting a long name and a flag
    // banner grow past the fold and scroll. Same arrangement the prizes page and
    // Home use.
    //
    // Bottom-anchored rather than tucked under the details, because this page is
    // two thirds empty on anyone whose school and major are short, and a button
    // floating in the middle of that reads as a page that failed to finish
    // loading. It is also the one control here, on a phone being held one-handed
    // at a desk.
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
            title = stringResource(R.string.scan_participant_title),
            onBack = onBack,
            backDescription = stringResource(R.string.scan_participant_back),
        )

        Spacer(Modifier.height(Dimens.SpaceLg))
        IdentityCard(participant = participant)

        // Directly under the name, above the details. A flag is the one thing on
        // this page that changes what the admin should *do*, so it sits where
        // the eye already is rather than at the foot of a card they may not
        // scroll to.
        if (participant.isFlagged) {
            Spacer(Modifier.height(Dimens.Space))
            FlaggedBanner()
        }

        Spacer(Modifier.height(Dimens.Space))
        DetailsCard(participant = participant)

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(Dimens.SpaceLg))
        PillButton(
            text = stringResource(R.string.scan_again),
            onClick = onBack,
        )

        // The nav bar floats over this page — it is the Scan tab, not a sheet
        // above the shell — so the button needs room under it or the bar sits on
        // top of the only way back to the camera.
        Spacer(Modifier.height(Dimens.NavBarClearance))
    }
    }
}

/**
 * The face of the page: who this is, in one block.
 *
 * The student id is in the accent because it is the thing being checked — an
 * admin holding a phone next to a printed pass is comparing that number and
 * nothing else on the card.
 */
@Composable
private fun IdentityCard(participant: Participant, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = CardRadius)
            .padding(Dimens.CardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Initials only. The roster carries no photograph — see [InitialsAvatar]
        // — so there is nothing here that could ever be a face, and a page that
        // reserved space for one would hold an empty circle at every scan.
        InitialsAvatar(initials = participant.initials, size = 64.dp)

        Spacer(Modifier.size(Dimens.CardPadding))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = participant.name,
                modifier = Modifier.semantics { heading() },
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 22.sp,
                lineHeight = 1.2.em,
                color = Color.White,
            )
            participant.studentId?.let { id ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = id,
                    fontFamily = AppSans,
                    fontWeight = AppTextWeight,
                    fontSize = 14.sp,
                    color = Palette.Accent,
                )
            }
            Spacer(Modifier.height(Dimens.SpaceSm))
            RoleChip(role = participant.role)
        }
    }
}

/**
 * What the account is, in the server's words.
 *
 * Shown for everyone rather than only for the unusual roles. On the admin's own
 * scanner "Participant" is the answer they expect and its absence would be the
 * signal — and a pass belonging to staff or a booth owner is exactly the case
 * where a missing chip would read as a rendering fault rather than as a fact.
 */
@Composable
private fun RoleChip(role: AccountRole, modifier: Modifier = Modifier) {
    val label = when (role) {
        AccountRole.Participant -> stringResource(R.string.profile_role_student)
        AccountRole.Staff -> stringResource(R.string.profile_role_staff)
        AccountRole.Admin -> stringResource(R.string.profile_role_admin)
        AccountRole.BoothOwner -> stringResource(R.string.profile_role_booth)
        AccountRole.Unknown -> stringResource(R.string.profile_role_unknown)
    }

    Text(
        text = label,
        modifier = modifier
            .clip(RoundedCornerShape(ChipRadius))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
        fontFamily = AppSans,
        fontWeight = AppTextWeight,
        fontSize = 11.sp,
        color = Ink.Label,
    )
}

/**
 * The one alarming thing this page can say.
 *
 * A banner rather than another muted line in the details, because it is not a
 * detail: it is the server's word that somebody has already looked at this
 * account, and an admin who skims past it has missed the only reason the flag
 * exists.
 */
@Composable
private fun FlaggedBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(Palette.Alert.copy(alpha = 0.16f))
            .padding(Dimens.CardPadding)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_alert_circle),
            contentDescription = null,
            tint = Palette.Alert,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(Dimens.Space))
        Text(
            text = stringResource(R.string.scan_student_flagged),
            fontFamily = AppSans,
            fontWeight = AppTextWeight,
            fontSize = 13.sp,
            lineHeight = 1.4.em,
            color = Palette.Alert,
        )
    }
}

/** School, major, and how far along the fair this person is. */
@Composable
private fun DetailsCard(participant: Participant, modifier: Modifier = Modifier) {
    val unset = stringResource(R.string.profile_unset)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = CardRadius)
            .padding(horizontal = Dimens.CardPadding),
    ) {
        val rows = listOf(
            Triple(
                R.drawable.ic_school,
                stringResource(R.string.profile_school),
                participant.school.orEmpty(),
            ),
            Triple(
                R.drawable.ic_major,
                stringResource(R.string.profile_major),
                participant.major.orEmpty(),
            ),
            Triple(
                R.drawable.ic_scan,
                stringResource(R.string.scan_participant_checkpoints),
                // The count is never blank and never "not set": a student who has
                // scanned nothing has scanned zero booths, which is an answer.
                stringResource(R.string.scan_student_visited, participant.visited),
            ),
        )
        rows.forEachIndexed { index, (icon, label, value) ->
            DetailRow(
                icon = icon,
                label = label,
                value = value.ifBlank { unset },
                muted = value.isBlank(),
            )
            if (index != rows.lastIndex) Hairline()
        }
    }
}

@Composable
private fun DetailRow(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.Space)
            // One announcement per row — "School, Not set" — rather than TalkBack
            // stopping on the label and the value as two unrelated fragments.
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Ink.Faint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(Dimens.Space))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 11.sp,
                color = Ink.Muted,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = value,
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 14.sp,
                lineHeight = 1.35.em,
                color = if (muted) Ink.Muted else Ink.Label,
            )
        }
    }
}

/** A stand-in for `@Preview`. Nothing in the running app reaches for it. */
private val PreviewParticipant = Participant(
    id = 1,
    firstName = "Yion",
    surname = "Suriya",
    email = "6831503029@lamduan.mfu.ac.th",
    studentId = "6831503029",
    phone = "0683150329",
    school = "Applied Digital Technology",
    major = "Software Engineering",
    role = AccountRole.Participant,
    isFlagged = false,
    visited = 15,
)

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun ParticipantScreenPreview() {
    SUClubFairTheme {
        Box(Modifier.fillMaxSize()) {
            MeshBackground()
            ParticipantScreen(participant = PreviewParticipant)
        }
    }
}

/** The case the page exists to make unmissable. */
@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun ParticipantFlaggedPreview() {
    SUClubFairTheme {
        Box(Modifier.fillMaxSize()) {
            MeshBackground()
            ParticipantScreen(
                participant = PreviewParticipant.copy(
                    isFlagged = true,
                    school = null,
                    visited = 0,
                ),
            )
        }
    }
}
