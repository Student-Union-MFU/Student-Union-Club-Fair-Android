package com.su.clubfair.ui.legal

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.components.Hairline
import com.su.clubfair.ui.components.SheetHeader
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.SUClubFairTheme

/** One titled block of a policy. */
internal data class Section(@StringRes val heading: Int, @StringRes val body: Int)

/**
 * The two policy documents, carried in the app.
 *
 * They used to be a pair of `https://su.mfu.ac.th/clubfair/…` addresses, and the
 * file that held them said in its own comment that the pages had to exist before
 * release. They did not. That left the app in the worst of the three possible
 * states: sign-up made a student agree to two documents by tapping a button, the
 * profile and Settings offered to show them, and every one of those taps opened a
 * browser on a 404. A policy nobody can read is not a policy, and a consent given
 * to one is not consent.
 *
 * Shipping the text instead of hosting it fixes more than the 404:
 *
 *  - It cannot rot independently of the app. A build that collects a new field is
 *    a build whose privacy policy mentions it, in the same commit and the same
 *    review.
 *  - It is readable in a concrete hall with no signal, which is exactly where
 *    this app is used and where a link is worth nothing.
 *  - It is translated by the same mechanism as everything else, so the Thai
 *    version cannot be the one somebody forgot to upload.
 *
 * Two things are still owed, and neither is code. The Student Union has to read
 * and approve this wording — it is an honest description of what the app does,
 * written for a student to read, and it is not a lawyer's work. And Google Play
 * separately requires a privacy policy at a public URL on the store listing,
 * which is a field in the console rather than anything the app can satisfy; the
 * text below is what belongs at that address.
 */
enum class LegalDocument(@StringRes internal val title: Int) {
    Terms(R.string.settings_terms),
    Privacy(R.string.settings_privacy),
    ;

    @get:StringRes
    internal val intro: Int
        get() = when (this) {
            Terms -> R.string.legal_terms_intro
            Privacy -> R.string.legal_privacy_intro
        }

    internal val sections: List<Section>
        get() = when (this) {
            Terms -> TermsSections
            Privacy -> PrivacySections
        }
}

private val TermsSections = listOf(
    Section(R.string.legal_terms_what, R.string.legal_terms_what_body),
    Section(R.string.legal_terms_who, R.string.legal_terms_who_body),
    Section(R.string.legal_terms_account, R.string.legal_terms_account_body),
    Section(R.string.legal_terms_fair, R.string.legal_terms_fair_body),
    Section(R.string.legal_terms_prizes, R.string.legal_terms_prizes_body),
    Section(R.string.legal_terms_content, R.string.legal_terms_content_body),
    Section(R.string.legal_terms_availability, R.string.legal_terms_availability_body),
    Section(R.string.legal_terms_ending, R.string.legal_terms_ending_body),
    Section(R.string.legal_terms_changes, R.string.legal_terms_changes_body),
    Section(R.string.legal_contact, R.string.legal_contact_body),
)

private val PrivacySections = listOf(
    Section(R.string.legal_privacy_who, R.string.legal_privacy_who_body),
    Section(R.string.legal_privacy_what, R.string.legal_privacy_what_body),
    Section(R.string.legal_privacy_why, R.string.legal_privacy_why_body),
    Section(R.string.legal_privacy_google, R.string.legal_privacy_google_body),
    Section(R.string.legal_privacy_device, R.string.legal_privacy_device_body),
    Section(R.string.legal_privacy_shared, R.string.legal_privacy_shared_body),
    Section(R.string.legal_privacy_camera, R.string.legal_privacy_camera_body),
    Section(R.string.legal_privacy_retention, R.string.legal_privacy_retention_body),
    Section(R.string.legal_privacy_rights, R.string.legal_privacy_rights_body),
    Section(R.string.legal_contact, R.string.legal_contact_body),
)

/**
 * A policy, as a page of the app rather than a page of the web.
 *
 * One card with hairlines between the sections, which is the same furniture
 * Settings and Profile are built from — the point being that this reads as
 * somewhere in the app you arrived at, not as a document that has been pasted
 * into it. Every heading is marked as one for TalkBack, so a reader looking for
 * "What is collected" can jump between headings instead of listening to the
 * whole thing.
 */
@Composable
fun LegalScreen(
    document: LegalDocument,
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
            title = stringResource(document.title),
            onBack = onBack,
            backDescription = stringResource(R.string.profile_back),
        )

        Spacer(Modifier.height(Dimens.SpaceLg))
        Text(
            text = stringResource(document.intro),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 1.5.em,
            color = Ink.Label,
        )
        Spacer(Modifier.height(Dimens.SpaceXs))
        Text(
            text = stringResource(R.string.legal_updated),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Ink.Muted,
        )

        Spacer(Modifier.height(Dimens.Space))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(cornerRadius = Dimens.RadiusLg)
                .padding(horizontal = Dimens.CardPadding),
        ) {
            document.sections.forEachIndexed { index, section ->
                if (index > 0) Hairline()
                Column(modifier = Modifier.padding(vertical = Dimens.Space)) {
                    Text(
                        text = stringResource(section.heading),
                        modifier = Modifier.semantics { heading() },
                        fontFamily = AlanSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(Dimens.SpaceXs))
                    Text(
                        text = stringResource(section.body),
                        fontFamily = AlanSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        // Looser than the app's other body copy. This is the one
                        // screen someone reads a paragraph at a time rather than
                        // glances at, and at 13sp over a dark backdrop the line
                        // spacing is what keeps the eye from losing its place.
                        lineHeight = 1.6.em,
                        color = Ink.Label,
                    )
                }
            }
        }

        Spacer(Modifier.height(Dimens.SpaceXl))
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun LegalPrivacyPreview() {
    SUClubFairTheme {
        androidx.compose.foundation.layout.Box {
            MeshBackground()
            LegalScreen(document = LegalDocument.Privacy)
        }
    }
}
