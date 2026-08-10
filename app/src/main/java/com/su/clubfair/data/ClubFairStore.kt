package com.su.clubfair.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Everything this app keeps on the device, behind one DataStore.
 *
 * Before this, the app kept nothing: the signed-in student, the booths they had
 * scanned and the reactions they had left all lived in `remember` and died with
 * the process. A student who closed the app half-way round the fair came back to
 * an empty card and a login screen.
 *
 * DataStore rather than SharedPreferences because every read here is a [Flow]
 * the UI collects — the checkpoint grid on Home and the tick on a booth tile are
 * two views of the same set and have to move together. It is also the one
 * storage API on Android whose writes are transactional; `SharedPreferences`
 * `apply()` is fire-and-forget and drops the last write if the process dies,
 * which for a scan a student just took at a booth is the write you can least
 * afford to lose.
 *
 * ## The account / session split
 *
 * This device holds **one account**. [account] is the registered student and
 * their password material, and it outlives sign-out so they can sign back in.
 * [session] is whether that account is currently signed in. Registering a
 * different student replaces the account and takes the previous one's scans with
 * it — see [saveAccount] — because there is no server to keep two students'
 * progress apart on one phone.
 *
 * None of this is a substitute for a server. A reinstall loses everything, and
 * nothing here stops a determined device owner editing their own progress.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "clubfair",
)

/**
 * The registered student on this device.
 *
 * [passwordSalt] and [passwordHash] are PBKDF2 material, never the password —
 * see [PasswordHasher].
 */
data class StoredAccount(
    val studentId: String,
    val firstName: String,
    val surname: String,
    val email: String,
    val phone: String,
    val school: String,
    val major: String,
    val isAdmin: Boolean,
    val passwordSalt: String,
    val passwordHash: String,
)

/**
 * One booth, scanned at a moment.
 *
 * The timestamp is not decoration. It is what makes this a queue as well as a
 * record: when there is a server, everything it has not acknowledged is the
 * backlog to send, in the order the student actually walked the floor.
 */
data class ScanRecord(
    val booth: Int,
    val atMillis: Long,
)

class ClubFairStore(context: Context) {

    private val store = context.applicationContext.dataStore

    /**
     * A read that survives a corrupt or unreadable file.
     *
     * DataStore throws [IOException] into the flow rather than returning a
     * default, and an uncaught one there takes down every collector — which in
     * this app means the whole signed-in shell. Empty preferences is the right
     * answer to "the file will not load": the student is signed out and starts
     * again, which is recoverable, rather than the app crashing on launch
     * forever.
     */
    private val preferences: Flow<Preferences> = store.data.catch { cause ->
        if (cause is IOException) emit(emptyPreferences()) else throw cause
    }

    // ---- Account and session ---------------------------------------------

    val account: Flow<StoredAccount?> = preferences.map { it.toAccount() }

    /** The account, but only while it is signed in. */
    val session: Flow<StoredAccount?> = preferences.map { prefs ->
        prefs.toAccount()?.takeIf { prefs[Keys.SignedIn] == true }
    }

    private fun Preferences.toAccount(): StoredAccount? {
        // The id is the key the rest of the record hangs off; without it there is
        // no account, whatever else happens to be in the file.
        val id = this[Keys.StudentId] ?: return null
        return StoredAccount(
            studentId = id,
            firstName = this[Keys.FirstName].orEmpty(),
            surname = this[Keys.Surname].orEmpty(),
            email = this[Keys.Email].orEmpty(),
            phone = this[Keys.Phone].orEmpty(),
            school = this[Keys.School].orEmpty(),
            major = this[Keys.Major].orEmpty(),
            isAdmin = this[Keys.IsAdmin] == true,
            passwordSalt = this[Keys.PasswordSalt].orEmpty(),
            passwordHash = this[Keys.PasswordHash].orEmpty(),
        )
    }

    /**
     * Registers [account] and signs it in.
     *
     * Clears the scans and reactions on the way through, because this is the
     * only path by which the phone changes hands to a different student.
     * Inheriting the last person's 19 checkpoints would be both wrong and, with
     * a prize draw at the end, worth cheating for.
     */
    suspend fun saveAccount(account: StoredAccount) {
        store.edit { prefs ->
            prefs.remove(Keys.Scans)
            prefs.remove(Keys.Reactions)
            prefs[Keys.StudentId] = account.studentId
            prefs[Keys.FirstName] = account.firstName
            prefs[Keys.Surname] = account.surname
            prefs[Keys.Email] = account.email
            prefs[Keys.Phone] = account.phone
            prefs[Keys.School] = account.school
            prefs[Keys.Major] = account.major
            prefs[Keys.IsAdmin] = account.isAdmin
            prefs[Keys.PasswordSalt] = account.passwordSalt
            prefs[Keys.PasswordHash] = account.passwordHash
            prefs[Keys.SignedIn] = true
        }
    }

