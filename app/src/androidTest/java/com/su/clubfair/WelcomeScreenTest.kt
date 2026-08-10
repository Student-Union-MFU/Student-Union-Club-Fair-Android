package com.su.clubfair

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.su.clubfair.ui.welcome.WelcomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, runs on a device or emulator via `./gradlew connectedAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_showsTitleAndCta() {
        composeTestRule.setContent { WelcomeScreen() }

        composeTestRule.onNodeWithText("Club Fair").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed()
    }
}
