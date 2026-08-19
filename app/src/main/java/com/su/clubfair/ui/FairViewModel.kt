package com.su.clubfair.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.su.clubfair.ClubFairApplication
import com.su.clubfair.data.FairRepository
import com.su.clubfair.data.PostOutcome
import com.su.clubfair.data.ReactionOutcome
import com.su.clubfair.data.ScanOutcome
import com.su.clubfair.data.SessionStatus
import com.su.clubfair.ui.model.Announcement
import com.su.clubfair.ui.model.Booth
import com.su.clubfair.ui.model.FairProgress
import com.su.clubfair.ui.model.Participant
import com.su.clubfair.ui.model.ProgramEntry
import com.su.clubfair.ui.model.Student
import com.su.clubfair.ui.model.Zone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Whether there is a signed-in student, once that is actually known.
 *
 * [Restoring] is not a nicety. Reading the token off DataStore is asynchronous,
 * so for the first frames after launch the app knows nothing about the session.
 * Treating "not yet known" as "signed out" is what makes a persisted login flash
 * the Welcome screen on every cold start.
 */
sealed interface SessionState {
    data object Restoring : SessionState
    data object SignedOut : SessionState

    data class SignedIn(
        val student: Student,
        val onboardingSeen: Boolean,
    ) : SessionState
}

/** Everything the signed-in shell renders, as one snapshot. */
data class FairUiState(
    val booths: List<Booth> = emptyList(),
    val zones: List<Zone> = emptyList(),
    val progress: FairProgress = FairProgress(),
    val announcements: List<Announcement> = emptyList(),
    val program: List<ProgramEntry> = emptyList(),
    val hapticsEnabled: Boolean = true,
    val pendingScans: Int = 0,
    val refreshing: Boolean = false,
    /** The last refresh could not reach the server; what is shown may be stale. */
    val offline: Boolean = false,
)

/**
 * The signed-in half of the app.
 *
 * Reads the repository, which reads su-server with DataStore as its cache. The
 * ViewModel holds no fair data of its own — the repository outlives it, so a
 * rotation does not re-fetch and a tab change does not lose a scan.
 */
/** How often the booth display asks for a fresh code — see [FairViewModel.boothDisplay]. */
private const val BoothPollMillis = 10_000L

/** The back-off when the account's booth could not be looked up at all. */
private const val BoothLookupRetryMillis = 30_000L

/**
 * How long su-server keeps accepting a code — `CheckInMaxAge`, three minutes.
 *
 * Mirrored here for one decision only: when a display that has lost the server
 * should stop showing the last code it got. It is deliberately the *accepted*
 * age and not the thirty-second rotation, because a code one minute old still
 * scans and blanking it would close a working booth.
 */
private const val CodeAcceptedMillis = 3 * 60 * 1000L

/**
 * What the booth owner's phone is showing.
 *
 * [payload] is null before the first poll lands and again once a code has aged
 * out with no replacement; [failing] says the last poll did not arrive, which is
 * true whether or not there is still a usable code on screen. The card needs both
 * — "here is a code, and it is no longer being refreshed" is a real state and the
 * one a booth owner has to be able to see at a glance.
 */
data class BoothDisplay(
    val booth: Booth? = null,
    val payload: String? = null,
    val fetchedAtMillis: Long = 0L,
    val failing: Boolean = false,
)

class FairViewModel(private val repository: FairRepository) : ViewModel() {

    init {
        // Cache first so the first frame has content, then the network. In that
        // order: a refresh takes a round trip, and the alternative is an empty
        // shell for however long the hall's signal takes.
        viewModelScope.launch {
            repository.primeFromCache()
            repository.refresh()
        }
    }

    val session: StateFlow<SessionState> =
        combine(repository.session, repository.onboardingSeen) { status, seen ->
            when (status) {
                SessionStatus.SignedOut -> SessionState.SignedOut
                is SessionStatus.SignedIn -> SessionState.SignedIn(status.student, seen)
            }
        }.stateIn(
            scope = viewModelScope,
            // Warm across a configuration change, so a rotation does not drop back
            // to Restoring and replay the entry animation.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionState.Restoring,
        )

