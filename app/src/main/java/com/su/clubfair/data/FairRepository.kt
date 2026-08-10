package com.su.clubfair.data

import com.su.clubfair.data.net.AnnouncementDto
import com.su.clubfair.data.net.ApiResult
import com.su.clubfair.data.net.BoothDto
import com.su.clubfair.data.net.CheckInRequest
import com.su.clubfair.data.net.ClubFairApi
import com.su.clubfair.data.net.ProgressDto
import com.su.clubfair.data.net.RegisterRequest
import com.su.clubfair.data.net.UpdateProfileRequest
import com.su.clubfair.data.net.ZoneDto
import com.su.clubfair.data.net.valueOrNull
import com.su.clubfair.ui.model.Announcement
import com.su.clubfair.ui.model.Booth
import com.su.clubfair.ui.model.FairProgress
import com.su.clubfair.ui.model.PrizeTier
import com.su.clubfair.ui.model.Reaction
import com.su.clubfair.ui.model.Student
import com.su.clubfair.ui.model.Zone
import com.su.clubfair.ui.model.boothFrom
import com.su.clubfair.ui.model.toCachedUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

/**
 * Whether anyone is signed in.
 *
 * No `Restoring` case here on purpose. "The token has not been read off disk yet"
 * is not a fact about the session, it is the absence of one — the flow simply has
 * not emitted. `FairViewModel` represents it as the initial value of its own
 * `StateFlow`, which is where it belongs and where it cannot be mistaken for a
 * state the repository might return.
 */
sealed interface SessionStatus {
    data object SignedOut : SessionStatus
    data class SignedIn(val student: Student) : SessionStatus
}

/** What a sign-in attempt came back with. */
sealed interface AuthOutcome {
    data object Success : AuthOutcome

    /** The server said no, with its own message — already in the reader's language. */
    data class Rejected(val message: String?) : AuthOutcome

    /** Never reached the server. Signing in is the one thing that cannot be queued. */
    data object Offline : AuthOutcome
}

/** What happened to a scan. */
sealed interface ScanOutcome {
    data class Recorded(val booth: Booth?) : ScanOutcome
    data class AlreadyScanned(val booth: Booth?) : ScanOutcome

    /** Not a fair code, or one that did not verify. Carries the server's message. */
    data class Rejected(val message: String?) : ScanOutcome

    /** Expired — the student did nothing wrong and re-scanning fixes it. */
    data class Expired(val message: String?) : ScanOutcome

    /**
     * Held on the device because there was no signal. Not a failure: it is
     * recorded locally and will go up on the next successful call.
     */
    data object Queued : ScanOutcome
}

/**
 * The one thing the UI reads the fair through.
 *
 * Server-first, cache-backed. Every collection is a [StateFlow] seeded from
 * DataStore and replaced by a successful fetch, which is what lets a student in a
 * concrete hall with no signal still see the fair. Three rules hold throughout:
 *
 *  1. **A read never fails visibly.** If the network is down the cached value
 *     stays on screen. There is nothing useful to say about a stale booth list.
 *  2. **A write is queued, never lost.** A scan taken offline goes into
 *     `pendingScans` and is retried; its `client_id` makes the retry idempotent,
 *     so a resend cannot double-count.
 *  3. **Sign-in is the exception to both.** It cannot be answered from a cache
 *     and it cannot be queued, so [AuthOutcome.Offline] is a real state the UI
 *     has to show.
 */
