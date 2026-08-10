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
    fun loginScreen_showsWrongPasswordBanner() {
        composeTestRule.setContent {
            LoginScreen(
                state = LoginForm(
                    phone = "0683150329",
                    formError = FormError.WrongPassword,
                ),
            )
        }

        composeTestRule.onNodeWithText("Wrong password. Try again.").assertIsDisplayed()
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
            "Student ID",
            "Phone number",
            "School",
            "Major",
            "Password",
            "Create Account",
        ).forEach { composeTestRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun registerScreen_rejectsAShortStudentId() {
        composeTestRule.setContent {
            RegisterScreen(state = RegisterForm(studentId = "683", showErrors = true))
        }

        composeTestRule.onNodeWithText("A student ID is 10 digits").assertIsDisplayed()
    }

    @Test
    fun registerScreen_warnsWhenItWouldReplaceAnAccount() {
        composeTestRule.setContent {
            RegisterScreen(state = RegisterForm(), replacesExistingAccount = true)
        }

        composeTestRule
            .onNodeWithText(
                "Signing up replaces the account already on this phone, and its booth progress.",
            )
            .assertIsDisplayed()
    }
}
