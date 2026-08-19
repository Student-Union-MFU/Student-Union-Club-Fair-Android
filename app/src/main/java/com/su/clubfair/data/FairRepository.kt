package com.su.clubfair.data

import com.su.clubfair.data.net.AnnouncementDto
import com.su.clubfair.data.net.ApiResult
import com.su.clubfair.data.net.BoothDto
import com.su.clubfair.data.net.CheckInRequest
import com.su.clubfair.data.net.ClubFairApi
import com.su.clubfair.data.net.GoogleAccount
import com.su.clubfair.data.net.ProgramEntryDto
import com.su.clubfair.data.net.ProgressDto
import com.su.clubfair.data.net.RegisterRequest
import com.su.clubfair.data.net.UpdateProfileRequest
import com.su.clubfair.data.net.UserDto
import com.su.clubfair.data.net.ZoneDto
import com.su.clubfair.data.net.isMissingToken
import com.su.clubfair.data.net.isUnauthorized
import com.su.clubfair.data.net.valueOrNull
import com.su.clubfair.ui.model.Announcement
import com.su.clubfair.ui.model.Booth
import com.su.clubfair.ui.model.FairProgress
import com.su.clubfair.ui.model.PrizeTier
import com.su.clubfair.ui.model.ProgramEntry
import com.su.clubfair.ui.model.Reaction
import com.su.clubfair.ui.model.Student
import com.su.clubfair.ui.model.Zone
import com.su.clubfair.ui.model.boothFrom
import com.su.clubfair.ui.model.filledFrom
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
import java.time.OffsetDateTime
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

    /**
     * The identifier or the password was wrong.
     *
     * Split out of [Rejected] so the app can phrase this one itself. su-server's
     * copy for a failed login names three things the student might have got
     * wrong — "รหัสนักศึกษา เบอร์โทร หรือรหัสผ่าน" — because the route accepts
     * all three as an identifier. The form only asks for a student id, so two of
     * the three are fields the reader never saw, and the message is Thai
     * whatever language the app is set to.
     *
     * Every other rejection still passes the server's own words through: only
     * the server knows about a flagged account or a rate limit, and it says so
     * better than a generic string could.
     */
    data object BadCredentials : AuthOutcome

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
 * What happened to a reaction tap.
 *
 * A reaction is the one write in this app that is neither queued nor retried: it
 * is a toggle on a shared count, and a tap replayed from a pocket an hour later
 * would land on a post the student has long stopped looking at. So it either
 * takes now or it says it did not.
 */
sealed interface ReactionOutcome {
    data object Saved : ReactionOutcome
    data class Rejected(val message: String?) : ReactionOutcome
    data object Offline : ReactionOutcome
}

/**
 * What happened to a staff member's announcement.
 *
 * Deliberately not a [ScanOutcome]-style `Queued` case. A scan is queued offline
 * because it is a fact about something that already happened — the student stood
 * at that booth — and holding it costs nothing. An announcement is the opposite:
 * it is an instruction to two thousand phones, its whole value is in arriving
 * now, and one that surfaced from a pocket an hour later would be worse than one
 * that was never sent. So this fails, keeps the draft, and lets the author
 * decide whether it is still worth sending.
 */
sealed interface PostOutcome {
    data object Posted : PostOutcome

    /** The server said no — an empty body, one over 2000 characters, or not staff. */
    data class Rejected(val message: String?) : PostOutcome

    /** Never reached the server. Nothing was published. */
    data object Offline : PostOutcome
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
    private val _program = MutableStateFlow<List<ProgramEntry>>(emptyList())

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

