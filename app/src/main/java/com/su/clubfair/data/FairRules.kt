package com.su.clubfair.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
 * When the fair runs. **These are the real dates**, confirmed by the Student
 * Union: one evening, 22 August 2026, 16:00 to 21:30.
 *
 * They are no longer a placeholder, and it matters that this comment says so.
 * Two earlier windows were invented to keep the countdown ticking — 26–27 July,
 * then 22–23 August, both of them two full days from nine in the morning — and
 * both were read straight onto the welcome screen and the student pass as though
 * they were facts. A placeholder nobody has labelled outlives the person who
 * wrote it.
 *
 * **One window, one evening**, which also retires a bug rather than fixing it:
 * a two-day range modelled as a single span claimed the hall was open at three
 * in the morning between the days, because nothing here knew about daily opening
 * hours. An evening has no overnight to get wrong. If the fair ever runs two
 * days again, that gap comes back and this object needs sessions rather than a
 * wider range.
 *
 * Everything else reads these: the countdown and its badge on Home, the dates on
 * the welcome screen, and the date printed on the student pass. Nothing should
 * carry its own copy of them — a hardcoded "26–27 Jul" survived on that pass for
 * two releases after the fair moved.
 *
 * Fixed to Bangkok rather than the device's zone on purpose: the fair opens at
 * 16:00 local to the campus whatever a visiting student's phone is set to.
 *
 * `java.util.Calendar` rather than `java.time`, which needs API 26 or core
 * library desugaring; this app's floor is 24 and a two-field date range is not
 * worth either.
 */
object FairSchedule {

    private val Campus: TimeZone = TimeZone.getTimeZone("Asia/Bangkok")

    /**
     * The window as this build understands it, before the server has said.
     *
     * Not the source of truth any more — `GET /clubfair/info` is, and
     * [applyServer] overwrites both of these the moment it lands. This is what
     * the welcome screen prints on a first run with no signal, which is why it
     * is kept correct rather than left at whatever was true when it was typed.
     */
    private val DefaultStart = campusTime(2026, Calendar.AUGUST, 22, hour = 16, minute = 0)
    private val DefaultEnd = campusTime(2026, Calendar.AUGUST, 22, hour = 21, minute = 30)

    /**
     * Compose state, not plain fields.
     *
     * Every screen that shows a date reads these directly — the countdown, the
     * welcome block, the pass — and a plain `var` would leave all three showing
     * the built-in window until something else happened to recompose them. As
     * state, the server's answer lands on screen the moment it arrives without a
     * single call site having to be told about it.
     */
    var startMillis by mutableLongStateOf(DefaultStart)
        private set
    var endMillis by mutableLongStateOf(DefaultEnd)
        private set

    /** The hall, Thai then English. Null until the server has been heard from. */
    var venue by mutableStateOf<String?>(null)
        private set
    var venueEn by mutableStateOf<String?>(null)
        private set

    /**
     * Take the server's word for it.
     *
     * Guarded on a sane window rather than trusted outright: a row with its end
     * before its start would make the countdown negative and the status
     * permanently "ended", and the app has a correct window of its own to keep
     * showing instead. su-server has a check constraint saying the same thing,
     * so this only catches a route that changes shape under us.
     */
    fun applyServer(startMillis: Long, endMillis: Long, venue: String?, venueEn: String?) {
        if (endMillis <= startMillis) return
        this.startMillis = startMillis
        this.endMillis = endMillis
        this.venue = venue?.takeIf { it.isNotBlank() }
        this.venueEn = venueEn?.takeIf { it.isNotBlank() }
    }

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
