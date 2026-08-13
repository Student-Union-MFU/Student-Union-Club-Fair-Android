package com.su.clubfair.ui.model

import com.su.clubfair.data.CachedUser
import com.su.clubfair.data.MfuEmail
import com.su.clubfair.data.net.GoogleAccount
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
    /**
     * Whether sign-up finished. False for an account Google has just created,
     * which the gate in `MainActivity` sends to the sign-up form rather than
     * into the fair.
     */
    val profileComplete: Boolean = true,
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
            profileComplete = dto.profileComplete,
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
            profileComplete = cached.profileComplete,
        )
    }
}

/**
 * Fills the gaps in a server account from what Google said about the same person.
 *
 * **The server stays authoritative.** Every field below is written only where the
 * server left a null or a blank, so nothing a student has actually saved can be
 * overwritten by a credential bundle — a student who fixes their surname in
 * su-server's admin keeps the fix, and does not have it reverted at the next
 * sign-in by whatever Google has on file. The direction matters more than the
 * fields do: this is a fallback, not a sync.
 *
 * This is a **fallback layer, not the main supply**, and it is worth being clear
 * about that because most of these fields normally arrive filled:
 *
 *  - **The phone** is the one that genuinely earns its place. The server has no
 *    phone for a Google account until the student types one, and Google
 *    sometimes has a verified number — so this pre-fills the sign-up field. It
 *    is still validated and still editable.
 *  - **The photo** is normally the server's: `clubfair_auth_service.go` reads
 *    the token's `picture` claim and stores it as `avatar_url`, refreshing it on
 *    every sign-in. This covers only the case where that column is empty. Note
 *    that displaying it is a separate matter — the URL was stored and never
 *    drawn until `StudentAvatar` existed.
 *  - **The student id** the server also derives, from the same email local part.
 *    Kept for the same reason: cheap, and the pass needs something to encode.
 *  - **The name** the server always has. Reached only if it came back blank, so
 *    that an empty greeting is not the result.
 *
 * A [google] belonging to a different address is ignored outright — see
 * [GoogleAccount.isFor] for the shared-phone case that motivates it.
 */
fun Student.filledFrom(google: GoogleAccount?): Student {
    if (google == null || !google.isFor(email)) return this
    return copy(
        firstName = firstName.ifBlank { google.firstName ?: firstName },
        surname = surname.ifBlank { google.surname ?: surname },
        avatarUrl = avatarUrl.orNull() ?: google.photoUrl.orNull(),
        studentId = studentId.orNull() ?: MfuEmail.studentIdFrom(google.email),
        phone = phone.orNull() ?: google.phone.orNull(),
    )
}

/**
 * Blank and null are the same thing, on both sides.
 *
 * Applied to Google's values as well as the server's, which is not symmetry for
 * its own sake: a credential can carry `phone = ""`, and letting that through
 * would pre-fill the sign-up form with an empty string that then fails the
 * form's own validation — a field the student never touched, reported as wrong.
 */
private fun String?.orNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

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
    profileComplete = profileComplete,
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

/**
 * A stand-in for `@Preview` and for the screen tests.
 *
 * The two tiers are the fair's real shape — fifteen booths for the first prize
 * and all twenty-eight for the second — rather than the three seeded rows this
 * used to carry. The names are flat on purpose and match the rows on su-server:
 * a stop on the route is "Prize 1", not a phrase describing how it feels to
 * reach it. They are still only a stand-in: the running app renders whatever
 * `clubfair_prize_tier` holds, so moving a threshold is a row on su-server and
 * not a release. What matching reality buys is that the prize route is designed
 * and tested against the number of stops it will actually have; three evenly
 * spaced tiers hid the fact that the second half of this fair's route is one long
 * unbroken run to the end.
 */
val PreviewProgress = FairProgress(
    visited = 7,
    total = PreviewBoothCount,
    visitedBoothIds = setOf(1, 2, 5, 7, 11, 17, 24),
    rank = 42,
    prizes = listOf(
        PrizeTier(1, 15, "Prize 1", "15 booths visited", reached = false, claimed = false),
        PrizeTier(2, 28, "Prize 2", "28 booths visited", reached = false, claimed = false),
    ),
)
