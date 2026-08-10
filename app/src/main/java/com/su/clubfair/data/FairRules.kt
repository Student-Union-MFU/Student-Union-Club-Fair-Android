package com.su.clubfair.data

import java.util.Calendar
import java.util.TimeZone

/**
 * The rules of the fair that a phone can settle on its own.
 *
 * The distinction this file draws is the one that matters everywhere else in the
 * app: how many prizes a student has earned is a function of how many booths
 * they have scanned, so a phone can answer it; where they *rank* against every
 * other student is not, so a phone cannot, and the UI has to say so rather than
 * print a number that came from nowhere. Home used to show `#42`.
 */
object FairRules {

    /**
     * Scan counts that earn a prize tier.
     *
     * 20 is the one a student is actually chasing — it is the prize-draw
     * threshold the announcements channel quotes. 10 is a halfway marker so the
     * count means something before then, and 27 is the clean sweep.
     */
    val PrizeMilestones = listOf(10, 20, 27)

    /** How many tiers [visited] booths has earned. */
    fun prizesFor(visited: Int): Int = PrizeMilestones.count { visited >= it }

    /** The next tier to aim at, or null once they are all earned. */
    fun nextMilestone(visited: Int): Int? = PrizeMilestones.firstOrNull { visited < it }
}

/**
 * When the fair runs.
 *
 * **These are placeholder dates**, matching the "26–27 Jul" printed on the
 * student pass. Set them to the real ones before release — they are the only
 * thing the countdown on Home reads, and a wrong end time is worse than none,
 * because a student plans the end of their afternoon around it.
 *
 * Fixed to Bangkok rather than the device's zone on purpose: the fair opens at
 * 09:00 local to the campus whatever a visiting student's phone is set to.
 *
 * `java.util.Calendar` rather than `java.time`, which needs API 26 or core
 * library desugaring; this app's floor is 24 and a two-field date range is not
 * worth either.
 */
object FairSchedule {

    private val Campus: TimeZone = TimeZone.getTimeZone("Asia/Bangkok")

    val startMillis: Long = campusTime(2026, Calendar.JULY, 26, hour = 9, minute = 0)
    val endMillis: Long = campusTime(2026, Calendar.JULY, 27, hour = 17, minute = 0)

    fun statusAt(nowMillis: Long): FairStatus = when {
        nowMillis < startMillis -> FairStatus.BeforeStart(startMillis - nowMillis)
        nowMillis < endMillis -> FairStatus.Running(endMillis - nowMillis)
        else -> FairStatus.Ended
    }

    private fun campusTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(Campus).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
}

/** Where the clock stands relative to the fair, and how long until that changes. */
sealed interface FairStatus {
    data class BeforeStart(val untilStartMillis: Long) : FairStatus
    data class Running(val remainingMillis: Long) : FairStatus
    data object Ended : FairStatus
}
