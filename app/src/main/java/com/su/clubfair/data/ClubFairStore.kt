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
import com.su.clubfair.data.net.GoogleAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Everything the app keeps on the device, behind one DataStore.
 *
 * Its job changed when su-server arrived, and the change is worth stating because
 * the file looks similar and means something different. It used to be the **source
 * of truth**: the account, a PBKDF2 password hash, and the scans that existed
 * nowhere else. It is now three narrower things:
 *
 *  1. **The session token.** What proves who the student is on every request.
 *  2. **An offline cache.** The booths, the last known progress and the channel,
 *     so a phone with no signal in a concrete hall shows the fair rather than a
 *     spinner. Always stale, never authoritative.
 *  3. **An outbound queue.** Scans taken while offline, waiting to be sent.
 *
 * The password hashing is gone. Verifying a password against material on the
 * device only ever proved the phone had been told the right answer; the server
 * does it properly now, and keeping the old code would have invited someone to
 * use it.
 *
 * ⚠ Nothing here is a security boundary. The token is what an attacker with the
 * file would want, and DataStore does not encrypt it — see [authToken].
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "clubfair",
)

/**
 * One scan waiting to be sent.
 *
 * The whole payload is kept, not the booth number: the server parses the QR and
 * verifies its HMAC, so the client has nothing to gain by interpreting it and
 * would only be able to get it wrong. [clientId] is minted when the code is
 * scanned and never changes, which is what makes a resend idempotent.
 */
@Serializable
data class PendingScan(
    val payload: String,
    val clientId: String,
    val deviceTime: String,
)

/** The signed-in student, cached so Profile renders offline. */
@Serializable
data class CachedUser(
    val id: Int,
    val role: String,
    val firstName: String,
    val surname: String,
    val email: String,
    val phone: String? = null,
    val studentId: String? = null,
    val school: String? = null,
    val major: String? = null,
    val avatarUrl: String? = null,
    val hasPassword: Boolean = false,
    /**
     * Cached so the sign-up gate survives a cold start. Defaults to true for the
     * same reason the DTO field does: a cache written by an older build belongs
     * to an account that finished signing up before the gate existed.
     */
    val profileComplete: Boolean = true,
)

class ClubFairStore(context: Context) {

    private val store = context.applicationContext.dataStore

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * A read that survives a corrupt or unreadable file.
     *
     * DataStore throws [IOException] into the flow rather than returning a
     * default, and an uncaught one there takes down every collector — which in
     * this app means the whole signed-in shell. Empty preferences is the right
     * answer to "the file will not load": the student signs in again, which is
     * recoverable, rather than the app crashing on launch forever.
     */
    private val preferences: Flow<Preferences> = store.data.catch { cause ->
        if (cause is IOException) emit(emptyPreferences()) else throw cause
    }

    // ---- Session ----------------------------------------------------------

    /**
     * The bearer token, or null when signed out.
     *
     * ⚠ Stored in plain DataStore, which is app-private but not encrypted: root,
     * a device backup or an unlocked debug build can read it. It is excluded from
     * cloud backup (see `data_extraction_rules.xml`) and expires in 30 days, which
     * bounds the damage without pretending it is protected. `EncryptedSharedPrefs`
     * is deprecated and its replacement is not settled; the honest position is
     * that a student's booth count is not worth a keystore-backed cipher, and this
     * comment is here so nobody later assumes it is protected.
     */
    val authToken: Flow<String?> = preferences.map { it[Keys.AuthToken] }

    /** Read once, for an outbound request. */
    suspend fun currentToken(): String? = preferences.first()[Keys.AuthToken]

    val cachedUser: Flow<CachedUser?> = preferences.map { prefs ->
        prefs[Keys.CachedUser]?.let { raw ->
            runCatching { json.decodeFromString<CachedUser>(raw) }.getOrNull()
        }
    }

