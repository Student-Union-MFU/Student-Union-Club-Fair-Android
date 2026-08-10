package com.su.clubfair.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.components.PillButton
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.SUClubFairTheme

/**
 * Sign-in.
 *
 * The form is [LoginForm]'s, not the screen's: what was typed, whether it is
 * acceptable and whether a submit is in flight all live in `AuthViewModel`, so
 * none of it is lost to a rotation and the validation can be unit-tested without
 * a device. This screen decides only what that state looks like.
 *
 * It now scrolls. Two error messages and a rejected-password banner add about
 * 120dp to a form that already filled a short phone, and a CTA that cannot be
 * reached is worse than one that has to be scrolled to.
 */
@Composable
fun LoginScreen(
    state: LoginForm,
    modifier: Modifier = Modifier,
    onPhoneChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
    onSignUp: () -> Unit = {},
    onGoogleLogin: () -> Unit = {},
) {
    AuthBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp),
        ) {
            // Keeps the form in the lower half, over the darkest part of the art.
            Spacer(Modifier.weight(1f))

            Text(
                text = stringResource(R.string.login_title),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Bold,
                fontSize = 46.sp,
                lineHeight = 1.1.em,
                color = Color.White,
            )
            Spacer(Modifier.height(Dimens.SpaceXs))
            Text(
                text = stringResource(R.string.login_subtitle),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 1.3.em,
                color = Color.White,
            )

            // Above the fields, not below the button: this is the answer to the
            // last submit, and it has to be readable while the field it refers
            // to is being corrected.
            AnimatedVisibility(visible = state.formError != null) {
                Column {
                    Spacer(Modifier.height(Dimens.Space))
                    state.formError?.let { AuthFormError(error = it) }
                }
            }

            Spacer(Modifier.height(Dimens.SpaceLg))
            AuthTextField(
                value = state.phone,
                onValueChange = onPhoneChange,
                placeholder = stringResource(R.string.auth_phone_hint),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                error = state.phoneError,
                showError = state.showErrors,
            )
            Spacer(Modifier.height(Dimens.Space))
            AuthTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                placeholder = stringResource(R.string.auth_password_hint),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                isPassword = true,
                error = state.passwordError,
                showError = state.showErrors,
            )

            Spacer(Modifier.height(Dimens.SpaceXs))
            TextButton(
                onClick = onSignUp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = stringResource(R.string.login_no_account),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(Dimens.Space))
            PillButton(
                // The button says what it is doing while it does it. Sign-in
                // runs a 120k-iteration key derivation, which is deliberately
                // not instant on a mid-range phone.
                text = stringResource(
                    if (state.submitting) R.string.login_working else R.string.login_cta,
                ),
                onClick = onSubmit,
                enabled = !state.submitting,
            )

            Spacer(Modifier.height(Dimens.Space))
            AuthDivider(
                label = stringResource(R.string.login_or),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Dimens.Space))
            GoogleButton(
                text = stringResource(R.string.login_google),
                onClick = onGoogleLogin,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun LoginScreenPreview() {
    SUClubFairTheme {
        LoginScreen(state = LoginForm())
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun LoginScreenErrorPreview() {
    SUClubFairTheme {
        LoginScreen(
            state = LoginForm(
                phone = "0683",
                password = "",
                showErrors = true,
                formError = FormError.WrongPassword,
            ),
        )
    }
}
