package com.su.clubfair.data

import com.su.clubfair.ui.model.Announcement
import com.su.clubfair.ui.model.Booth
import com.su.clubfair.ui.model.BoothCount
import com.su.clubfair.ui.model.Reaction
import com.su.clubfair.ui.model.SeedAnnouncements
import com.su.clubfair.ui.model.Student
import com.su.clubfair.ui.model.boothRoster
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** What came back from a sign-in or sign-up attempt. */
sealed interface AuthResult {
    data object Success : AuthResult

    /** No account has been registered on this device. */
    data object NoAccount : AuthResult

    /** There is an account, but for a different phone number. */
    data object UnknownPhone : AuthResult

    data object WrongPassword : AuthResult
}

/** What a scanned payload turned out to be. */
sealed interface ScanOutcome {
    data class Recorded(val booth: Booth) : ScanOutcome

    /** A real booth, already ticked. Worth saying out loud rather than no-oping. */
    data class AlreadyScanned(val booth: Booth) : ScanOutcome

    data class NotABoothCode(val payload: String) : ScanOutcome
}

/**
 * The one thing the UI reads the fair through.
 *
 * Everything above this line is Compose; everything below is storage. That
 * boundary is the point of the class — screens used to reach straight into
 * `PlaceholderStudent` and `boothRoster(19)`, which is why nothing in the app
 * could change and nothing could be tested.
 *
 * ## Where the server goes
 *
 * Three things here are answered locally that a server will answer instead, and
 * each is marked at its own definition rather than only here:
 *
 *  - [signIn] checks the password against material on this device. A phone
 *    cannot authenticate anyone; see [PasswordHasher].
 *  - [recordScan] writes a checkpoint the student's own device asserts. A phone
 *    cannot certify that anyone stood in front of a booth; see [BoothCode].
 *  - [announcements] is a fixed seed list. There is no channel to read.
 *
 * [unsyncedScans] is the seam for the first of those to become real: it is every
 * scan this device holds, oldest first, which is exactly the backlog to POST
 * once there is somewhere to POST it.
 *
 * [clock] is injected so tests can hold time still.
 */