class FairRepository(
    private val store: ClubFairStore,
    private val api: ClubFairApi,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _booths = MutableStateFlow<List<Booth>>(emptyList())
    private val _zones = MutableStateFlow<List<Zone>>(emptyList())
    private val _progress = MutableStateFlow(FairProgress())
    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())

    /** True while a refresh is in flight, for a pull-to-refresh indicator. */
    private val _refreshing = MutableStateFlow(false)

    /**
     * Set when the last refresh could not reach the server.
     *
     * A banner, not an error screen: the cached data is still on display, so this
     * says "what you are looking at may be out of date" rather than "something
     * broke".
     */
    private val _offline = MutableStateFlow(false)

    val booths: StateFlow<List<Booth>> = _booths.asStateFlow()
    val zones: StateFlow<List<Zone>> = _zones.asStateFlow()
    val progress: StateFlow<FairProgress> = _progress.asStateFlow()
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    val hapticsEnabled: Flow<Boolean> = store.hapticsEnabled
    val onboardingSeen: Flow<Boolean> = store.onboardingSeen
    val pendingScanCount: Flow<Int> = store.pendingScans.map { it.size }

    /**
     * Whether anyone is signed in.
     *
     * Both halves are needed: a token with no cached profile cannot render the
     * shell, and a profile with no token cannot make a request. Either missing is
     * signed out.
     */
    val session: Flow<SessionStatus> =
        combine(store.authToken, store.cachedUser) { token, cached ->
            when {
                token.isNullOrBlank() -> SessionStatus.SignedOut
                cached == null -> SessionStatus.SignedOut
                else -> SessionStatus.SignedIn(Student.from(cached))
            }
        }

    // ---- Startup ---------------------------------------------------------

    /**
     * Loads whatever the last session cached, before any network call.
     *
     * Called once at process start so the first frame after launch has content.
     * Silent on failure by design: a cache that will not decode is a cache that
     * gets replaced by the refresh a moment later.
     */
    suspend fun primeFromCache() {
        store.cachedZones.first()?.let { raw ->
            decode<List<ZoneDto>>(raw)?.let { dtos -> _zones.value = dtos.map(Zone::from) }
        }
        val visited = store.cachedProgress.first()
            ?.let { decode<ProgressDto>(it) }
            ?.also { _progress.value = it.toModel() }
            ?.visitedBoothIds?.toSet()
            ?: emptySet()

        store.cachedBooths.first()?.let { raw ->
            decode<List<BoothDto>>(raw)?.let { dtos ->
                _booths.value = dtos.map { boothFrom(it, scanned = it.id in visited) }
            }
        }
        store.cachedAnnouncements.first()?.let { raw ->
            decode<List<AnnouncementDto>>(raw)?.let { dtos ->
                _announcements.value = dtos.map { it.toModel() }
            }
        }
    }

    private inline fun <reified T> decode(raw: String): T? =
        runCatching { json.decodeFromString<T>(raw) }.getOrNull()

    // ---- Refresh ---------------------------------------------------------

    private val refreshLock = Mutex()

    /**
     * Pulls everything the signed-in shell shows.
     *
     * Serialised behind a mutex: the shell refreshes on launch, on resume and on a
     * pull, and three overlapping refreshes would race to write the same flows —
     * with the slowest response winning regardless of which was newest.
     *
     * The queue is drained first. A scan taken offline should be counted before
     * progress is read, or the student watches their own scan appear one refresh
     * late.
     */
    suspend fun refresh() {
        if (refreshLock.isLocked) return
        refreshLock.withLock {
            _refreshing.value = true
            try {
                flushPendingScans()

                var reachedServer = false

                api.booths().also { result ->
                    result.valueOrNull()?.let { dtos ->
                        reachedServer = true
                        store.cacheBooths(json.encodeToString(dtos))
                        val visited = _progress.value.visitedBoothIds
                        _booths.value = dtos.map { boothFrom(it, scanned = it.id in visited) }
                    }
                }

                api.zones().valueOrNull()?.let { dtos ->
                    reachedServer = true
                    store.cacheZones(json.encodeToString(dtos))
                    _zones.value = dtos.map(Zone::from)
                }

                api.progress().valueOrNull()?.let { dto ->
                    reachedServer = true
                    store.cacheProgress(json.encodeToString(dto))
                    applyProgress(dto)
                }

                api.announcements().valueOrNull()?.let { dtos ->
                    reachedServer = true
                    store.cacheAnnouncements(json.encodeToString(dtos))
                    _announcements.value = dtos.map { it.toModel() }
                }

                _offline.value = !reachedServer
            } finally {
                _refreshing.value = false
            }
        }
    }

    /** Progress and the booth ticks are two views of one fact; they move together. */
    private fun applyProgress(dto: ProgressDto) {
        _progress.value = dto.toModel()
        val visited = dto.visitedBoothIds.toSet()
        _booths.value = _booths.value.map { it.copy(scanned = it.id in visited) }
    }

    // ---- Auth ------------------------------------------------------------

    suspend fun signInWithGoogle(idToken: String): AuthOutcome =
        completeSignIn(api.signInWithGoogle(idToken))

    suspend fun signInWithPassword(phone: String, password: String): AuthOutcome =
        completeSignIn(api.signInWithPassword(phone, password))

    suspend fun register(
        firstName: String,
        surname: String,
        email: String,
        phone: String,
        school: String?,
        major: String?,
        password: String,
    ): AuthOutcome = completeSignIn(
        api.register(
            RegisterRequest(
                firstName = firstName,
                surname = surname,
                email = email,
                phone = phone,
                // Deliberately not sent. The server derives it from the MFU email
                // local part, which is the same number and cannot be mistyped.
                studentId = null,
                school = school,
                major = major,
                password = password,
            ),
        ),
    )

    private suspend fun completeSignIn(
        result: ApiResult<com.su.clubfair.data.net.SessionDto>,
    ): AuthOutcome = when (result) {
        is ApiResult.Success -> {
            val student = Student.from(result.value.user)
            store.saveSession(result.value.token, student.toCachedUser(result.value.user.role))
            // Straight into a refresh, so the shell opens on real data rather than
            // on a frame of empty state followed by a jump.
            refresh()
            AuthOutcome.Success
        }

        is ApiResult.Offline -> AuthOutcome.Offline
        is ApiResult.Failure -> AuthOutcome.Rejected(result.message)
    }

    /**
     * Ends the session locally.
     *
     * There is no server call: the token is stateless and expires on its own, so
     * "sign out" means forgetting it. A revocation endpoint would be worth having
     * if a token were ever long-lived enough for it to matter.
     */
    suspend fun signOut() {
        store.clearSession()
        _progress.value = FairProgress()
        _announcements.value = emptyList()
        _booths.value = _booths.value.map { it.copy(scanned = false) }
    }

    suspend fun updateProfile(phone: String?, school: String?, major: String?): Boolean {
        val result = api.updateProfile(UpdateProfileRequest(phone, school, major))
        val user = result.valueOrNull() ?: return false
        store.saveCachedUser(Student.from(user).toCachedUser(user.role))
        return true
    }

    // ---- Scanning --------------------------------------------------------

    /**
     * Sends a scanned code, or queues it.
     *
     * The payload goes up verbatim. The client does not parse it, does not know
     * which booth it names, and cannot: the code is an HMAC only the server can
     * check. That is the point — it is why a student can no longer type a booth
     * number in by hand.
     */
    suspend fun recordScan(payload: String): ScanOutcome {
        val clientId = UUID.randomUUID().toString()
        val deviceTime = Instant.ofEpochMilli(clock()).toString()
        val request = CheckInRequest(payload, clientId, deviceTime)

        return when (val result = api.recordCheckIn(request)) {
            is ApiResult.Success -> {
                val boothId = result.value.checkIn?.boothId
                // Re-read rather than adjusting the local count: the server is the
                // authority on how many booths this student has, and guessing would
                // let the two drift on any outcome the client mispredicted.
                api.progress().valueOrNull()?.let { applyProgress(it) }

                val booth = boothId?.let { id -> _booths.value.firstOrNull { it.id == id } }
                when (result.value.outcome) {
                    "recorded" -> ScanOutcome.Recorded(booth)
                    // A replay of the app's own queued scan is a success the
                    // student has already been told about; treat it as the stamp
                    // it is rather than as an error.
                    "duplicate_request" -> ScanOutcome.Recorded(booth)
                    else -> ScanOutcome.AlreadyScanned(booth)
                }
            }

            is ApiResult.Offline -> {
                store.enqueueScan(PendingScan(payload, clientId, deviceTime))
                ScanOutcome.Queued
            }

            is ApiResult.Failure -> when (result.status) {
                // The window passed. Its own outcome: re-scanning fixes it, and
                // telling the student the code was "invalid" would send them
                // looking for a different booth.
                409 -> ScanOutcome.Expired(result.message)
                else -> ScanOutcome.Rejected(result.message)
            }
        }
    }

    /**
     * Sends whatever is waiting, oldest first.
     *
     * A rejection dequeues as firmly as a success. A queued code whose window has
     * passed will never be accepted, so keeping it would retry forever on every
     * refresh — the honest outcome is that the scan is lost and the student must
     * re-scan, which is the cost of rotating codes stated in the server's own
     * comments.
     */
    private suspend fun flushPendingScans() {
        val queued = store.pendingScans.first()
        for (scan in queued) {
            when (api.recordCheckIn(CheckInRequest(scan.payload, scan.clientId, scan.deviceTime))) {
                is ApiResult.Success -> store.dequeueScan(scan.clientId)
                is ApiResult.Failure -> store.dequeueScan(scan.clientId)
                // Still no network. Stop: the rest will fail the same way, and
                // hammering a dead connection costs battery for nothing.
                is ApiResult.Offline -> return
            }
        }
    }

    // ---- Channel ---------------------------------------------------------

    /**
     * Toggles a reaction, updating the screen before the server answers.
     *
     * Optimistic because a chip that waits for a round trip feels broken, and the
     * write is trivially reversible. The server's answer wins: on failure the flip
     * is undone, so a rejected tap does not leave a chip lit.
     */
    suspend fun toggleReaction(postId: Long, emoji: String) {
        val before = _announcements.value
        _announcements.value = before.map { post ->
            if (post.id == postId) post.toggling(emoji) else post
        }

        val result = api.toggleReaction(postId, emoji)
        if (result.valueOrNull() == null) {
            _announcements.value = before
            return
        }
        // Re-read the channel so the counts are everyone's, not this device's
        // guess at everyone's.
        api.announcements().valueOrNull()?.let { dtos ->
            store.cacheAnnouncements(json.encodeToString(dtos))
            _announcements.value = dtos.map { it.toModel() }
        }
    }

    // ---- Preferences -----------------------------------------------------

    suspend fun setHapticsEnabled(enabled: Boolean) = store.setHapticsEnabled(enabled)

    suspend fun markOnboardingSeen() = store.setOnboardingSeen()

    /** Settings' "erase everything on this phone". */
    suspend fun eraseDevice() {
        store.clearAll()
        _progress.value = FairProgress()
        _announcements.value = emptyList()
        _booths.value = emptyList()
        _zones.value = emptyList()
    }
}

