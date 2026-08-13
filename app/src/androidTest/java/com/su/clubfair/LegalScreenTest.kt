package com.su.clubfair

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.su.clubfair.ui.auth.LoginForm
import com.su.clubfair.ui.auth.RegisterGoogleScreen
import com.su.clubfair.ui.legal.LegalDocument
import com.su.clubfair.ui.legal.LegalScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The policies, and the consent that points at them.
 *
 * Worth a test of its own because the failure this replaces was invisible from
 * inside the app: the two documents were addresses on a site that had never
 * published them, so every route to them "worked" — a row that highlighted, a
 * browser that opened — and none of them ever put a policy in front of anybody.
 * Nothing here can catch a bad URL. What it can catch is the version of that
 * failure that could come back: a consent sentence with no way into either
 * document, or a document that renders as a title and nothing else.
 */
@RunWith(AndroidJUnit4::class)
class LegalScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun privacyPolicy_isCarriedInTheApp() {
        composeTestRule.setContent { LegalScreen(document = LegalDocument.Privacy) }

        composeTestRule.onNodeWithText("Privacy Policy").assertIsDisplayed()
        composeTestRule.onNodeWithText("What is collected").assertIsDisplayed()
        // Scrolled to rather than asserted where it lies: the document is longer
        // than a phone, so a section from the far end is off screen at rest and
        // "is it displayed" would only be asking about the viewport. Reaching it
        // by scrolling is the question worth asking — it fails both if the
        // section stops being rendered and if the page stops scrolling.
        composeTestRule.onNodeWithText("The camera").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Your rights").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun termsOfService_isCarriedInTheApp() {
        composeTestRule.setContent { LegalScreen(document = LegalDocument.Terms) }

        composeTestRule.onNodeWithText("Terms of Service").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Collecting stamps fairly")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun legalScreen_closes() {
        var closed = false
        composeTestRule.setContent {
            LegalScreen(document = LegalDocument.Terms, onBack = { closed = true })
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(closed)
    }

    /**
     * The sign-up screen a student actually reaches asks for consent.
     *
     * The sentence used to live on `RegisterScreen` — the email form, which
     * nothing routes to any more — so the shipping flow created an account
     * without showing anyone what they were agreeing to.
     */
    @Test
    fun googleSignUp_asksForConsentAndNamesBothDocuments() {
        composeTestRule.setContent {
            RegisterGoogleScreen(state = LoginForm(), googleAvailable = true)
        }

        composeTestRule
            .onNodeWithText("Terms of Service", substring = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Privacy Policy", substring = true)
            .assertIsDisplayed()
    }
}
