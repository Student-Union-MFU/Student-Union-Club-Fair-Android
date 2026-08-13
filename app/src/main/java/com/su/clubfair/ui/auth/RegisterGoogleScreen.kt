package com.su.clubfair.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.legal.LegalConsentNotice
import com.su.clubfair.ui.legal.LegalDocument
import com.su.clubfair.ui.legal.LegalScreen
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.SUClubFairTheme

/**
 * The gate in front of the sign-up form: identity first, details second.
 *
 * Sign-up used to open straight onto the form, which asked for a name, a phone
 * number and a password before it knew who was filling it in. Putting Google
 * ahead of it means the account exists — and is verified by someone other than
 * us — before the form asks for anything, and the form behind it is then only
 * ever collecting what Google cannot tell us: a phone number, a school, a major
 * and a password. That form is `CompleteProfileScreen`, and the gate in
 * `MainActivity` routes to it on its own once the account exists.
 *
 * Google is the only way to sign up. There is no email form behind this screen:
 * every account is an MFU one, the address *is* the student id, and a form that
 * asks a student to type both — plus a name and a surname Google already knows —
 * collects in six fields what one tap establishes better, since Google verifies
 * the address and a typed one proves nothing. `RegisterScreen` still exists and
 * nothing routes to it; see `AuthStep` in `MainActivity` for why it was kept.
 *
 * The cost is real and worth naming: a student whose Google session is not on
 * the phone in their hand cannot sign up on it. They can still *sign in* with a
 * phone number and password once the account exists, which is what that path is
 * for.
 *
 * [state] is the login form's, deliberately. Every Google outcome — the sheet
 * failing, the server refusing an ineligible intake, no signal — lands in
 * `AuthViewModel`'s login state whichever screen started it, and giving this one
 * its own copy would mean an error raised here reported over there.
 */
@Composable
fun RegisterGoogleScreen(
    state: LoginForm,
    modifier: Modifier = Modifier,
    googleAvailable: Boolean = false,
    onGoogleContinue: () -> Unit = {},
    onLogin: () -> Unit = {},
) {
    // Which policy is open over this screen, if either. Local because reading one
    // is a detour within sign-up, not a step of it: back returns to the button
    // that was about to be tapped, with nothing else disturbed.
    var reading by remember { mutableStateOf<LegalDocument?>(null) }

    AuthBackground(
        modifier = modifier,
        // Full-bleed, over the form — not inside it. See `AuthBackground`.
        overlay = {
            reading?.let { document ->
                BackHandler { reading = null }
                MeshBackground()
                LegalScreen(document = document, onBack = { reading = null })
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 20.dp),
        ) {
            // Same lower-half anchor as the login form, so the two screens don't
            // shunt their headings up and down as you slide between them.
            Spacer(Modifier.weight(1f))

            Text(
                text = stringResource(R.string.register_title),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Bold,
                fontSize = 46.sp,
                lineHeight = 1.1.em,
                color = Color.White,
            )
            Spacer(Modifier.height(Dimens.SpaceXs))
            Text(
                text = stringResource(R.string.register_google_subtitle),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 1.3.em,
                color = Color.White,
            )

            // Whatever went wrong, above the button that would try again.
            AnimatedVisibility(visible = state.formError != null) {
                Column {
                    Spacer(Modifier.height(Dimens.Space))
                    state.formError?.let { AuthFormError(error = it) }
                }
            }

            Spacer(Modifier.height(Dimens.SpaceXl))
            GoogleButton(
                text = stringResource(R.string.login_google),
                onClick = onGoogleContinue,
                enabled = googleAvailable && !state.submitting,
            )

            // Sign-up is Google and nothing else, so a build with no client id
            // has no way through this screen at all. Saying so is the least the
            // screen can do: a disabled button with no explanation reads as the
            // app being broken, which — for that build — it is.
            if (!googleAvailable) {
                Spacer(Modifier.height(Dimens.SpaceXs))
                Text(
                    text = stringResource(R.string.auth_google_unavailable_signup),
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    lineHeight = 1.4.em,
                    color = Ink.Muted,
                    textAlign = TextAlign.Center,
                )
            }

            // The consent, on the screen where consent is actually given.
            //
            // It was on `RegisterScreen` — the email form, which nothing reaches
            // any more — and nowhere near this button, which is the one that
            // creates the account. So the shipping sign-up flow asked for no
            // agreement to anything and offered no way to read either document.
            // Both are tappable and both open in the app; see `LegalScreen`.
            Spacer(Modifier.height(Dimens.Space))
            LegalConsentNotice(onOpen = { reading = it })

            Spacer(Modifier.height(Dimens.SpaceXs))
            TextButton(
                onClick = onLogin,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = stringResource(R.string.register_have_account),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Color.White,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun RegisterGoogleScreenPreview() {
    SUClubFairTheme {
        RegisterGoogleScreen(state = LoginForm(), googleAvailable = true)
    }
}
