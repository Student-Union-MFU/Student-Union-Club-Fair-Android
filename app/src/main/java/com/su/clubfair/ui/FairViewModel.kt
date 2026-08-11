package com.su.clubfair.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.su.clubfair.ClubFairApplication
import com.su.clubfair.data.FairRepository
import com.su.clubfair.data.ScanOutcome
import com.su.clubfair.data.SessionStatus
import com.su.clubfair.ui.model.Announcement
import com.su.clubfair.ui.model.Booth
import com.su.clubfair.ui.model.FairProgress
import com.su.clubfair.ui.model.Student
import com.su.clubfair.ui.model.Zone
import kotlinx.coroutines.flow.MutableStateFlow
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
    )

    private val content = combine(
        repository.booths,
        repository.zones,
        repository.progress,
        repository.announcements,
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

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    fun toggleReaction(postId: Long, emoji: String) {
        viewModelScope.launch { repository.toggleReaction(postId, emoji) }
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
