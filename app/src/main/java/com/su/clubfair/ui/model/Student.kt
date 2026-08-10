package com.su.clubfair.ui.model

import com.su.clubfair.data.CachedUser
import com.su.clubfair.data.net.UserDto

/**
 * The signed-in student, as far as the UI is concerned.
 *
 * One holder rather than per-screen constants, because the same person is
 * rendered on Home, the pass and the profile — three copies of a name would drift
 * the moment one of them changed.
 *
 * The counters are no longer part of this. `visited`, `total`, `rank` and the
 * prize tiers all come from `GET /clubfair/progress` as [FairProgress], because
 * they change when a booth is scanned while the identity does not — and folding
 * them together meant every scan produced a "new student" and rebuilt the shell.
 */
data class Student(
    val id: Int,
    val firstName: String,
    val surname: String,
    val email: String,
    val studentId: String?,
    val phone: String?,
    val school: String?,
    val major: String?,
    val avatarUrl: String?,
    /**
     * From the server's own token claim, not a local flag. Decides whether the
     * Events tab shows a composer — and the server enforces it again on the
     * route, so a tampered client gains nothing.
     */
    val isStaff: Boolean,
    /** Whether a password is set, so a Google-only account can be offered one. */
    val hasPassword: Boolean,
) {
    /** Both names, for anywhere that just wants to print the person. */
    val name: String get() = "$firstName $surname"

    /** Avatar monogram — one letter from each name, so two students rarely collide. */
    val initials: String
        get() = "${firstName.take(1)}${surname.take(1)}".uppercase()

    companion object {
        private fun isStaffRole(role: String) = role == "staff" || role == "admin"

        fun from(dto: UserDto) = Student(
            id = dto.id,
            firstName = dto.firstName,
            surname = dto.surname,
            email = dto.email,
            studentId = dto.studentId,
            phone = dto.phone,
            school = dto.school,
            major = dto.major,
            avatarUrl = dto.avatarUrl,
            isStaff = isStaffRole(dto.role),
            hasPassword = dto.hasPassword,
        )

        fun from(cached: CachedUser) = Student(
            id = cached.id,
            firstName = cached.firstName,
            surname = cached.surname,
            email = cached.email,
            studentId = cached.studentId,
            phone = cached.phone,
            school = cached.school,
            major = cached.major,
            avatarUrl = cached.avatarUrl,
            isStaff = isStaffRole(cached.role),
            hasPassword = cached.hasPassword,
        )
    }
}

fun Student.toCachedUser(role: String) = CachedUser(
    id = id,
    role = role,
    firstName = firstName,
    surname = surname,
    email = email,
    phone = phone,
    studentId = studentId,
    school = school,
    major = major,
    avatarUrl = avatarUrl,
    hasPassword = hasPassword,
)

/**
 * How far round the fair this student has got.
 *
 * Separate from [Student] because it changes on every scan while the identity does
 * not. [rank] is nullable and stays that way — the server returns null for a
 * student who has scanned nothing, and the UI shows an em dash rather than
 * inventing a position.
 */
data class FairProgress(
    val visited: Int = 0,
    val total: Int = 0,
    /** Booth **ids**, so the checkpoint grid lights the right cells. */
    val visitedBoothIds: Set<Int> = emptySet(),
    val rank: Int? = null,
    val prizes: List<PrizeTier> = emptyList(),
) {
    val progress: Float get() = if (total > 0) visited.toFloat() / total else 0f

    /** Tiers actually earned — what Home's "Prizes" tile counts. */
    val prizesEarned: Int get() = prizes.count { it.reached }
}

/**
 * A prize threshold, and where this student stands against it.
 *
 * The thresholds live on the server (`clubfair_prize_tier`) rather than in the
 * app, so the Student Union can move one mid-fair without a Play Store release.
 */
data class PrizeTier(
    val id: Int,
    val threshold: Int,
    val name: String,
    val description: String?,
    val reached: Boolean,
    val claimed: Boolean,
)

/** A stand-in student for `@Preview`. Nothing in the running app reaches for it. */
val PreviewStudent = Student(
    id = 1,
    firstName = "Yion",
    surname = "Suriya",
    email = "6831503029@lamduan.mfu.ac.th",
    studentId = "6831503029",
    phone = "0683150329",
    school = "Applied Digital Technology",
    major = "Software Engineering",
    avatarUrl = null,
    isStaff = false,
    hasPassword = true,
)

val PreviewProgress = FairProgress(
    visited = 7,
    total = PreviewBoothCount,
    visitedBoothIds = setOf(1, 2, 5, 7, 11, 17, 24),
    rank = 42,
    prizes = listOf(
        PrizeTier(1, 10, "Halfway", "Ten booths visited", reached = false, claimed = false),
        PrizeTier(2, 20, "Prize draw", null, reached = false, claimed = false),
        PrizeTier(3, 28, "Full sweep", null, reached = false, claimed = false),
    ),
)