    suspend fun setSignedIn(signedIn: Boolean) {
        store.edit { it[Keys.SignedIn] = signedIn }
    }

    /** Wipes the account, its progress and its preferences. Used by Settings. */
    suspend fun clearAll() {
        store.edit { it.clear() }
    }

    // ---- Scans ------------------------------------------------------------

    val scans: Flow<List<ScanRecord>> = preferences.map { prefs ->
        prefs[Keys.Scans].orEmpty()
            .mapNotNull(::decodeScan)
            .sortedBy { it.atMillis }
    }

    /**
     * Records a scan, and reports whether it was new.
     *
     * `false` means this booth was already scanned. The caller wants that
     * distinction: walking back past a booth you have done should say so, not
     * silently re-tick a box and look like nothing happened.
     *
     * Read-modify-write inside a single [DataStore.edit], which DataStore runs
     * under its own lock — two scans landing together cannot lose one another.
     */
    suspend fun recordScan(booth: Int, atMillis: Long): Boolean {
        var added = false
        store.edit { prefs ->
            val existing = prefs[Keys.Scans].orEmpty()
            val already = existing.any { decodeScan(it)?.booth == booth }
            if (!already) {
                prefs[Keys.Scans] = existing + encodeScan(ScanRecord(booth, atMillis))
                added = true
            }
        }
        return added
    }

    // ---- Reactions --------------------------------------------------------

    /**
     * Which announcements the student has reacted to, and with what.
     *
     * Only *their own* reactions. The counts belong to everyone and will come
     * from the server; what has to survive a restart locally is the one bit the
     * student can see about themselves — whether their chip is lit.
     */
    val reactions: Flow<Set<String>> = preferences.map { it[Keys.Reactions].orEmpty() }

    suspend fun toggleReaction(key: String) {
        store.edit { prefs ->
            val current = prefs[Keys.Reactions].orEmpty()
            prefs[Keys.Reactions] = if (key in current) current - key else current + key
        }
    }

    // ---- Preferences ------------------------------------------------------

    /** Defaults on: the tick at a booth is the app's only non-visual feedback. */
    val hapticsEnabled: Flow<Boolean> = preferences.map { it[Keys.Haptics] != false }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        store.edit { it[Keys.Haptics] = enabled }
    }

    /**
     * Whether the "how the fair works" card has been shown.
     *
     * Stored rather than sequenced into the sign-up flow, because it is a
     * property of the person and not of the login: a student who signs out and
     * back in during the fair does not need telling twice, and one who reinstalls
     * does.
     */
    val onboardingSeen: Flow<Boolean> = preferences.map { it[Keys.OnboardingSeen] == true }

    suspend fun setOnboardingSeen() {
        store.edit { it[Keys.OnboardingSeen] = true }
    }

    private object Keys {
        val SignedIn = booleanPreferencesKey("signed_in")
        val StudentId = stringPreferencesKey("student_id")
        val FirstName = stringPreferencesKey("first_name")
        val Surname = stringPreferencesKey("surname")
        val Email = stringPreferencesKey("email")
        val Phone = stringPreferencesKey("phone")
        val School = stringPreferencesKey("school")
        val Major = stringPreferencesKey("major")
        val IsAdmin = booleanPreferencesKey("is_admin")
        val PasswordSalt = stringPreferencesKey("password_salt")
        val PasswordHash = stringPreferencesKey("password_hash")
        val Scans = stringSetPreferencesKey("scans")
        val Reactions = stringSetPreferencesKey("reactions")
        val Haptics = booleanPreferencesKey("haptics")
        val OnboardingSeen = booleanPreferencesKey("onboarding_seen")
    }
}

/**
 * `"<booth>@<millis>"`.
 *
 * A string set rather than two parallel keys, because a set is the one
 * Preferences type with no ordering to keep in sync and no chance of the two
 * halves of a record drifting apart. [decodeScan] is total — a malformed entry
 * is dropped rather than thrown, so one bad row cannot make the card unreadable.
 */
internal fun encodeScan(record: ScanRecord): String = "${record.booth}@${record.atMillis}"

internal fun decodeScan(raw: String): ScanRecord? {
    val at = raw.indexOf('@')
    if (at <= 0) return null
    val booth = raw.substring(0, at).toIntOrNull() ?: return null
    val millis = raw.substring(at + 1).toLongOrNull() ?: return null
    return ScanRecord(booth, millis)
}

/** `"<postId>:<emoji>"` — the key a student's own reaction is stored under. */
internal fun reactionKey(postId: Int, emoji: String): String = "$postId:$emoji"
