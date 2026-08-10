package com.su.clubfair.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLogin: (phone: String, password: String) -> Unit = { _, _ -> },
    onSignUp: () -> Unit = {},
    onGoogleLogin: () -> Unit = {},
) {
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    AuthBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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

            Spacer(Modifier.height(Dimens.SpaceLg))
            AuthTextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = stringResource(R.string.auth_phone_hint),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
            )
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
                text = stringResource(R.string.login_cta),
                onClick = { onLogin(phone, password) },
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
        LoginScreen()
    }
}