    /** Stores the token and the profile it belongs to, together. */
    suspend fun saveSession(token: String, user: CachedUser) {
        store.edit { prefs ->
            prefs[Keys.AuthToken] = token
            prefs[Keys.CachedUser] = json.encodeToString(user)
        }
    }

    suspend fun saveCachedUser(user: CachedUser) {
        store.edit { it[Keys.CachedUser] = json.encodeToString(user) }
    }

    /**
     * What Google said about the signed-in student, kept beside the session.
     *
     * Stored rather than held in memory because it has to survive the two things
     * that outlive the credential sheet: a profile refresh, which rewrites
     * [CachedUser] from the server's answer and would otherwise drop the avatar
     * on the floor, and a cold start, where there is no sheet to ask again.
     * Written only after a sign-in the server accepted, and dropped with
     * everything else on sign-out.
     *
     * Read on demand rather than exposed as a flow — nothing observes it, it is
     * only consulted at the moment an account is written to the cache.
     */
    suspend fun currentGoogleAccount(): GoogleAccount? =
        preferences.first()[Keys.GoogleAccount]?.let { raw ->
            runCatching { json.decodeFromString<GoogleAccount>(raw) }.getOrNull()
        }

    suspend fun saveGoogleAccount(account: GoogleAccount) {
        store.edit { it[Keys.GoogleAccount] = json.encodeToString(account) }
    }

    /**
     * Sign-out: drop the token, the profile and every cached read.
     *
     * The whole lot rather than the token alone. This is a shared-phone situation
     * as often as not — a student hands it to a friend at a booth — and leaving
     * one student's progress on screen for the next person is worse than making a
     * re-login re-fetch it. The queue goes too: a pending scan belongs to the
     * account that took it, and sending it under the next student's token would
     * credit the wrong person.
     */
    suspend fun clearSession() {
        store.edit { prefs ->
            prefs.remove(Keys.AuthToken)
            prefs.remove(Keys.CachedUser)
            // Goes with the profile it was filling in. Leaving it would hand the
            // next student on a shared phone the last one's photo.
            prefs.remove(Keys.GoogleAccount)
            prefs.remove(Keys.CachedProgress)
            prefs.remove(Keys.CachedAnnouncements)
            prefs.remove(Keys.PendingScans)
            // Booths and zones survive: they are the same public list for
            // everyone, so re-downloading them on the next sign-in is pure waste.
        }
    }

    // ---- Offline cache ---------------------------------------------------

    /**
     * The last successful read of each collection, verbatim.
     *
     * Stored as the server's own JSON rather than as parsed rows. A cache that
     * re-encodes into a second shape is a second thing to keep in step with the
     * API, and the only consumer decodes it with the same DTOs the network path
     * uses — so the string is the honest representation.
     */
    val cachedBooths: Flow<String?> = preferences.map { it[Keys.CachedBooths] }
    val cachedZones: Flow<String?> = preferences.map { it[Keys.CachedZones] }
    val cachedProgress: Flow<String?> = preferences.map { it[Keys.CachedProgress] }
    val cachedAnnouncements: Flow<String?> = preferences.map { it[Keys.CachedAnnouncements] }
    val cachedProgram: Flow<String?> = preferences.map { it[Keys.CachedProgram] }
    val cachedFairInfo: Flow<String?> = preferences.map { it[Keys.CachedFairInfo] }

    suspend fun cacheBooths(raw: String) = store.edit { it[Keys.CachedBooths] = raw }
    suspend fun cacheZones(raw: String) = store.edit { it[Keys.CachedZones] = raw }
    suspend fun cacheProgress(raw: String) = store.edit { it[Keys.CachedProgress] = raw }
    suspend fun cacheAnnouncements(raw: String) =
        store.edit { it[Keys.CachedAnnouncements] = raw }
    suspend fun cacheProgram(raw: String) = store.edit { it[Keys.CachedProgram] = raw }

