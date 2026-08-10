package com.su.clubfair

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.su.clubfair.ui.auth.LoginScreen
import com.su.clubfair.ui.auth.RegisterScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, runs on a device or emulator via `./gradlew connectedAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class AuthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_showsFieldsAndCta() {
        composeTestRule.setContent { LoginScreen() }

        composeTestRule.onNodeWithText("Log in").assertIsDisplayed()
        composeTestRule.onNodeWithText("683XXXXXXX").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Login").assertIsDisplayed()
    }

    @Test
    fun loginScreen_signUpLinkNavigates() {
        var signUpTapped = false
        composeTestRule.setContent { LoginScreen(onSignUp = { signUpTapped = true }) }

        composeTestRule.onNodeWithText("Don't have an account? Sign up").performClick()

        assertTrue(signUpTapped)
    }

    @Test
    fun registerScreen_showsEveryField() {
        composeTestRule.setContent { RegisterScreen() }

        listOf("Name", "683XXXXXXX", "Password", "Confirm", "School", "Major", "Create Account")
            .forEach { composeTestRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun registerScreen_schoolDropdownPicksAnOption() {
        composeTestRule.setContent { RegisterScreen() }

        composeTestRule.onNodeWithText("School").performClick()
        composeTestRule.onNodeWithText("School of Information Technology").performClick()

        composeTestRule.onNodeWithText("School of Information Technology").assertIsDisplayed()
    }
}
