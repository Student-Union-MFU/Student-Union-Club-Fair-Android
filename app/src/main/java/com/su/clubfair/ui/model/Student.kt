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
/**
 * What kind of account this is, from su-server's role string.
 *
 * One mapping, in one place, because the answer now decides more than a label:
 * whether Home offers checkpoints, whether the scanner turns its camera on, and
 * — once su-server can say which booth an account belongs to — whether Home
 * shows a booth's own code instead.
 *
 * [Unknown] is not a failure state. su-server can gain a role tomorrow, and the
 * app has to do something sensible with a value it has never seen. It is treated
 * as a participant everywhere a decision has to be made: the server enforces
 * every one of these rules again on the route, so the worst case is a student
 * offered a screen that answers 403, where the other default locks a real
 * participant out of the fair over a string the app did not recognise.
 */
enum class AccountRole { Participant, Staff, BoothOwner, Admin, Unknown }

/**
 * su-server's four spellings, lowercased.
 *
 * The list is not a guess: `clubfair_users.role` carries a check constraint
 * allowing exactly `student`, `staff`, `admin` and `booth_owner`, so a fifth
 * value cannot arrive without a migration — and [AccountRole.Unknown] is what
 * happens on the day one does.
 */
fun accountRoleOf(role: String): AccountRole = when (role.lowercase().trim()) {
    "student" -> AccountRole.Participant
    "staff" -> AccountRole.Staff
    "admin" -> AccountRole.Admin
    "booth_owner" -> AccountRole.BoothOwner
    else -> AccountRole.Unknown
}

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
    /**
     * The server's own role string — `student`, `staff`, `admin`, and whatever
     * su-server calls a booth account.
     *
     * Kept beside [isStaff] rather than replacing it. [isStaff] answers "may this
     * account do staff things", which is a question with two answers and is what
     * the composer and the claim gate gate on; this answers "what is this
     * account", which the profile prints and which will keep gaining values as
     * su-server does. Collapsing them would mean every new role had to be
     * classified before it could be named.
     */
    val role: String,
    /** Whether a password is set, so a Google-only account can be offered one. */
    val hasPassword: Boolean,
    /**
     * Whether sign-up finished. False for an account Google has just created,
     * which the gate in `MainActivity` sends to the sign-up form rather than
     * into the fair.
     */
    val profileComplete: Boolean = true,
) {
    /** What this account is — see [AccountRole]. */
    val account: AccountRole get() = accountRoleOf(role)

    /**
     * Whether the fair's game is this account's to play.
     *
     * Staff, admins and booth accounts are *running* the fair rather than
     * walking it: a booth owner standing at their own booth all day has no
     * checkpoints to collect and no reason to be offered a card counting them.
     * This is presentation, not security — su-server decides what a token may
     * actually do, and this only decides what is worth putting on screen.
     */
    val collectsCheckpoints: Boolean
        get() = account == AccountRole.Participant || account == AccountRole.Unknown

    /** The scanner is the checkpoint game's instrument, so it follows it exactly. */
    val canScan: Boolean get() = collectsCheckpoints

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
            role = dto.role,
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
            role = cached.role,
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

    /**
     * The MFU333 tier — the lowest threshold the fair offers.
     *
     * Picked by threshold rather than by list position or by name: `sort_order`
     * is the server's business and the name is the Student Union's to rewrite,
     * but "the first thing a student can reach" is neither. It survives both.
     */
    val mfu333: PrizeTier? get() = prizes.minByOrNull { it.threshold }

    /**
     * Whether the MFU333 code is unlocked.
     *
     * `reached` is the server's answer, not arithmetic done here — the threshold
     * can move mid-fair and this must move with it. No tiers at all reads as
     * locked: that is the state before the first `GET /clubfair/progress` lands,
     * and showing a claim code to someone who has not earned one is the worse of
     * the two ways to be wrong for a moment.
     */
    val mfu333Unlocked: Boolean get() = mfu333?.reached == true

    /** Booths still to walk before it unlocks; zero once it has. */
    val boothsToMfu333: Int
        get() = mfu333?.let { (it.threshold - visited).coerceAtLeast(0) } ?: 0
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
    role = "student",
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
