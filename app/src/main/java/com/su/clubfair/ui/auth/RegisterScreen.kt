package com.su.clubfair.ui.auth

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
 * The second half of sign-up: what Google's account doesn't already establish.
 *
 * This was a six-field form — name, phone, password, confirmation, school and
 * major — from when it was the whole of registration. [RegisterGoogleScreen] now
 * runs ahead of it, so the identity questions belong there and this asks for two
 * things only. The school and major pickers went with the rest; `AuthDropdownField`
 * and their `register_school_hint` / `register_major_hint` strings are deliberately
 * left in place, since those two are the likeliest of the six to come back.
 */
@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    onCreateAccount: () -> Unit = {},
    onLogin: () -> Unit = {},
) {
    var firstName by rememberSaveable { mutableStateOf("") }
    var surname by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

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

            Spacer(Modifier.height(24.dp))
            // Side by side rather than stacked: they are two halves of one answer,
            // and a full-width field each would read as two unrelated questions.
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space)) {
                AuthTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    placeholder = stringResource(R.string.register_first_hint),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                )
                AuthTextField(
                    value = surname,
                    onValueChange = { surname = it },
                    placeholder = stringResource(R.string.register_surname_hint),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                )
            }
            Spacer(Modifier.height(Dimens.Space))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = stringResource(R.string.auth_password_hint),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                isPassword = true,
            )

            Spacer(Modifier.height(24.dp))
            PillButton(
                text = stringResource(R.string.register_cta),
                onClick = onCreateAccount,
            )

            Spacer(Modifier.height(Dimens.SpaceSm))
            Text(
                text = legalNotice(),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 1.45.em,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
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

/** Underlines the two policy names inside the consent sentence. */
@Composable
private fun legalNotice(): AnnotatedString {
    val notice = stringResource(R.string.register_legal)
    val terms = stringResource(R.string.register_legal_terms)
    val privacy = stringResource(R.string.register_legal_privacy)

    return buildAnnotatedString {
        append(notice)
        listOf(terms, privacy).forEach { phrase ->
            val start = notice.indexOf(phrase)
            if (start >= 0) {
                addStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline,
                    ),
                    start,
                    start + phrase.length,
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun RegisterScreenPreview() {
    SUClubFairTheme {
        RegisterScreen()
    }
}
