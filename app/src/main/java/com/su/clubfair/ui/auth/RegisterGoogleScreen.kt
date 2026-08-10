package com.su.clubfair.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.SUClubFairTheme

/**
 * The gate in front of the sign-up form: identity first, details second.
 *
 * Sign-up used to open straight onto the form, which asked for a name, a phone
 * number and a password before it knew who was filling it in. Putting Google
 * ahead of it means the account exists — and is verified by someone other than
 * us — before the form asks for anything, and the form behind it is then only
 * ever collecting what Google can't tell us: school and major.
 *
 * The Google call itself is still a stub; see [com.su.clubfair.MainActivity].
 */
@Composable
fun RegisterGoogleScreen(
    modifier: Modifier = Modifier,
    onGoogleContinue: () -> Unit = {},
    onLogin: () -> Unit = {},
) {
    AuthBackground(modifier = modifier) {
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

            Spacer(Modifier.height(Dimens.SpaceXl))
            GoogleButton(
                text = stringResource(R.string.login_google),
                onClick = onGoogleContinue,
            )

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
        RegisterGoogleScreen()
    }
}
