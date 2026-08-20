package com.su.clubfair

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.su.clubfair.ui.model.FairProgress
import com.su.clubfair.ui.model.PreviewProgress
import com.su.clubfair.ui.prizes.PrizeRoadmap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The prize route, from the two angles a drawing can fail at.
 *
 * A path is the one kind of screen where "it renders" and "it says anything" come
 * apart: a student who cannot see it gets whatever the semantics tree carries,
 * and a route made of discs and a curve carries nothing by default. So the test
 * that matters is not that the gift is on screen — it is that each stop still
 * announces itself as a name, a threshold and a distance.
 */
@RunWith(AndroidJUnit4::class)
class PrizeRoadmapTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * The route, in a scroller of its own.
     *
     * These cases used to drive it through `PrizesScreen`, and that stopped
     * testing anything the day the page dropped the route in favour of the code
     * and its explainer: every assertion below was looking for a component the
     * screen no longer draws. [PrizeRoadmap] is still a live composable and is
     * still the subject here, so it is composed directly — which is also the
     * honest scope, since none of this was ever about the page around it.
     *
     * The scroller stays because the assertions reach stops taller than a phone,
     * and `performScrollTo` needs something to scroll.
     */
    @Composable
    private fun Roadmap(progress: FairProgress) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            PrizeRoadmap(progress = progress)
        }
    }

    @Test
    fun everyStopAnnouncesWhatItIsAndHowFarOff() {
        // Seven of twenty-eight, against the fair's two tiers: eight booths to the
        // first, twenty-one to the second.
        composeTestRule.setContent { Roadmap(PreviewProgress) }

        composeTestRule
            .onNodeWithContentDescription("Prize 1, at 15 booths. 8 more booths to go")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Prize 2, at 28 booths. 21 more booths to go")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun anEarnedStopSaysSoRatherThanCountingDown() {
        composeTestRule.setContent {
            Roadmap(
                PreviewProgress.copy(
                    visited = 16,
                    prizes = PreviewProgress.prizes.mapIndexed { index, tier ->
                        tier.copy(reached = index == 0)
                    },
                ),
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Prize 1, at 15 booths. Earned — claim it at the Student Union desk",
            )
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * One booth short of a prize is the state the route is read in most, and the
     * one the copy used to get wrong — "1 more booths to go".
     */
    @Test
    fun oneBoothShortReadsAsOneBooth() {
        composeTestRule.setContent { Roadmap(PreviewProgress.copy(visited = 14)) }

        composeTestRule
            .onNodeWithContentDescription("Prize 1, at 15 booths. 1 more booth to go")
            .performScrollTo()
            .assertIsDisplayed()
    }

    /** The marker is the only thing on the route that says where the student is. */
    @Test
    fun theRouteMarksWhereTheStudentIs() {
        composeTestRule.setContent { Roadmap(PreviewProgress) }

        composeTestRule.onNodeWithText("You're here").performScrollTo().assertIsDisplayed()
    }
}
