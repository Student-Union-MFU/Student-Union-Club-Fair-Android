package com.su.clubfair.ui.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.data.Links
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.components.ActionRow
import com.su.clubfair.ui.components.Hairline
import com.su.clubfair.ui.components.SheetHeader
import com.su.clubfair.ui.components.StatEntry
import com.su.clubfair.ui.components.StatPane
import com.su.clubfair.ui.components.StudentAvatar
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.model.FairProgress
import com.su.clubfair.ui.model.PreviewProgress
import com.su.clubfair.ui.model.PreviewStudent
import com.su.clubfair.ui.model.Student
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.LocalAccent
import com.su.clubfair.ui.theme.SUClubFairTheme

private val CardRadius = Dimens.RadiusLg

/**
 * The Profile tab: who's signed in, what they've done at the fair, and the
 * details they gave at registration.
 *
 * Everything shown comes from [Student] — the same holder Home and the pass read,
 * so the name and ID can't disagree between tabs. Nothing here is editable yet;
 * there's no backend to write to.
 *
 * The details are what sign-up actually collected now, not a fixed set of
 * invented ones, so a field the student was never asked for reads "Not set"
 * instead of showing somebody else's school.
 */
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    student: Student = PreviewStudent,
    progress: FairProgress = PreviewProgress,
    onBack: () -> Unit = {},
    onOpenPass: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val scroll = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        // No backdrop here: AppShell paints one behind every tab, so a tab that
        // painted its own would stack a second full-screen gradient on it.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(scroll)
                .padding(horizontal = Dimens.ScreenPadding),
        ) {
            // Profile is a sheet now rather than a tab, so it carries its own way
            // out. The button matches Home's top-bar buttons, which is where the
            // way *in* is — the same control, mirrored.
            Spacer(Modifier.height(Dimens.Space))
            SheetHeader(
                title = stringResource(R.string.profile_title),
                onBack = onBack,
                backDescription = stringResource(R.string.profile_back),
            )

            Spacer(Modifier.height(Dimens.SpaceLg))
            IdentityCard(student = student)

            // The pass used to be its own tab. It lives here now because it's a
            // thing you fetch and put away, not a place you browse to — Scan took
            // the tab, since that's the action you take standing at a booth.
            Spacer(Modifier.height(Dimens.Space))
            PassRow(onClick = onOpenPass)

            Spacer(Modifier.height(Dimens.Space))
            StatPane(
                entries = listOf(
                    StatEntry(
                        value = "${progress.visited}/${progress.total}",
                        label = stringResource(R.string.profile_stat_booths),
                    ),
                    StatEntry(
                        // The server ranks by check-in count. An em dash for a
                        // student who has scanned nothing: they are not in the
                        // running, and a made-up position is what `#42` was.
                        value = progress.rank?.let { "#$it" }
                            ?: stringResource(R.string.home_stat_unknown),
                        label = stringResource(R.string.home_stat_rank),
                    ),
                    StatEntry(
                        value = "${progress.prizesEarned}",
                        label = stringResource(R.string.home_stat_prizes),
                    ),
                ),
            )

            Spacer(Modifier.height(Dimens.Space))
            DetailsCard(student = student)

            Spacer(Modifier.height(Dimens.Space))
            SettingsRow(onClick = onOpenSettings)

            Spacer(Modifier.height(Dimens.Space))
            SignOutButton(onClick = onSignOut)

            Spacer(Modifier.height(Dimens.SpaceXl))
            LogoRow()

            Spacer(Modifier.height(Dimens.Space))
            LegalLinks()

            // A sheet, not a tab: the nav bar is behind this, so the old
            // NavBarClearance would just be dead space at the end of the scroll.
            Spacer(Modifier.height(Dimens.SpaceXl))
        }
    }
}

/** Way through to [com.su.clubfair.ui.settings.SettingsScreen]. */
@Composable
private fun SettingsRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = CardRadius)
            .padding(horizontal = Dimens.CardPadding),
    ) {
        ActionRow(
            icon = R.drawable.ic_settings,
            title = stringResource(R.string.profile_settings),
            subtitle = stringResource(R.string.profile_settings_hint),
            onClick = onClick,
        )
    }
}

@Composable
private fun IdentityCard(student: Student, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = CardRadius)
            .padding(Dimens.CardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The Google account photo when there is one, initials when there is not
        // — which is still most students, since a phone-and-password sign-up
        // brings no photo with it. Both cases live in the one composable.
        StudentAvatar(student = student, size = 64.dp)

        Spacer(Modifier.size(Dimens.CardPadding))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.name,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = student.studentId ?: student.email,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = Ink.Muted,
            )
            Spacer(Modifier.height(Dimens.SpaceSm))
            VerifiedChip()
        }
    }
}