    /**
     * The language the app is read in, as a BCP-47 tag, or null to follow the
     * phone.
     *
     * Held apart from [uiState] because it is read a level above the signed-in
     * shell — the sign-in screens are in it too — and because it must be
     * available before anything is drawn. The initial null is the phone's own
     * language, which is the right thing to show for the frame or two before
     * DataStore answers.
     */
    val language: StateFlow<String?> = repository.language.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    fun setLanguage(tag: String?) {
        viewModelScope.launch { repository.setLanguage(tag) }
    }

    /**
     * Whether the fair is worth drawing yet — see `FairRepository.ready`.
     *
     * Its own flow rather than a field on [SessionState.SignedIn]: it is a fact
     * about the data, not about the session, and folding it in would rebuild the
     * signed-in tree every time it changed.
     */
    val contentReady: StateFlow<Boolean> = repository.ready.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    /**
     * The four collections the repository owns, gathered first.
     *
     * Two steps rather than one, because `combine` has typed overloads up to five
     * flows and there are eight. The alternative — the vararg form over an
     * `Array<Any?>` — loses every type and needs a cast per field, which is how a
     * reordering becomes a runtime crash instead of a compile error.
     */
    private data class FairContent(
        val booths: List<Booth>,
        val zones: List<Zone>,
        val progress: FairProgress,
        val announcements: List<Announcement>,
        val program: List<ProgramEntry>,
    )

    private val content = combine(
        repository.booths,
        repository.zones,
        repository.progress,
        repository.announcements,
        repository.program,
        ::FairContent,
    )

    /** Whether a refresh is running, and whether the last one reached the server. */
    private data class SyncState(val refreshing: Boolean, val offline: Boolean, val pending: Int)

    private val sync = combine(
        repository.refreshing,
        repository.offline,
        repository.pendingScanCount,
        ::SyncState,
    )