    /**
     * Whether there is anything worth drawing yet.
     *
     * False in exactly two windows: before the cache has been read at process
     * start, and between a sign-in and the first fetch made with the new token.
     * The second is the one that is visibly slow — [completeSignIn] runs four
     * round trips before the shell has a booth list, a rank or a prize tier, and
     * without this the fair opens on a screen of zeroes that then rearrange
     * themselves.
     *
     * Not the same question as [refreshing]. That one is true on every pull and
     * every resume, when there is already a fair on screen and the right thing
     * to show is a spinner in the corner rather than a screen of its own.
     */
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    /**
     * True when the session ended because the server stopped accepting the token.
     *
     * Distinct from plain signed-out, and it has to be. A token lasts 30 days and
     * then simply stops working; without this the app kept the student "signed
     * in" against a token every authenticated call was rejecting, which is the
     * worst of the three possible states — the booth list and the zones are
     * public, so they kept arriving, `offline` stayed false, and the app looked
     * healthy while progress, announcements and every scan failed silently. There
     * was no way out but to find Sign out and use it.
     *
     * Set by [endExpiredSession] and cleared by a sign-in or a deliberate
     * [signOut], so it stays true for exactly the window in which the sign-in
     * screen has something to explain. See `AuthViewModel`, which turns it into
     * the notice on the login form.
     */
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    val booths: StateFlow<List<Booth>> = _booths.asStateFlow()
    val zones: StateFlow<List<Zone>> = _zones.asStateFlow()
    val progress: StateFlow<FairProgress> = _progress.asStateFlow()
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()
    val program: StateFlow<List<ProgramEntry>> = _program.asStateFlow()
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    val hapticsEnabled: Flow<Boolean> = store.hapticsEnabled
    val language: Flow<String?> = store.language
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
        store.cachedProgram.first()?.let { raw ->
            decode<List<ProgramEntryDto>>(raw)?.let { dtos ->
                _program.value = dtos.mapNotNull { it.toModel() }
            }
        }

