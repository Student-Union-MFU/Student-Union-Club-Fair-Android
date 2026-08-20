package com.su.clubfair

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.su.clubfair.ui.model.AccountRole
import com.su.clubfair.ui.model.Participant
import com.su.clubfair.ui.model.previewProgram
import com.su.clubfair.ui.program.ProgramScreen
import com.su.clubfair.ui.scan.ParticipantScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The two places the app has to say what it just found.
 *
 * The admin's scanned pass, which is a page rather than a card over a live
 * camera, and the running order's mark on the entry that is on right now.
 */
@RunWith(AndroidJUnit4::class)
class ScanResultPageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Both screens animate forever — the live dot's siblings, the route rail. */
    @Before
    fun pinTheClock() {
        composeTestRule.mainClock.autoAdvance = false
    }

    private val person = Participant(
        id = 1,
        firstName = "Yion",
        surname = "Suriya",
        email = "6831503029@lamduan.mfu.ac.th",
        studentId = "6831503029",
        phone = "0683150329",
        school = "Applied Digital Technology",
        major = "Software Engineering",
        role = AccountRole.Participant,
        isFlagged = false,
        visited = 15,
    )

    // ---- The scanned pass, as a page --------------------------------------

    @Test
    fun scannedPass_showsWhoItBelongsTo() {
        composeTestRule.setContent { ParticipantScreen(participant = person) }

        composeTestRule.onNodeWithText("Scanned pass").assertIsDisplayed()
        composeTestRule.onNodeWithText("Yion Suriya").assertIsDisplayed()
        composeTestRule.onNodeWithText("6831503029").assertIsDisplayed()
        composeTestRule.onNodeWithText("Participant").assertIsDisplayed()
        composeTestRule.onNodeWithText("15 booths scanned").assertIsDisplayed()
    }

    /**
     * The contact details su-server sends with every participant stay off this
     * screen on purpose — it is held up at a booth in front of a queue. The page
     * has room for them; that was never the reason they were left out.
     */
    @Test
    fun scannedPass_doesNotShowContactDetails() {
        composeTestRule.setContent { ParticipantScreen(participant = person) }

        composeTestRule.onNodeWithText(person.email).assertDoesNotExist()
        composeTestRule.onNodeWithText("0683150329").assertDoesNotExist()
    }

    /** A flag is the one thing here that changes what the admin should do. */
    @Test
    fun aFlaggedAccountSaysSo() {
        composeTestRule.setContent {
            ParticipantScreen(participant = person.copy(isFlagged = true))
        }

        composeTestRule.onNodeWithText("This account is flagged").assertIsDisplayed()
    }

    @Test
    fun anUnflaggedAccountStaysQuiet() {
        composeTestRule.setContent { ParticipantScreen(participant = person) }

        composeTestRule.onNodeWithText("This account is flagged").assertDoesNotExist()
    }

    /** The way back to the camera. Without it the admin scans one pass, ever. */
    @Test
    fun scanAnotherReturnsToTheScanner() {
        var back = false
        composeTestRule.setContent {
            ParticipantScreen(participant = person, onBack = { back = true })
        }

        composeTestRule.onNodeWithText("Scan another").performClick()

        assertTrue(back)
    }

    // ---- The running order's mark on what is on now ------------------------

    /**
     * Twice: the hero card at the top of the page, and the badge on the row in
     * the running order below it. One occurrence would mean the row lost its
     * badge and is back to being distinguished by an accent hairline alone —
     * which is invisible to a screen reader and easy to miss in a bright hall.
     */
    @Test
    fun theRunningEntryIsLabelledInWords() {
        composeTestRule.setContent {
            ProgramScreen(program = previewProgram(System.currentTimeMillis()))
        }

        val labels = composeTestRule
            .onAllNodesWithText("Happening now")
            .fetchSemanticsNodes()

        assertEquals("hero card and running-order row should both say it", 2, labels.size)
    }
}