    val uiState: StateFlow<FairUiState> =
        combine(content, sync, repository.hapticsEnabled) { fair, status, haptics ->
            FairUiState(
                booths = fair.booths,
                zones = fair.zones,
                progress = fair.progress,
                announcements = fair.announcements,
                program = fair.program,
                hapticsEnabled = haptics,
                pendingScans = status.pending,
                refreshing = status.refreshing,
                offline = status.offline,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FairUiState(),
        )

    /**
     * The booth display, for an account that runs a booth.
     *
     * A cold flow behind `WhileSubscribed`, which is the whole lifecycle: it
     * starts polling when the card that shows it is composed and stops when the
     * booth owner leaves Home. Nothing has to remember to switch it off, and a
     * student's phone never runs it at all because no screen ever collects it.
     *
     * The poll is ten seconds against a code that rotates every thirty. That
     * looks wasteful and is the interval su-server's own notes prescribe: the
     * refresh has to land inside the rotation with room for a slow request, and
     * three tries per window is what makes a single failed poll invisible.
     *
     * The booth is looked up once and kept. It is a row in `clubfair_booth_owner`
     * that changes when someone reassigns a stall, not something to re-ask about
     * every ten seconds; if the lookup fails there is nothing to poll, so that
     * case backs off to half a minute instead of hammering a server that is
     * evidently unwell.
     */
    val boothDisplay: StateFlow<BoothDisplay> = flow {
        var state = BoothDisplay()
        while (true) {
            if (state.booth == null) {
                state = state.copy(booth = repository.myBooth())
            }
            val booth = state.booth
            if (booth == null) {
                emit(state)
                delay(BoothLookupRetryMillis)
                continue
            }

            val payload = repository.boothCode(booth.id)
            state = if (payload != null) {
                state.copy(
                    payload = payload,
                    fetchedAtMillis = System.currentTimeMillis(),
                    failing = false,
                )
            } else {
                // Hold the last code up while it can still be scanned, and say
                // out loud that it is not being refreshed. Dropping it on the
                // first failed poll would blank a working booth over one dropped
                // request; holding it past `accepted_until` would leave students
                // scanning something the server has stopped taking, which is the
                // worse of the two because it looks like it is working.
                val dead = System.currentTimeMillis() - state.fetchedAtMillis > CodeAcceptedMillis
                state.copy(failing = true, payload = if (dead) null else state.payload)
            }
            emit(state)
            delay(BoothPollMillis)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(2_000),
        initialValue = BoothDisplay(),
    )

    /**
     * The most recent scan result, for the card at the foot of the Scan tab.
     *
     * Held here rather than in the screen so walking to Home and back does not
     * lose what the camera just read.
     */
    private val _lastScan = MutableStateFlow<ScanOutcome?>(null)
    val lastScan: StateFlow<ScanOutcome?> = _lastScan.asStateFlow()

    fun onScanned(payload: String) {
        // The analyzer fires per frame, so a code held in front of the lens would
        // otherwise produce thirty requests a second.
        if (_lastScan.value != null || scanning) return
        scanning = true
        viewModelScope.launch {
            try {
                _lastScan.value = repository.recordScan(payload)
            } finally {
                scanning = false
            }
        }
    }

    /** Guards the window between a tap and the outcome landing. */
    private var scanning = false

    fun clearScan() {
        _lastScan.value = null
    }

    /**
     * The current code for one booth, for the admin wall.
     *
     * A plain suspend call rather than a second polling flow: the wall shows one
     * booth at a time and the screen that opens it owns the loop, so there is no
     * lifecycle here to get wrong. Compare `boothDisplay`, which is a flow
     * because a booth owner's Home has no such moment to hang a loop off.
     */
    suspend fun boothCode(boothId: Int): String? = repository.boothCode(boothId)

    /** Who a scanned pass belongs to, for an admin's scanner. */
    suspend fun findParticipant(studentId: String): Participant? =
        repository.findParticipant(studentId)

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    /**
     * Why the last reaction did not stick, if it did not.
     *
     * Only failures land here — a reaction that saves has nothing to say, and
     * the chip lighting up is the confirmation.
     */
    private val _lastReaction = MutableStateFlow<ReactionOutcome?>(null)
    val lastReaction: StateFlow<ReactionOutcome?> = _lastReaction.asStateFlow()

    fun toggleReaction(postId: Long, emoji: String) {
        viewModelScope.launch {
            val outcome = repository.toggleReaction(postId, emoji)
            _lastReaction.value = outcome.takeIf { it != ReactionOutcome.Saved }
        }
    }

    fun clearReactionOutcome() {
        _lastReaction.value = null
    }

    /**
     * How the last announcement went, for the composer at the foot of Events.
     *
     * Held here rather than in the screen for the reason [lastScan] is: the
     * answer arrives after a round trip, and a tab change in the meantime
     * disposes the screen. It is also what tells the composer whether it may
     * clear the draft — see `EventsScreen`.
     */
    private val _lastPost = MutableStateFlow<PostOutcome?>(null)
    val lastPost: StateFlow<PostOutcome?> = _lastPost.asStateFlow()

    /** True while a post is in flight, so the send button cannot be tapped twice. */
    private val _posting = MutableStateFlow(false)
    val posting: StateFlow<Boolean> = _posting.asStateFlow()

    fun postAnnouncement(body: String) {
        if (_posting.value) return
        _posting.value = true
        viewModelScope.launch {
            try {
                _lastPost.value = repository.postAnnouncement(body)
            } finally {
                _posting.value = false
            }
        }
    }

    fun clearPostOutcome() {
        _lastPost.value = null
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setHapticsEnabled(enabled) }
    }

    fun markOnboardingSeen() {
        viewModelScope.launch { repository.markOnboardingSeen() }
    }

    fun signOut() {
        viewModelScope.launch { repository.signOut() }
    }

    fun eraseDevice() {
        viewModelScope.launch { repository.eraseDevice() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as ClubFairApplication
                FairViewModel(app.repository)
            }
        }
    }
}
