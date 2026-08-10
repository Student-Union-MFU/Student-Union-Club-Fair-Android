package com.su.clubfair.ui.model

import com.su.clubfair.data.FairRules

/**
 * The signed-in student, as far as the UI is concerned.
 *
 * One holder rather than per-screen constants, because the same person is
 * rendered on Home, the pass and the profile — and three copies of a name would
 * drift the moment one of them changed.
 *
 * The name is stored as two fields because sign-up asks for two. It used to be
 * one `name` string, which meant nothing could tell a given name from a family
 * name — no "Hi, Yion" without guessing at a space, and no initials that use
 * both. [name] rebuilds the display form so callers that only want the whole
 * thing are unaffected.
 *
 * [visited] and [prizes] are now real: the first counts what this device has
 * actually scanned, the second falls out of it by [FairRules]. [rank] is the one
 * field a phone cannot compute — see its own note.
 */
data class Student(
    val firstName: String,
    val surname: String,
    val email: String,
    val studentId: String,
    val phone: String,
    val school: String,
    val major: String,
    /** Booths this device has recorded a scan for. */
    val visited: Int,
    /** Booths at the fair. */
    val total: Int,
    /**
     * Standing against every other student — or null when that isn't known.
     *
     * Null is the honest value today and will be until something ranks students
     * centrally. It was a hardcoded `42`, which Home printed as `#42` beside two
     * numbers that were real; one invented figure in a row of measurements makes
     * the other two unreadable, because a student has no way to tell which is
     * which. Everything that renders this has to handle null rather than
     * defaulting it to a number.
     */
    val rank: Int? = null,
    /**
     * Whether this account may post to the announcements channel.
     *
     * There is no role system behind this — no backend, no claim on a token,
     * nothing to check. It is a stored flag standing in for one, and it decides
     * whether the Events tab shows a composer or the read-only footer that every
     * student sees.
     */
    val isAdmin: Boolean = false,
) {
    /** Both names, for anywhere that just wants to print the person. */
    val name: String get() = "$firstName $surname"

    /** Avatar monogram — one letter from each name, so two students rarely collide. */
    val initials: String
        get() = "${firstName.take(1)}${surname.take(1)}".uppercase()

    /** Prize tiers earned, derived from [visited] rather than stored. */
    val prizes: Int get() = FairRules.prizesFor(visited)

    /** 0f..1f across the whole fair, for the progress track and the percentage. */
    val progress: Float get() = if (total > 0) visited.toFloat() / total else 0f
}

/**
 * A stand-in student for `@Preview` and for screenshot tests.
 *
 * Preview-only, and named so it cannot be mistaken for a fallback: nothing in
 * the running app reaches for this any more. The signed-in student comes from
 * `FairRepository`, and a screen with no session shows its signed-out state
 * rather than quietly rendering someone made up.
 */
val PreviewStudent = Student(
    firstName = "Yion",
    surname = "Suriya",
    email = "yion.sur@lamduan.mfu.ac.th",
    studentId = "6831503029",
    phone = "068 315 0329",
    school = "Applied Digital Technology",
    major = "Software Engineering",
    visited = 19,
    total = 27,
    rank = null,
)
