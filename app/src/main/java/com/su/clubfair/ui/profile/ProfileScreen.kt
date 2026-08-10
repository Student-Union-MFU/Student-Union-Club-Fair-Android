package com.su.clubfair.ui.profile

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
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.components.GlassIconButton
import com.su.clubfair.ui.components.Hairline
import com.su.clubfair.ui.components.StatEntry
import com.su.clubfair.ui.components.StatPane
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.model.PlaceholderStudent
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
 */
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    student: Student = PlaceholderStudent,
    onBack: () -> Unit = {},
    onOpenPass: () -> Unit = {},
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(
                    icon = R.drawable.ic_arrow_back,
                    contentDescription = stringResource(R.string.profile_back),
                    onClick = onBack,
                )
                Spacer(Modifier.size(Dimens.Space))
                Text(
                    text = stringResource(R.string.profile_title),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                )
            }

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
                        value = "${student.visited}/${student.total}",
                        label = stringResource(R.string.profile_stat_booths),
                    ),
                    StatEntry(
                        value = "#${student.rank}",
                        label = stringResource(R.string.home_stat_rank),
                    ),
                    StatEntry(
                        value = "${student.prizes}",
                        label = stringResource(R.string.home_stat_prizes),
                    ),
                ),
            )

            Spacer(Modifier.height(Dimens.Space))
            DetailsCard(student = student)

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

@Composable
private fun IdentityCard(student: Student, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = CardRadius)
            .padding(Dimens.CardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(LocalAccent.current.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                // Initials rather than a stock avatar — there are no photos to
                // show, and a generic silhouette says less than the letters do.
                // Both names now that sign-up asks for both: one letter collides
                // across a fair of thousands, two mostly don't.
                text = student.initials,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = LocalAccent.current,
            )
        }

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
                text = student.studentId,
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
        val rows = listOf(
            // Email leads: it comes from the Google account sign-up now goes
            // through, which makes it the one detail here the student didn't
            // type by hand.
            Triple(R.drawable.ic_mail, stringResource(R.string.profile_email), student.email),
            Triple(R.drawable.ic_phone, stringResource(R.string.profile_phone), student.phone),
            Triple(R.drawable.ic_school, stringResource(R.string.profile_school), student.school),
            Triple(R.drawable.ic_major, stringResource(R.string.profile_major), student.major),
        )
        rows.forEachIndexed { index, (icon, label, value) ->
            DetailRow(icon = icon, label = label, value = value)
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
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.Space),
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
                color = Ink.Label,
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

/** The same wording the sign-up screen makes you agree to, kept reachable after. */
@Composable
private fun LegalLinks(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        listOf(
            stringResource(R.string.register_legal_terms),
            stringResource(R.string.register_legal_privacy),
        ).forEachIndexed { index, label ->
            if (index > 0) {
                Text(
                    text = "  ·  ",
                    fontFamily = AlanSans,
                    fontSize = 11.sp,
                    color = Ink.Faint,
                )
            }
            Text(
                text = label,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = Ink.Muted,
            )
        }
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
