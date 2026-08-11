package com.su.clubfair.data

import java.util.Calendar
import java.util.TimeZone

/*
 * What is left here after su-server arrived.
 *
 * The prize thresholds used to live in this file as `PrizeMilestones = [10, 20,
 * 27]`. They are `clubfair_prize_tier` rows now, so the Student Union can move one
 * mid-fair without a Play Store release, and `rank` — which no phone can compute —
 * comes from the server as well.
 *
 * The schedule stays local because nothing serves it: `events.date`/`events.time`
 * are VARCHAR(50) over there and are the SU-wide listing, not this fair's window.
 */
/**
 * When the fair runs.
 *
 * **These are placeholder dates.** Set them to the real ones before release —
 * they are the only thing the countdown on Home reads, and a wrong end time is
 * worse than none, because a student plans the end of their afternoon around it.
 *
 * They were 26–27 July 2026, which is now in the past, so every phone showed
 * "Fair · Ended" on the one tile of the home screen that is supposed to give the
 * student a reason to hurry. A placeholder that has expired is not a neutral
 * stand-in — it actively tells students the event is over. So the window is
 * 22–23 August 2026, which is ahead of today and keeps the countdown doing the
 * job it is on the screen to do.
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

    val startMillis: Long = campusTime(2026, Calendar.AUGUST, 22, hour = 9, minute = 0)
    val endMillis: Long = campusTime(2026, Calendar.AUGUST, 23, hour = 17, minute = 0)

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