// ---- Mapping -------------------------------------------------------------

private fun ProgressDto.toModel() = FairProgress(
    visited = visited,
    total = total,
    visitedBoothIds = visitedBoothIds.toSet(),
    rank = rank,
    prizes = prizes.map {
        PrizeTier(
            id = it.id,
            threshold = it.threshold,
            name = it.name,
            description = it.description,
            reached = it.reached,
            claimed = it.claimed,
        )
    },
)

private fun AnnouncementDto.toModel() = Announcement(
    id = id,
    author = author,
    postedAtMillis = runCatching { Instant.parse(postedAt).toEpochMilli() }
        // An unparseable timestamp shows as "just now" rather than 1970, which
        // would read as a corrupted post rather than a formatting slip.
        .getOrDefault(System.currentTimeMillis()),
    body = body,
    reactions = reactions.map { Reaction(it.emoji, it.count, it.mine) },
)

/**
 * The local half of an optimistic reaction tap.
 *
 * Only has to be right for the instant before the server answers — the real
 * counts arrive immediately after — but it has to be right in the same direction,
 * or the chip flickers the wrong way first.
 */
private fun Announcement.toggling(emoji: String): Announcement {
    val existing = reactions.firstOrNull { it.emoji == emoji }
    val updated = when {
        existing == null -> reactions + Reaction(emoji, count = 1, mine = true)
        // Last one out removes the chip rather than leaving a dead 0.
        existing.mine && existing.count == 1 -> reactions - existing
        existing.mine -> reactions.map {
            if (it.emoji == emoji) it.copy(count = it.count - 1, mine = false) else it
        }
        else -> reactions.map {
            if (it.emoji == emoji) it.copy(count = it.count + 1, mine = true) else it
        }
    }
    return copy(reactions = updated)
}
