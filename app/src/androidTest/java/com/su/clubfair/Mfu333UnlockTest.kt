package com.su.clubfair

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.su.clubfair.data.PrizeUnlock
import com.su.clubfair.data.ScanOutcome
import com.su.clubfair.ui.model.FairProgress
import com.su.clubfair.ui.model.PreviewStudent
import com.su.clubfair.ui.model.PrizeTier
import com.su.clubfair.ui.prizes.PrizesScreen
import com.su.clubfair.ui.scan.ScanScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The two halves of the fifteenth booth: the card at the camera, and the seal
 * coming off the code.
 *
 * Instrumented rather than JVM tests because both are Compose, and both are
 * about *when* something appears rather than about a value — which is exactly
 * what the previous shape of this feature got wrong in silence.
 *
 * `./gradlew connectedAndroidTest`. The scan cases need CAMERA granted to the
 * test app, or the screen requests it on composition and the system dialog
 * lands on top of the assertions:
 *
 *     adb shell pm grant com.su.clubfair.debug android.permission.CAMERA
 */
@RunWith(AndroidJUnit4::class)
class Mfu333UnlockTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Both screens carry an endless animation — the scanner's reticle sweep, the
     * roadmap's "you are here" pulse — so an auto-advancing clock never reaches
     * idle and every `onNode…` call would block. The reveal cases drive this
     * clock deliberately, which is the point of them.
     */
    @Before
    fun pinTheClock() {
        composeTestRule.mainClock.autoAdvance = false
    }

    private fun tier(threshold: Int = Mfu333Threshold) = PrizeTier(
        id = 1,
        threshold = threshold,
        name = "Prize 1",
        description = null,
        reached = true,
        claimed = false,
    )

    /** Fifteen booths walked, first tier earned, second still ahead. */
    private fun earnedProgress() = FairProgress(
        visited = Mfu333Threshold,
        total = 28,
        visitedBoothIds = (1..Mfu333Threshold).toSet(),
        prizes = listOf(
            tier(),
            PrizeTier(2, 28, "Prize 2", null, reached = false, claimed = false),
        ),
    )

    // ---- The card at the camera ------------------------------------------

    @Test
    fun scanThatCrossesTheThreshold_announcesTheUnlock() {
        composeTestRule.setContent {
            ScanScreen(
                outcome = ScanOutcome.Recorded(
                    booth = null,
                    unlocked = PrizeUnlock(tier(), isMfu333 = true),
                ),
            )
        }

        composeTestRule.onNodeWithText("MFU333 unlocked").assertIsDisplayed()
        // The threshold, not the visited count — see `UnlockCard`.
        composeTestRule.onNodeWithText("15 booths walked").assertIsDisplayed()
        composeTestRule.onNodeWithText("See my code").assertIsDisplayed()
    }

    /**
     * The regression that matters most here: an ordinary checkpoint must stay
     * ordinary. A celebration on every scan is worse than none.
     */
    @Test
    fun ordinaryScan_doesNotAnnounceAnUnlock() {
        composeTestRule.setContent {
            ScanScreen(outcome = ScanOutcome.Recorded(booth = null))
        }

        composeTestRule.onNodeWithText("MFU333 unlocked").assertDoesNotExistNow()
        composeTestRule.onNodeWithText("Checkpoint recorded").assertIsDisplayed()
    }

    @Test
    fun unlockCard_seeMyCodeOpensThePrizePage() {
        var opened = false
        composeTestRule.setContent {
            ScanScreen(
                outcome = ScanOutcome.Recorded(
                    booth = null,
                    unlocked = PrizeUnlock(tier(), isMfu333 = true),
                ),
                onOpenPrizes = { opened = true },
            )
        }

        composeTestRule.onNodeWithText("See my code").performClick()

        assertTrue(opened)
    }

    /**
     * Fifteen is the first prize, not the last. The way back to the camera has
     * to survive the celebration.
     */
    @Test
    fun unlockCard_stillOffersTheWayBackToTheCamera() {
        var again = false
        composeTestRule.setContent {
            ScanScreen(
                outcome = ScanOutcome.Recorded(
                    booth = null,
                    unlocked = PrizeUnlock(tier(), isMfu333 = true),
                ),
                onClearScan = { again = true },
            )
        }

        composeTestRule.onNodeWithText("Scan another").performClick()

        assertTrue(again)
    }

    // ---- The seal coming off ----------------------------------------------

    @Test
    fun firstVisitAfterUnlocking_playsTheRevealAndRemembersIt() {
        var celebrated = false
        composeTestRule.setContent {
            PrizesScreen(
                progress = earnedProgress(),
                student = PreviewStudent,
                mfu333RevealSeen = false,
                onUnlockCelebrated = { celebrated = true },
            )
        }

        // Nothing is spent up front: a student who backs straight out has not
        // seen the one showing this reward gets.
        composeTestRule.mainClock.advanceTimeByFrame()
        assertFalse("marked seen before the reveal finished", celebrated)

        composeTestRule.mainClock.advanceTimeBy(RevealSettleMillis)
        composeTestRule.mainClock.advanceTimeByFrame()

        assertTrue("reveal finished without being remembered", celebrated)
    }

    /**
     * The other half of "plays once". Without the persisted flag this is the
     * visit that would replay the animation, which is how a celebration becomes
     * wallpaper.
     */
    @Test
    fun laterVisits_doNotReplayTheReveal() {
        var celebrated = false
        composeTestRule.setContent {
            PrizesScreen(
                progress = earnedProgress(),
                student = PreviewStudent,
                mfu333RevealSeen = true,
                onUnlockCelebrated = { celebrated = true },
            )
        }

        composeTestRule.mainClock.advanceTimeBy(RevealSettleMillis)
        composeTestRule.mainClock.advanceTimeByFrame()

        assertFalse("a settled page replayed the unlock", celebrated)
    }

    /** A code that is not earned yet has nothing to reveal, seen flag or not. */
    @Test
    fun lockedCode_neverCelebrates() {
        var celebrated = false
        composeTestRule.setContent {
            PrizesScreen(
                progress = earnedProgress().copy(
                    visited = 7,
                    prizes = earnedProgress().prizes.map { it.copy(reached = false) },
                ),
                student = PreviewStudent,
                mfu333RevealSeen = false,
                onUnlockCelebrated = { celebrated = true },
            )
        }

        composeTestRule.mainClock.advanceTimeBy(RevealSettleMillis)
        composeTestRule.mainClock.advanceTimeByFrame()

        assertFalse("a locked code celebrated", celebrated)
    }

    private companion object {
        /**
         * The fair's first threshold. A local constant, not a value read from
         * the app: the app's copy is the server's, and a test that reads the
         * same source as the code under test asserts nothing.
         */
        const val Mfu333Threshold = 15

        /** Comfortably past `PrizesScreen.RevealMillis`, which is private. */
        const val RevealSettleMillis = 2_000L
    }
}

/**
 * `assertDoesNotExist` reads as an assertion about the past tense everywhere
 * else in this file; this keeps the sentence in the same tense as its
 * neighbours.
 */
private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertDoesNotExistNow() =
    assertDoesNotExist()
