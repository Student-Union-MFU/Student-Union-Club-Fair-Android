package com.su.clubfair.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.su.clubfair.ClubFairApplication
import com.su.clubfair.data.FairRepository
import com.su.clubfair.data.ScanOutcome
import com.su.clubfair.ui.model.Announcement
import com.su.clubfair.ui.model.Booth
import com.su.clubfair.ui.model.Student
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Whether there is a signed-in student, once that is actually known.
 *
 * [Restoring] is not a nicety. Reading DataStore is asynchronous, so on a cold
 * start there is a frame — sometimes several — where the app knows nothing about
 * the session. Treating "not yet known" as "signed out" is what makes a
 * persisted login flash the Welcome screen on every launch before snapping to
 * Home, which reads as a crash-and-recover. The shell waits on this instead.
 */
sealed interface SessionState {
    data object Restoring : SessionState
    data object SignedOut : SessionState

    /**
     * [onboardingSeen] rides along rather than being its own flow, so the shell
     * never has to render one of the two answers while waiting for the other —
     * which is how a returning student gets a flash of the welcome card.
     */
    data class SignedIn(
        val student: Student,
        val onboardingSeen: Boolean,
    ) : SessionState
}

/** Everything the signed-in shell renders, as one snapshot. */
data class FairUiState(
    val booths: List<Booth> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val hapticsEnabled: Boolean = true,
    val unsyncedScans: Int = 0,
)

/**
 * The signed-in half of the app.
 *
 * A ViewModel rather than screen-level `remember`, for the reason the Events tab
 * demonstrated best: its feed lived in a `mutableStateListOf` inside the
 * composable, so every reaction a student left was discarded when they rotated
 * the phone. State that outlives a composition has to live somewhere that
 * outlives a composition.
 */
class FairViewModel(private val repository: FairRepository) : ViewModel() {

    val session: StateFlow<SessionState> =
        combine(repository.student, repository.onboardingSeen) { student, seen ->
            if (student == null) {
                SessionState.SignedOut
            } else {
                SessionState.SignedIn(student, onboardingSeen = seen)
            }
        }.stateIn(
            scope = viewModelScope,
            // Keeps the flow warm across a configuration change so a rotation
            // does not drop back to Restoring and replay the entry animation.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionState.Restoring,
        )

    val uiState: StateFlow<FairUiState> = combine(
        repository.booths,
        repository.announcements,
        repository.hapticsEnabled,
        repository.unsyncedScans,
    ) { booths, announcements, haptics, unsynced ->
        FairUiState(
            booths = booths,
            announcements = announcements,
            hapticsEnabled = haptics,
            unsyncedScans = unsynced.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FairUiState(),
    )

    /**
     * The most recent scan result, for the card at the bottom of the Scan tab.
     *
     * Held here rather than in the screen so that walking away to Home and back
     * does not lose what the camera just read.
     */
    private val _lastScan = MutableStateFlow<ScanOutcome?>(null)
    val lastScan: StateFlow<ScanOutcome?> = _lastScan.asStateFlow()

    fun onScanned(payload: String) {
        // Ignore repeat decodes of the code already on screen. The analyzer fires
        // per frame, so without this a code held in front of the lens produces
        // thirty identical writes a second.
        if (_lastScan.value != null) return
        viewModelScope.launch {
            _lastScan.value = repository.recordScan(payload)
        }
    }

    fun clearScan() {
        _lastScan.value = null
    }

    fun toggleReaction(postId: Int, emoji: String) {
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
