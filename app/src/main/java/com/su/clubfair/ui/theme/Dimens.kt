package com.su.clubfair.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The app's spacing and radius scale.
 *
 * Every screen was picking its own numbers — radii of 12, 13, 14, 16, 18, 20, 22
 * and 24, card padding of 13, 14, 18 and 22, gaps of 10, 12, 20, 22 and 28. None
 * of it was wrong on its own, but side by side the surfaces stopped looking like
 * one app: cards in the same column had different corners, and identical-looking
 * blocks sat at different distances.
 *
 * Everything below is on a 4dp grid. Reach for these rather than a literal `.dp`
 * in a screen; if something genuinely needs a new step, add it here so the next
 * screen can use the same one.
 */
object Dimens {
    /** Inside a text block — label to value, icon to its caption. */
    val SpaceXs = 4.dp

    /** Within a component — icon to text, chip innards. */
    val SpaceSm = 8.dp

    /** Between sibling cards in a stack, and between grid cells. */
    val Space = 12.dp

    /** Title to first card, and between groups of related cards. */
    val SpaceLg = 20.dp

    /** Major separation — content to a footer. */
    val SpaceXl = 32.dp

    /** Screen gutter, shared by every tab so the nav bar lines up with content. */
    val ScreenPadding = 24.dp

    /** Inside any glass card. */
    val CardPadding = 16.dp

    /** Icon tiles and other small squares nested inside a card. */
    val RadiusSm = 12.dp

    /** Chips, inputs, and panes nested inside a card (one step in from [RadiusLg]). */
    val RadiusMd = 16.dp

    /** Every top-level glass card. */
    val RadiusLg = 20.dp

    /** Fully rounded — pills, filter chips, the nav bar. */
    val RadiusPill = 50.dp

    /** Floating nav bar. */
    val NavBarHeight = 56.dp

    /**
     * What every tab has to leave clear for the floating nav bar.
     *
     * The bar floats over the bottom of every tab, so this is what a tab pads its
     * bottom by for its last card to scroll clear of it. Four screens were each
     * spelling out the same sum; naming it is what stops one of them being missed
     * the next time the bar changes height.
     */
    val NavBarClearance = NavBarHeight + SpaceXl
}