        // A cache with booths in it is a fair that can be drawn now, whatever the
        // network is doing — which is the whole point of having one. An empty
        // cache waits for the fetch instead.
        if (_booths.value.isNotEmpty()) _ready.value = true
    }

    private inline fun <reified T> decode(raw: String): T? =
        runCatching { json.decodeFromString<T>(raw) }.getOrNull()

    // ---- Refresh ---------------------------------------------------------

    private val refreshLock = Mutex()

    /**
     * Pulls everything the signed-in shell shows.
     *
     * Serialised behind a mutex: the shell refreshes on launch, on resume, on a
     * pull and from Settings, and overlapping refreshes would race to write the
     * same flows — with the slowest response winning regardless of which was
     * newest. A call that arrives while one is running returns immediately rather
     * than queueing, because the one in flight is already fetching what it wants.
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

                api.progress().orEndSession().valueOrNull()?.let { dto ->
                    reachedServer = true
                    store.cacheProgress(json.encodeToString(dto))
                    applyProgress(dto)
                }

                api.announcements().orEndSession().valueOrNull()?.let { dtos ->
                    reachedServer = true
                    store.cacheAnnouncements(json.encodeToString(dtos))
                    _announcements.value = dtos.map { it.toModel() }
                }

                // Unauthenticated, so no `orEndSession` — a 401 from this route
                // would say nothing about the session and there is nothing here
                // to end one over.
                api.program().valueOrNull()?.let { dtos ->
                    reachedServer = true
                    store.cacheProgram(json.encodeToString(dtos))
                    _program.value = dtos.mapNotNull { it.toModel() }
                }

                _offline.value = !reachedServer
            } finally {
                _refreshing.value = false
                // Set even when nothing was reached. A student in a hall with no
                // signal has waited as long as waiting can help; the shell shows
                // the cache and the offline banner, which is more use than a
                // loading screen that will never finish.
                _ready.value = true
            }
        }
    }

    /**
     * Ends the session if this is the server refusing the token we sent.
     *
     * Wrapped around every authenticated call rather than checked at one chosen
     * place, because there is no such place: a token expires between two requests,
     * not at a moment the app picks, so whichever call happens to be next is the
     * one that finds out. The result is returned unchanged, so a call site reads
     * as it did before and the ordinary failure paths still run.
     *
     * The token is re-read before acting on it. A 401 can land after a deliberate
     * sign-out — the request was in flight while the session was being cleared —
     * and treating that as an expiry would tell a student who had just signed out
     * that they had been logged out.
     */
    private suspend fun <T> ApiResult<T>.orEndSession(): ApiResult<T> {
        if (isUnauthorized && store.currentToken() != null) endExpiredSession()
        return this
    }

    /** Progress and the booth ticks are two views of one fact; they move together. */
    private fun applyProgress(dto: ProgressDto) {
        _progress.value = dto.toModel()
        val visited = dto.visitedBoothIds.toSet()
        _booths.value = _booths.value.map { it.copy(scanned = it.id in visited) }
    }

    // ---- Auth ------------------------------------------------------------

    /**
     * Signs in with Google, keeping the profile that came with the token.
     *
     * [account] is stored only once the server has accepted the token, and
     * before [completeSignIn] writes the cache — that ordering is the whole
     * trick, since the merge reads it back out of the store. Saving it on a
     * rejected sign-in would leave a profile for a session that never existed.
     */
    suspend fun signInWithGoogle(idToken: String, account: GoogleAccount): AuthOutcome {
        val result = api.signInWithGoogle(idToken)
        if (result is ApiResult.Success) store.saveGoogleAccount(account)
        return completeSignIn(result)
    }

    suspend fun signInWithPassword(studentId: String, password: String): AuthOutcome {
        val result = api.signInWithPassword(studentId, password)
        // A 401 from *this* route is the credentials, not an expired session:
        // the call carries no token to expire. Everything else — a 429, a
        // disabled account — keeps su-server's own message.
        if (result.isUnauthorized) return AuthOutcome.BadCredentials
        return completeSignIn(result)
    }

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
            val user = result.value.user
            val student = Student.from(user).filledFrom(store.currentGoogleAccount())
            // Before the session is written, not after. Writing the session is
            // what flips the gate in `MainActivity`, and a gate that saw
            // "signed in" while this was still true would draw one frame of the
            // shell — empty — before the loading screen replaced it.
            _ready.value = false
            // Whatever the old token's fate, it has been answered.
            _sessionExpired.value = false
            store.saveSession(result.value.token, student.toCachedUser(user.role))
            // Straight into a refresh, so the shell opens on real data rather than
            // on a frame of empty state followed by a jump.
            refresh()
            AuthOutcome.Success
        }

        is ApiResult.Offline -> AuthOutcome.Offline
        is ApiResult.Failure -> AuthOutcome.Rejected(result.message)
    }

    /**
     * Ends the session locally, because the student asked.
     *
     * There is no server call: the token is stateless and expires on its own, so
     * "sign out" means forgetting it. A revocation endpoint would be worth having
     * if a token were ever long-lived enough for it to matter.
     */
    suspend fun signOut() {
        forgetSession()
        // Nothing expired. This is the student closing the door behind them, and
        // the sign-in screen has nothing to explain.
        _sessionExpired.value = false
    }

    /**
     * Ends the session because the server stopped accepting the token.
     *
     * The same clearing as [signOut] and a different flag, which is the whole
     * difference between the two: one of them owes the student an explanation on
     * the way out. See [sessionExpired].
     *
     * The queued scans go with it, as they do on any sign-out. That is a real
     * loss — those scans were taken and are now unsendable — and it is still the
     * right call for the reason `ClubFairStore.clearSession` gives: nothing here
     * knows that the next student to sign in on this phone is the one who took
     * them, and crediting the wrong person is worse than losing a stamp that can
     * be re-scanned by walking back to the booth.
     */
    private suspend fun endExpiredSession() {
        forgetSession()
        _sessionExpired.value = true
    }

    /** What both ways out of a session have in common. */
    private suspend fun forgetSession() {
        store.clearSession()
        _progress.value = FairProgress()
        _announcements.value = emptyList()
        _booths.value = _booths.value.map { it.copy(scanned = false) }
    }

    /**
     * Finishes sign-up for an account Google created.
     *
     * Two calls, in this order and not the other: the password first, the rest
     * second. `profile_complete` is computed from all four fields at once, so
     * the profile update is the only one of the two whose response can come back
     * with the flag already true — running them the other way round would cache
     * a student as still-incomplete and bounce them back to the form they had
     * just finished.
     *
     * Not transactional, and it does not need to be. A password that lands and a
     * profile update that does not leaves the account exactly where it started:
     * incomplete, on the same form, with the password box the only thing already
     * satisfied. Retrying is safe because both calls are writes of the same
     * values, not increments.
     */
    suspend fun completeSignUp(
        phone: String,
        school: String,
        major: String,
        password: String,
    ): AuthOutcome {
        when (val result = api.setPassword(password).orEndSession()) {
            is ApiResult.Success -> Unit
            is ApiResult.Offline -> return AuthOutcome.Offline
            is ApiResult.Failure -> return AuthOutcome.Rejected(result.message)
        }

        return when (
            val result = api.updateProfile(UpdateProfileRequest(phone, school, major))
                .orEndSession()
        ) {
            is ApiResult.Success -> {
                cacheAccount(result.value)
                AuthOutcome.Success
            }

            is ApiResult.Offline -> AuthOutcome.Offline
            is ApiResult.Failure -> AuthOutcome.Rejected(result.message)
        }
    }

    suspend fun updateProfile(phone: String?, school: String?, major: String?): Boolean {
        val result = api.updateProfile(UpdateProfileRequest(phone, school, major)).orEndSession()
        val user = result.valueOrNull() ?: return false
        cacheAccount(user)
        return true
    }

    /**
     * Writes a server account to the cache, with Google's copy filling the gaps.
     *
     * Every write of [CachedUser] outside sign-in goes through here, and that is
     * the point rather than tidiness: a site that called `saveCachedUser`
     * directly would drop whatever Google was covering the next time the student
     * edited their profile, and the field to notice it on would be the phone.
     * There is one such path left — sign-in itself, which writes the token in the
     * same edit and so applies the same merge inline.
     */
    private suspend fun cacheAccount(user: UserDto) {
        val student = Student.from(user).filledFrom(store.currentGoogleAccount())
        store.saveCachedUser(student.toCachedUser(user.role))
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

        return when (val result = api.recordCheckIn(request).orEndSession()) {
            is ApiResult.Success -> {
                val boothId = result.value.checkIn?.boothId
                // A scan that reached the server is the best evidence there is
                // that the queue behind it can go up too, and this is the moment
                // it matters: a student who scanned three booths in a dead corner
                // and then found signal is standing at the fourth. Waiting for the
                // next refresh to notice would leave those three sitting in the
                // queue while the app cheerfully records the one in front of them.
                flushPendingScans()
                // Re-read rather than adjusting the local count: the server is the
                // authority on how many booths this student has, and guessing would
                // let the two drift on any outcome the client mispredicted. Read
                // after the flush, so it counts what was just sent.
                api.progress().orEndSession().valueOrNull()?.let { applyProgress(it) }

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
     *
     * With one exception: a scan the server would not even look at because of who
     * sent it. A 401 says nothing about the code — it will be perfectly good to
     * the next session, if the queue survived — so dequeuing on it would throw
     * away every waiting scan for a reason that has nothing to do with any of
     * them. It ends the session instead and stops, exactly as the offline case
     * does, for the same reason: the rest of the queue would fail identically.
     *
     * Not serialised against itself, and does not need to be. [refresh] calls
     * this under its mutex and [recordScan] calls it outside one, so a scan taken
     * while a refresh is running can start a second drain over the same queue.
     * What that costs is a duplicate request: `client_id` is minted once per scan
     * and never changes, so the server answers the second copy `duplicate_request`
     * rather than counting it twice, and [dequeueScan] is a filter rather than a
     * decrement. A lock here would buy nothing and would make a scan wait on a
     * refresh, which is the wrong way round — the scan is the thing the student is
     * standing there doing.
     */
    private suspend fun flushPendingScans() {
        val queued = store.pendingScans.first()
        for (scan in queued) {
            val result = api.recordCheckIn(
                CheckInRequest(scan.payload, scan.clientId, scan.deviceTime),
            ).orEndSession()
            when {
                result is ApiResult.Success -> store.dequeueScan(scan.clientId)
                // Still no network. Stop: the rest will fail the same way, and
                // hammering a dead connection costs battery for nothing.
                result is ApiResult.Offline -> return
                // Not signed in, or no longer signed in. `orEndSession` has
                // already cleared the session in the second case.
                result.isUnauthorized || result.isMissingToken -> return
                else -> store.dequeueScan(scan.clientId)
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
    suspend fun toggleReaction(postId: Long, emoji: String): ReactionOutcome {
        val before = _announcements.value
        _announcements.value = before.map { post ->
            if (post.id == postId) post.toggling(emoji) else post
        }

        val result = api.toggleReaction(postId, emoji).orEndSession()
        if (result.valueOrNull() == null) {
            _announcements.value = before
            // The rollback used to be the whole of the failure handling, which
            // made a rejected tap indistinguishable from one that never
            // registered: the chip appeared and vanished inside a frame and the
            // student was left tapping a picker that closed and did nothing.
            // Saying so costs a line and is the difference between "this app is
            // broken" and "that did not go through".
            return when (result) {
                is ApiResult.Offline -> ReactionOutcome.Offline
                is ApiResult.Failure -> ReactionOutcome.Rejected(result.message)
                is ApiResult.Success -> ReactionOutcome.Rejected(null)
            }
        }
        // Re-read the channel so the counts are everyone's, not this device's
        // guess at everyone's.
        api.announcements().orEndSession().valueOrNull()?.let { dtos ->
            store.cacheAnnouncements(json.encodeToString(dtos))
            _announcements.value = dtos.map { it.toModel() }
        }
        return ReactionOutcome.Saved
    }

    /**
     * Publishes an announcement, then reconciles the channel against the server.
     *
     * The created post goes into the list first, from the 201 body, so the author
     * sees it land at once. Then the whole channel is re-read for the same reason
     * [toggleReaction] re-reads it: ordering and reaction counts are everyone's,
     * not this device's guess at everyone's, and a post someone else made in the
     * meantime arrives with it.
     *
     * The re-read is allowed to fail. It is a refresh, not the write — the post
     * is already published at that point, and dropping the optimistic insert
     * because the second call timed out would tell the author their announcement
     * had not been sent when it had. That is the one wrong answer available here.
     */
    suspend fun postAnnouncement(body: String): PostOutcome {
        val created = when (val result = api.postAnnouncement(body).orEndSession()) {
            is ApiResult.Success -> result.value
            is ApiResult.Offline -> return PostOutcome.Offline
            is ApiResult.Failure -> return PostOutcome.Rejected(result.message)
        }

        _announcements.value = _announcements.value + created.toModel()

        api.announcements().orEndSession().valueOrNull()?.let { dtos ->
            store.cacheAnnouncements(json.encodeToString(dtos))
            _announcements.value = dtos.map { it.toModel() }
        }
        return PostOutcome.Posted
    }

    // ---- Preferences -----------------------------------------------------

    suspend fun setHapticsEnabled(enabled: Boolean) = store.setHapticsEnabled(enabled)

    /** Null means "follow the phone" — see `ClubFairStore.language`. */
    suspend fun setLanguage(tag: String?) = store.setLanguage(tag)

    suspend fun markOnboardingSeen() = store.setOnboardingSeen()

    /** Settings' "erase everything on this phone". */
    suspend fun eraseDevice() {
        store.clearAll()
        // As deliberate an exit as [signOut], and owed no explanation either.
        _sessionExpired.value = false
        _progress.value = FairProgress()
        _announcements.value = emptyList()
        _program.value = emptyList()
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

/**
 * One programme entry, with its two timestamps read.
 *
 * Null — dropping the entry — when `starts_at` will not parse, which is the one
 * field the whole row is useless without: an entry with no time cannot be placed
 * on the route, cannot be compared against the clock, and would sit at the top
 * of the running order claiming to be first. That is worse than one row missing
 * from a schedule the server can fix.
 *
 * `OffsetDateTime` rather than `Instant.parse`, which is strict about wanting a
 * `Z`. su-server sends UTC today because its database session is UTC, and a
 * deployment whose session is Asia/Bangkok would send `+07:00` instead —
 * correct, the same moment, and unparseable by the stricter reader.
 */
private fun ProgramEntryDto.toModel(): ProgramEntry? {
    val startsAt = parseInstant(startsAt) ?: return null
    return ProgramEntry(
        id = id,
        startsAtMillis = startsAt,
        // An unreadable end is treated as absent rather than as a reason to drop
        // the entry: "we know when it starts and not when it finishes" is a
        // state the type already has a place for.
        endsAtMillis = endsAt?.let(::parseInstant),
        title = title,
        titleEn = titleEn,
        detail = detail,
        detailEn = detailEn,
        location = location,
        locationEn = locationEn,
        zoneCode = zone,
    )
}

private fun parseInstant(raw: String): Long? =
    runCatching { OffsetDateTime.parse(raw).toInstant().toEpochMilli() }.getOrNull()

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
