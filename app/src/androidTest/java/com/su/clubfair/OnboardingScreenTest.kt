package com.su.clubfair

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.su.clubfair.ui.onboarding.OnboardingScreen
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, runs on a device or emulator via `./gradlew connectedAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * The hero's idle float never ends, so an auto-advancing clock never reaches
     * idle and every `onNode…` call would block. Driving the clock by hand costs
     * nothing here — none of these assertions depend on animation progress.
     */
    @Before
    fun pinTheClock() {
        composeTestRule.mainClock.autoAdvance = false
    }

    @Test
    fun onboardingScreen_showsHeadlineAndCta() {
        composeTestRule.setContent { OnboardingScreen() }

        composeTestRule
            .onNodeWithText("Visit all 27 booths, scan each QR code, and win prizes!")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").assertIsDisplayed()
    }

    @Test
    fun onboardingScreen_continueFiresCallback() {
        var continued = false
        composeTestRule.setContent { OnboardingScreen(onContinue = { continued = true }) }

        composeTestRule.onNodeWithText("Continue").performClick()

        assertTrue(continued)
    }
}