/** Marks the pass as valid for the fair — the profile's tie back to the Pass tab. */
@Composable
private fun VerifiedChip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusPill))
            .background(LocalAccent.current.copy(alpha = 0.16f))
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_badge),
            contentDescription = null,
            tint = LocalAccent.current,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.size(Dimens.SpaceXs))
        Text(
            text = stringResource(R.string.profile_pass_active),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = LocalAccent.current,
        )
    }
}

@Composable
private fun DetailsCard(student: Student, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = CardRadius)
            .padding(horizontal = Dimens.CardPadding),
    ) {
        // Anything sign-up didn't collect says so, rather than rendering a blank
        // line that reads as the row having failed to load. Email is the one
        // that is routinely unset: it comes from the Google step, which is still
        // a stub.
        val unset = stringResource(R.string.profile_unset)
        val rows = listOf(
            Triple(
                R.drawable.ic_badge,
                stringResource(R.string.profile_student_id),
                student.studentId.orEmpty(),
            ),
            Triple(R.drawable.ic_mail, stringResource(R.string.profile_email), student.email),
            Triple(
                R.drawable.ic_phone,
                stringResource(R.string.profile_phone),
                student.phone.orEmpty(),
            ),
            Triple(
                R.drawable.ic_school,
                stringResource(R.string.profile_school),
                student.school.orEmpty(),
            ),
            Triple(
                R.drawable.ic_major,
                stringResource(R.string.profile_major),
                student.major.orEmpty(),
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
            // One announcement per row — "Email, Not set" — rather than TalkBack
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
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = Ink.Muted,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = value,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = if (muted) Ink.Muted else Ink.Label,
            )
        }
    }
}

/** Opens the student's pass as a sheet — see `AppShell`. */
@Composable
private fun PassRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = CardRadius)
            .clip(RoundedCornerShape(CardRadius))
            .clickable(onClick = onClick)
            .padding(Dimens.CardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Dimens.RadiusSm))
                .background(LocalAccent.current.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_scan),
                contentDescription = null,
                tint = LocalAccent.current,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.size(Dimens.CardPadding))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.profile_pass_row),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.profile_pass_row_hint),
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
}

/**
 * Ghost rather than the white [com.su.clubfair.ui.components.PillButton]: signing
 * out is the one destructive thing on the page, and it shouldn't outshout the
 * content the way a solid white pill would.
 */
@Composable
private fun SignOutButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = Dimens.RadiusLg)
            .clip(RoundedCornerShape(Dimens.RadiusLg))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_log_out),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.size(Dimens.SpaceSm))
        Text(
            text = stringResource(R.string.profile_sign_out),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color.White,
        )
    }
}

/**
 * The same wording the sign-up screen makes you agree to, kept reachable after.
 *
 * These were plain `Text`. Two policy names, styled like links, going nowhere —
 * on a screen whose sign-up counterpart makes a student agree to both by tapping
 * a button. They open now; see [Links] for the pages that have to exist.
 *
 * Each is a 48dp target rather than an 11sp line of type, which is what a
 * tappable thing has to be whatever size its label is set at.
 */
@Composable
private fun LegalLinks(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val noBrowser = stringResource(R.string.settings_no_browser)
    val open: (String) -> Unit = { url ->
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, noBrowser, Toast.LENGTH_SHORT).show()
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegalLink(
            label = stringResource(R.string.register_legal_terms),
            onClick = { open(Links.Terms) },
        )
        Text(
            text = "·",
            fontFamily = AlanSans,
            fontSize = 11.sp,
            color = Ink.Faint,
        )
        LegalLink(
            label = stringResource(R.string.register_legal_privacy),
            onClick = { open(Links.Privacy) },
        )
    }
}

@Composable
private fun LegalLink(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(Dimens.RadiusSm))
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = Dimens.Space),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = AlanSans,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            textDecoration = TextDecoration.Underline,
            color = Ink.Muted,
        )
    }
}

/** The same three marks as the welcome screen footer, tying the flow together. */
@Composable
private fun LogoRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_mfu),
            contentDescription = stringResource(R.string.logo_mfu),
            modifier = Modifier.height(30.dp),
        )
        Spacer(Modifier.size(Dimens.SpaceLg))
        Image(
            painter = painterResource(R.drawable.logo_su),
            contentDescription = stringResource(R.string.logo_su),
            modifier = Modifier.height(24.dp),
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun ProfilePreview() {
    SUClubFairTheme {
        // The shell supplies the backdrop in the app; stand one in for the preview.
        Box {
            MeshBackground()
            ProfileScreen()
        }
    }
}
