package com.su.clubfair.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.data.Campus
import com.su.clubfair.ui.components.PillButton
import com.su.clubfair.ui.legal.LegalConsentNotice
import com.su.clubfair.ui.legal.LegalDocument
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.SUClubFairTheme

/**
 * The second half of sign-up: what Google's account doesn't already establish.
 *
 * This asked for a name and a password, on the reasoning that
 * [RegisterGoogleScreen] establishes the rest. It doesn't — that button is a
 * stub with no Credential Manager call behind it — so everything the app then
 * showed had to be invented, and it was: a hardcoded student id, school, major
 * and phone number that belonged to nobody using the app.
 *
 * So the form asks. Each field below is one the app then actually renders:
 *
 *  - name → the greeting on Home, the initials on the pass
 *  - student id → **the QR code on the pass**, which is the whole point of it
 *  - phone → the identifier sign-in matches against
 *  - school, major → the profile's details card
 *
 * Email is the one still left to the Google step, and Profile shows it as unset
 * rather than guessing at a `@lamduan.mfu.ac.th` address from a name.
 */
@Composable
fun RegisterScreen(
    state: RegisterForm,
    modifier: Modifier = Modifier,
    onChange: (RegisterForm.() -> RegisterForm) -> Unit = {},
    onCreateAccount: () -> Unit = {},
    onLogin: () -> Unit = {},
    /**
     * Opens a policy. A parameter rather than state held here, unlike the Google
     * sign-up screen: this one is unreferenced — see the note on `AuthStep` — so
     * the honest thing is to leave the decision to whoever revives it.
     */
    onOpenLegal: (LegalDocument) -> Unit = {},
) {
    AuthBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp),
        ) {
            Spacer(Modifier.height(Dimens.SpaceLg))
            Text(
                text = stringResource(R.string.register_title),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                lineHeight = 1.05.em,
                color = Color.White,
            )
            Spacer(Modifier.height(Dimens.SpaceSm))
            Text(
                text = stringResource(R.string.register_subtitle),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 1.35.em,
                color = Color.White,
            )

            // The answer to the last submit, above the fields so it is readable
            // while the field it refers to is being corrected.
            AnimatedVisibility(visible = state.formError != null) {
                Column {
                    Spacer(Modifier.height(Dimens.Space))
                    state.formError?.let { AuthFormError(error = it) }
                }
            }

            Spacer(Modifier.height(24.dp))
            // Side by side rather than stacked: they are two halves of one answer,
            // and a full-width field each would read as two unrelated questions.
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space),
                verticalAlignment = Alignment.Top,
            ) {
                AuthTextField(
                    value = state.firstName,
                    onValueChange = { value -> onChange { copy(firstName = value) } },
                    placeholder = stringResource(R.string.register_first_hint),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    error = state.firstNameError,
                    showError = state.showErrors,
                )
                AuthTextField(
                    value = state.surname,
                    onValueChange = { value -> onChange { copy(surname = value) } },
                    placeholder = stringResource(R.string.register_surname_hint),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    error = state.surnameError,
                    showError = state.showErrors,
                )
            }

            // The email, not the student id. The local part of an MFU address *is*
            // the student id, so su-server derives it — asking for both would be
            // two fields for one number and two chances for them to disagree.
            Spacer(Modifier.height(Dimens.Space))
            AuthTextField(
                value = state.email,
                onValueChange = { value -> onChange { copy(email = value) } },
                placeholder = stringResource(R.string.register_email_hint),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                error = state.emailError,
                showError = state.showErrors,
            )
            // Shown as soon as the address parses, so the id the server is about to
            // store is never a surprise on the profile screen afterwards.
            state.derivedStudentId?.let { id ->
                Spacer(Modifier.height(Dimens.SpaceXs))
                Text(
                    text = stringResource(R.string.register_derived_id, id),
                    modifier = Modifier.padding(start = Dimens.CardPadding),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = Ink.Muted,
                )
            }

            Spacer(Modifier.height(Dimens.Space))
            AuthTextField(
                value = state.phone,
                onValueChange = { value -> onChange { copy(phone = value) } },
                placeholder = stringResource(R.string.register_phone_hint),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                error = state.phoneError,
                showError = state.showErrors,
            )

            Spacer(Modifier.height(Dimens.Space))
            AuthDropdownField(
                value = state.school,
                onValueChange = { value -> onChange { copy(school = value) } },
                placeholder = stringResource(R.string.register_school_hint),
                options = Campus.Schools,
                modifier = Modifier.fillMaxWidth(),
                error = state.schoolError,
                showError = state.showErrors,
            )

            Spacer(Modifier.height(Dimens.Space))
            AuthTextField(
                value = state.major,
                onValueChange = { value -> onChange { copy(major = value) } },
                placeholder = stringResource(R.string.register_major_hint),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                error = state.majorError,
                showError = state.showErrors,
            )

            Spacer(Modifier.height(Dimens.Space))
            AuthTextField(
                value = state.password,
                onValueChange = { value -> onChange { copy(password = value) } },
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

            Spacer(Modifier.height(24.dp))
            PillButton(
                text = stringResource(
                    if (state.submitting) R.string.register_working else R.string.register_cta,
                ),
                onClick = onCreateAccount,
                enabled = !state.submitting,
            )

            Spacer(Modifier.height(Dimens.SpaceSm))
            // Shared with the Google sign-up screen, which is the one a student
            // actually reaches. Both policy names open the document in the app.
            LegalConsentNotice(
                onOpen = onOpenLegal,
                notice = R.string.register_legal,
            )

            Spacer(Modifier.height(4.dp))
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
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun RegisterScreenPreview() {
    SUClubFairTheme {
        RegisterScreen(state = RegisterForm())
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun RegisterScreenErrorPreview() {
    SUClubFairTheme {
        RegisterScreen(
            state = RegisterForm(
                firstName = "Yion",
                email = "yion@gmail.com",
                password = "short",
                showErrors = true,
            ),
        )
    }
}
