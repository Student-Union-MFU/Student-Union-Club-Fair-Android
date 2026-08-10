package com.su.clubfair.ui.model

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
 * Nothing populates this from a backend yet; [PlaceholderStudent] stands in.
 */
data class Student(
    val firstName: String,
    val surname: String,
    val email: String,
    val studentId: String,
    val phone: String,
    val school: String,
    val major: String,
    val visited: Int,
    val total: Int,
    val rank: Int,
    val prizes: Int,
    /**
     * Whether this account may post to the announcements channel.
     *
     * There is no role system behind this — no backend, no claim on a token,
     * nothing to check. It is a local flag standing in for one, and it decides
     * whether the Events tab shows a composer or the read-only footer that every
     * student sees. Flip it to preview the Student Union's side of that screen.
     */
    val isAdmin: Boolean = false,
) {
    /** Both names, for anywhere that just wants to print the person. */
    val name: String get() = "$firstName $surname"

    /** Avatar monogram — one letter from each name, so two students rarely collide. */
    val initials: String
        get() = "${firstName.take(1)}${surname.take(1)}".uppercase()
}

/** Stand-in until sign-in returns a real profile. */
val PlaceholderStudent = Student(
    firstName = "Yion",
    surname = "Suriya",
    email = "yion.sur@lamduan.mfu.ac.th",
    studentId = "6831503029",
    phone = "068 315 0329",
    school = "Applied Digital Technology",
    major = "Software Engineering",
    visited = 19,
    total = 27,
    rank = 42,
    prizes = 3,
)
