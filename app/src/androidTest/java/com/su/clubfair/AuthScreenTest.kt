package com.su.clubfair

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.su.clubfair.ui.auth.FormError
import com.su.clubfair.ui.auth.LoginForm
import com.su.clubfair.ui.auth.LoginScreen
import com.su.clubfair.ui.auth.RegisterForm
import com.su.clubfair.ui.auth.RegisterScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, runs on a device or emulator via `./gradlew connectedAndroidTest`.
 *
 * These used to assert against fields the register screen had already dropped —
 * "Name", "Confirm", "School of Information Technology" — and passed anyway,
 * because nothing ran them. They now drive the screens through their state
 * object, which is the only way the error paths are reachable at all.
 */
@RunWith(AndroidJUnit4::class)
class AuthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_showsFieldsAndCta() {
        composeTestRule.setContent { LoginScreen(state = LoginForm()) }

        composeTestRule.onNodeWithText("Log in").assertIsDisplayed()
        composeTestRule.onNodeWithText("683XXXXXXX").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Login").assertIsDisplayed()
    }

    @Test
    fun loginScreen_signUpLinkNavigates() {
        var signUpTapped = false
        composeTestRule.setContent {
            LoginScreen(state = LoginForm(), onSignUp = { signUpTapped = true })
        }

        composeTestRule.onNodeWithText("Don't have an account? Sign up").performClick()

        assertTrue(signUpTapped)
    }

    @Test
    fun loginScreen_showsFieldErrorsOnlyWhenAsked() {
        // Same invalid input, twice, differing only in whether the form has been
        // submitted — which is the whole point of `showErrors`.
        composeTestRule.setContent {
            LoginScreen(state = LoginForm(phone = "12", password = ""))
        }
        composeTestRule
            .onNodeWithText("Enter a Thai mobile number, e.g. 0683150329")
            .assertDoesNotExist()
    }

    @Test
    fun loginScreen_showsFieldErrorsAfterSubmit() {
        composeTestRule.setContent {
            LoginScreen(state = LoginForm(phone = "12", password = "", showErrors = true))
        }

        composeTestRule
            .onNodeWithText("Enter a Thai mobile number, e.g. 0683150329")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Required").assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsTheServersOwnRejectionMessage() {
        composeTestRule.setContent {
            LoginScreen(
                state = LoginForm(
                    phone = "0683150329",
                    formError = FormError.Rejected("เบอร์โทรหรือรหัสผ่านไม่ถูกต้อง"),
                ),
            )
        }

        // The server's message, not a resource: only su-server knows whether it
        // was the number, the password or a flagged account, and it phrases that
        // in the reader's language already.
        composeTestRule
            .onNodeWithText("เบอร์โทรหรือรหัสผ่านไม่ถูกต้อง")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_ctaSaysWhatItIsDoingWhileSubmitting() {
        composeTestRule.setContent {
            LoginScreen(
                state = LoginForm(phone = "0683150329", password = "hunter22", submitting = true),
            )
        }

        composeTestRule.onNodeWithText("Signing in…").assertIsDisplayed()
    }

    @Test
    fun registerScreen_showsEveryFieldItCollects() {
        composeTestRule.setContent { RegisterScreen(state = RegisterForm()) }

        listOf(
            "First name",
            "Surname",
            "MFU email",
            "Phone number",
            "School",
            "Major",
            "Password",
            "Create Account",
        ).forEach { composeTestRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun registerScreen_requiresAnMfuEmail() {
        // The student id is no longer typed — su-server derives it from this
        // address — so the address is what the form has to police.
        composeTestRule.setContent {
            RegisterScreen(state = RegisterForm(email = "yion@gmail.com", showErrors = true))
        }

        composeTestRule.onNodeWithText("Use your @lamduan.mfu.ac.th address").assertIsDisplayed()
    }

    @Test
    fun registerScreen_showsTheStudentIdItWillDerive() {
        composeTestRule.setContent {
            RegisterScreen(state = RegisterForm(email = "6831503029@lamduan.mfu.ac.th"))
        }

        composeTestRule.onNodeWithText("Student ID 6831503029").assertIsDisplayed()
    }

    @Test
    fun loginScreen_saysWhenGoogleIsNotSetUp() {
        // The button stays visible but disabled until a Web OAuth client id is
        // built in; a control that vanishes reads as the wrong app.
        composeTestRule.setContent {
            LoginScreen(state = LoginForm(), googleAvailable = false)
        }

        composeTestRule
            .onNodeWithText(
                "Google sign-in isn't set up yet — use your phone number and password.",
            )
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsAnOfflineFailureDifferentlyFromARejection() {
        composeTestRule.setContent {
            LoginScreen(state = LoginForm(formError = FormError.Offline))
        }

        composeTestRule
            .onNodeWithText("Can't reach the server. Check your connection and try again.")
            .assertIsDisplayed()
    }
}
