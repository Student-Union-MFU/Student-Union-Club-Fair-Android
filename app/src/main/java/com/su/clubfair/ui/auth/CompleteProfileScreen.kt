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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.data.Campus
import com.su.clubfair.ui.components.PillButton
import com.su.clubfair.ui.theme.AppSans
import com.su.clubfair.ui.theme.AppTextWeight
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.SUClubFairTheme

/**
 * The rest of sign-up, once Google has said who the student is.
 *
 * Reached only from a Google sign-in whose account has not finished signing up —
 * see the gate in `MainActivity`. It is not a screen anyone navigates *to*: the
 * account already exists by the time it appears, which is why there is no
 * "create account" button on it and no way past it except finishing or signing
 * out.
 *
 * What it does *not* ask for is the point. Name, email and student id all came
 * from Google and are not editable here; retyping a name the account already has
 * is how the two end up disagreeing. The student id is shown rather than hidden
 * because it is what the pass's QR code encodes, and a student who has been
 * given the wrong one should find that out now rather than at a booth.
 */
@Composable
fun CompleteProfileScreen(
    state: CompleteProfileForm,
    modifier: Modifier = Modifier,
    onChange: (CompleteProfileForm.() -> CompleteProfileForm) -> Unit = {},
    onSubmit: () -> Unit = {},
    onSignOut: () -> Unit = {},
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
                text = stringResource(R.string.complete_title),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 44.sp,
                lineHeight = 1.05.em,
                color = Color.White,
            )
            Spacer(Modifier.height(Dimens.SpaceSm))
            Text(
                // Names the student, so it is obvious which account is being
                // finished — a student with two Google accounts on the phone can
                // pick the wrong one, and this is where that shows.
                text = stringResource(
                    R.string.complete_subtitle,
                    "${state.firstName} ${state.surname}".trim(),
                ),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 15.sp,
                lineHeight = 1.35.em,
                color = Color.White,
            )

            state.studentId?.let { id ->
                Spacer(Modifier.height(Dimens.SpaceXs))
                Text(
                    text = stringResource(R.string.complete_student_id, id),
                    fontFamily = AppSans,
                    fontWeight = AppTextWeight,
                    fontSize = 13.sp,
                    color = Ink.Muted,
                )
            }

            AnimatedVisibility(visible = state.formError != null) {
                Column {
                    Spacer(Modifier.height(Dimens.Space))
                    state.formError?.let { AuthFormError(error = it) }
                }
            }

            Spacer(Modifier.height(24.dp))
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
                // Changing school invalidates the major under it — the old one
                // belongs to a different course list, and leaving it in place is
                // how a student ends up submitting a major their school does not
                // teach.
                onValueChange = { value -> onChange { copy(school = value, major = "") } },
                placeholder = stringResource(R.string.register_school_hint),
                options = Campus.Schools,
                modifier = Modifier.fillMaxWidth(),
                error = state.schoolError,
                showError = state.showErrors,
            )
            if (state.schoolWasDerived) {
                Spacer(Modifier.height(Dimens.SpaceXs))
                Text(
                    // Said out loud because the field is pre-filled and editable:
                    // without this it reads as something the student typed and
                    // forgot, and nobody checks their own typing.
                    text = stringResource(R.string.complete_school_derived),
                    modifier = Modifier.padding(start = Dimens.CardPadding),
                    fontFamily = AppSans,
                    fontWeight = AppTextWeight,
                    fontSize = 12.sp,
                    color = Ink.Muted,
                )
            }

            Spacer(Modifier.height(Dimens.Space))
            AuthDropdownField(
                value = state.major,
                onValueChange = { value -> onChange { copy(major = value) } },
                placeholder = stringResource(R.string.register_major_hint),
                options = state.majorOptions,
                modifier = Modifier.fillMaxWidth(),
                error = state.majorError,
                showError = state.showErrors,
            )

            Spacer(Modifier.height(Dimens.Space))
            AuthTextField(
                value = state.password,
                onValueChange = { value -> onChange { copy(password = value) } },
                placeholder = stringResource(R.string.complete_password_hint),
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
            Text(
                // The password is not a second way of proving who they are — it
                // is the whole of the phone-and-password login, which is what a
                // student falls back to when Google will not cooperate in a hall
                // with no signal.
                text = stringResource(R.string.complete_password_why),
                modifier = Modifier.padding(start = Dimens.CardPadding),
                fontFamily = AppSans,
                fontWeight = AppTextWeight,
                fontSize = 12.sp,
                lineHeight = 1.4.em,
                color = Ink.Muted,
            )

            Spacer(Modifier.height(24.dp))
            PillButton(
                text = stringResource(
                    if (state.submitting) R.string.complete_working else R.string.complete_cta,
                ),
                onClick = onSubmit,
                enabled = !state.submitting,
            )

            Spacer(Modifier.height(Dimens.SpaceSm))
            TextButton(
                onClick = onSignOut,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    // The only way out. Without it a student who picked the wrong
                    // Google account is stuck on this form with no back gesture
                    // to undo a sign-in that already happened.
                    text = stringResource(R.string.complete_wrong_account),
                    fontFamily = AppSans,
                    fontWeight = AppTextWeight,
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
private fun CompleteProfileScreenPreview() {
    SUClubFairTheme {
        CompleteProfileScreen(
            state = CompleteProfileForm(
                firstName = "Yion",
                surname = "Suriya",
                studentId = "6831503029",
                school = "School of Applied Digital Technology",
            ),
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun CompleteProfileScreenErrorPreview() {
    SUClubFairTheme {
        CompleteProfileScreen(
            state = CompleteProfileForm(
                firstName = "Yion",
                surname = "Suriya",
                studentId = "6831503029",
                school = "School of Applied Digital Technology",
                phone = "123",
                password = "short",
                showErrors = true,
            ),
        )
    }
}