class FairRepository(
    private val store: ClubFairStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /** The signed-in student with their live counters, or null when signed out. */
    val student: Flow<Student?> =
        combine(store.session, store.scans) { account, scans ->
            account?.let {
                Student(
                    firstName = it.firstName,
                    surname = it.surname,
                    email = it.email,
                    studentId = it.studentId,
                    phone = it.phone,
                    school = it.school,
                    major = it.major,
                    visited = scans.size,
                    total = BoothCount,
                    // Not derivable on a phone — see Student.rank.
                    rank = null,
                    isAdmin = it.isAdmin,
                )
            }
        }

    /** Whether an account exists at all, which decides Welcome's call to action. */
    val hasAccount: Flow<Boolean> = store.account.map { it != null }

    /** The full roster, ticked against what this device has scanned. */
    val booths: Flow<List<Booth>> = store.scans.map { scans ->
        boothRoster(scans.mapTo(mutableSetOf()) { it.booth })
    }

    /**
     * The channel, with the student's own reactions merged back in.
     *
     * The seed's counts stand for everyone else; the stored set decides which
     * chips are lit and bumps those counts by one, so a reaction a student left
     * yesterday is still theirs when they come back.
     */
    val announcements: Flow<List<Announcement>> = store.reactions.map { mine ->
        SeedAnnouncements.map { post -> post.withMine(mine) }
    }

    /**
     * Scans this device is holding that no server has acknowledged.
     *
     * Today that is all of them, because there is no server. It is exposed
     * anyway, and Settings shows the count, because the alternative is a student
     * assuming their afternoon is safely recorded somewhere when it is on one
     * phone that could go in a fountain.
     */
    val unsyncedScans: Flow<List<ScanRecord>> = store.scans

    val hapticsEnabled: Flow<Boolean> = store.hapticsEnabled

    val onboardingSeen: Flow<Boolean> = store.onboardingSeen

    suspend fun markOnboardingSeen() = store.setOnboardingSeen()

    // ---- Auth -------------------------------------------------------------

    /**
     * Signs in against the account registered on this device.
     *
     * See the class note: this proves the phone was told the right password, not
     * that the person holding it is who they say they are.
     */
    suspend fun signIn(phone: String, password: String): AuthResult {
        val account = store.account.first() ?: return AuthResult.NoAccount
        if (!PhoneNumber.matches(account.phone, phone)) return AuthResult.UnknownPhone
        val ok = PasswordHasher.verify(password, account.passwordSalt, account.passwordHash)
        if (!ok) return AuthResult.WrongPassword
        store.setSignedIn(true)
        return AuthResult.Success
    }

    /** Registers the account this device holds, replacing any previous one. */
    suspend fun signUp(
        firstName: String,
        surname: String,
        studentId: String,
        phone: String,
        email: String,
        school: String,
        major: String,
        password: String,
    ): AuthResult {
        val salt = PasswordHasher.newSalt()
        store.saveAccount(
            StoredAccount(
                studentId = studentId.trim(),
                firstName = firstName.trim(),
                surname = surname.trim(),
                email = email.trim(),
                phone = PhoneNumber.normalise(phone),
                school = school.trim(),
                major = major.trim(),
                isAdmin = false,
                passwordSalt = salt,
                passwordHash = PasswordHasher.hash(password, salt),
            ),
        )
        return AuthResult.Success
    }

    /** Ends the session. The account and its scans stay, so signing back in works. */
    suspend fun signOut() = store.setSignedIn(false)

    /** Settings' "erase everything on this device" — account, scans and all. */
    suspend fun eraseDevice() = store.clearAll()

    // ---- Scanning ---------------------------------------------------------

    /**
     * Reads [payload] and records it if it names a booth.
     *
     * Returns what happened rather than a boolean, because the three outcomes
     * need three different things said to someone standing at a booth holding a
     * phone up: it worked, you already did this one, or that is not a fair code.
     */
    suspend fun recordScan(payload: String): ScanOutcome {
        val number = BoothCode.parse(payload, BoothCount)
            ?: return ScanOutcome.NotABoothCode(payload)

        val added = store.recordScan(number, clock())
        // Read the roster back rather than rebuilding it, so the returned booth
        // carries the tick that was just written.
        val booth = booths.first().first { it.number == number }
        return if (added) ScanOutcome.Recorded(booth) else ScanOutcome.AlreadyScanned(booth)
    }

    // ---- Channel ----------------------------------------------------------

    suspend fun toggleReaction(postId: Int, emoji: String) =
        store.toggleReaction(reactionKey(postId, emoji))

    suspend fun setHapticsEnabled(enabled: Boolean) = store.setHapticsEnabled(enabled)
}

/**
 * Lights this post's chips from [mine] and adds the student to those counts.
 *
 * An emoji the student picked that nobody else has used yet has no chip in the
 * seed, so it is appended at count 1 — the same shape [ScanOutcome] has, where
 * the stored fact wins over the placeholder.
 */
private fun Announcement.withMine(mine: Set<String>): Announcement {
    val chosen = mine.mapNotNullTo(mutableSetOf()) { key ->
        key.substringBefore(':').toIntOrNull()?.takeIf { it == id }?.let { key.substringAfter(':') }
    }
    if (chosen.isEmpty()) return this

    val existing = reactions.map { reaction ->
        if (reaction.emoji in chosen) {
            reaction.copy(count = reaction.count + 1, mine = true)
        } else {
            reaction
        }
    }
    val fresh = chosen
        .filterNot { emoji -> reactions.any { it.emoji == emoji } }
        .map { Reaction(it, count = 1, mine = true) }

    return copy(reactions = existing + fresh)
}