    /**
     * Cached because the welcome screen prints the date before any request has
     * had time to land, and a first frame that shows one window and then swaps to
     * another is worse than showing the older one steadily.
     */
    suspend fun cacheFairInfo(raw: String) = store.edit { it[Keys.CachedFairInfo] = raw }

    // ---- Outbound queue --------------------------------------------------

    val pendingScans: Flow<List<PendingScan>> = preferences.map { prefs ->
        prefs[Keys.PendingScans].orEmpty().mapNotNull { raw ->
            runCatching { json.decodeFromString<PendingScan>(raw) }.getOrNull()
        }
    }

    /**
     * Queues a scan the server has not accepted yet.
     *
     * A set keyed on the encoded value, so queueing the same scan twice — a tap
     * that fired two requests, a retry after a crash — cannot enqueue it twice.
     * The server would have deduplicated it anyway on `client_id`; this saves the
     * round trip.
     */
    suspend fun enqueueScan(scan: PendingScan) {
        store.edit { prefs ->
            prefs[Keys.PendingScans] = prefs[Keys.PendingScans].orEmpty() + json.encodeToString(scan)
        }
    }

    /** Drops one scan once the server has accounted for it. */
    suspend fun dequeueScan(clientId: String) {
        store.edit { prefs ->
            prefs[Keys.PendingScans] = prefs[Keys.PendingScans].orEmpty().filterNot { raw ->
                runCatching { json.decodeFromString<PendingScan>(raw).clientId == clientId }
                    .getOrDefault(false)
            }.toSet()
        }
    }

    // ---- Preferences ------------------------------------------------------

    /** Defaults on: the buzz at a booth is the app's only non-visual feedback. */
    val hapticsEnabled: Flow<Boolean> = preferences.map { it[Keys.Haptics] != false }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        store.edit { it[Keys.Haptics] = enabled }
    }

    /**
     * The language the student picked, as a BCP-47 tag — or null for "follow the
     * phone".
     *
     * Null is a real, distinct answer rather than a stand-in for English. A phone
     * set to Thai should open the app in Thai without anyone choosing anything,
     * and storing "th" at install time would then be indistinguishable from a
     * student who had deliberately chosen Thai on an English phone — so the next
     * time they changed the phone's language, the app would ignore them.
     */
    val language: Flow<String?> = preferences.map { it[Keys.Language] }

    suspend fun setLanguage(tag: String?) {
        store.edit { prefs ->
            if (tag == null) prefs.remove(Keys.Language) else prefs[Keys.Language] = tag
        }
    }

    /**
     * Whether the "how the fair works" card has been shown.
     *
     * Survives sign-out on purpose: a student who signs out and back in during the
     * fair does not need telling twice, and one who reinstalls does.
     */
    val onboardingSeen: Flow<Boolean> = preferences.map { it[Keys.OnboardingSeen] == true }

    suspend fun setOnboardingSeen() {
        store.edit { it[Keys.OnboardingSeen] = true }
    }

    /** Settings' "erase everything on this phone". */
    suspend fun clearAll() {
        store.edit { it.clear() }
    }

    private object Keys {
        val AuthToken = stringPreferencesKey("auth_token")
        val CachedUser = stringPreferencesKey("cached_user")
        val GoogleAccount = stringPreferencesKey("google_account")
        val CachedBooths = stringPreferencesKey("cached_booths")
        val CachedZones = stringPreferencesKey("cached_zones")
        val CachedProgress = stringPreferencesKey("cached_progress")
        val CachedAnnouncements = stringPreferencesKey("cached_announcements")
        val CachedProgram = stringPreferencesKey("cached_program")
        val CachedFairInfo = stringPreferencesKey("cached_fair_info")
        val PendingScans = stringSetPreferencesKey("pending_scans")
        val Haptics = booleanPreferencesKey("haptics")
        val Language = stringPreferencesKey("language")
        val OnboardingSeen = booleanPreferencesKey("onboarding_seen")
    }
}
