package com.clubmates.app.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubmates.app.data.network.WebSocketClient
import com.clubmates.app.data.network.WsEvent
import com.clubmates.app.data.network.WsSend
import com.clubmates.app.data.repository.DiscoverRepository
import com.clubmates.app.data.repository.VenueRepository
import com.clubmates.app.domain.model.Match
import com.clubmates.app.domain.model.SwipeDirection
import com.clubmates.app.domain.model.UserProfile
import com.clubmates.app.domain.model.Venue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DiscoverUiState(
    val venues: List<Venue> = emptyList(),
    val activeVenue: Venue? = null,
    val profiles: List<UserProfile> = emptyList(),
    val roster: List<UserProfile> = emptyList(),
    val canUndo: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val matchResult: MatchResult? = null,
    val venueEvent: String = ""
) {
    val isCheckedIn: Boolean get() = activeVenue != null
}

data class MatchResult(val profile: UserProfile, val matchId: String)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val venueRepository: VenueRepository,
    private val discoverRepository: DiscoverRepository,
    private val wsClient: WebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val swipeHistory = mutableListOf<UserProfile>()
    private val swipedIds = mutableSetOf<String>()
    private var heartbeatJob: Job? = null

    init {
        loadVenues()
        observeWsEvents()
    }

    fun loadVenues() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { venueRepository.getVenues() }
                .onSuccess { venues -> _uiState.update { it.copy(venues = venues, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun checkIn(venue: Venue) {
        viewModelScope.launch {
            runCatching { venueRepository.checkIn(venue.id) }
            _uiState.update { it.copy(activeVenue = venue, venueEvent = "Checked in at ${venue.name}") }
            clearVenueEventAfterDelay()
            loadRosterAndQueue(venue.id)
            startHeartbeat(venue.id)
        }
    }

    fun checkOut() {
        val venue = _uiState.value.activeVenue ?: return
        viewModelScope.launch {
            runCatching { venueRepository.checkOut(venue.id) }
            heartbeatJob?.cancel()
            swipeHistory.clear()
            swipedIds.clear()
            _uiState.update { it.copy(activeVenue = null, profiles = emptyList(), roster = emptyList(), canUndo = false, venueEvent = "You checked out") }
            clearVenueEventAfterDelay()
            loadVenues()
        }
    }

    fun swipe(profile: UserProfile, direction: SwipeDirection) {
        val venue = _uiState.value.activeVenue ?: return
        swipedIds.add(profile.id)
        swipeHistory.add(profile)
        _uiState.update { state ->
            state.copy(
                profiles = state.profiles.filter { it.id != profile.id },
                canUndo = swipeHistory.isNotEmpty()
            )
        }
        if (_uiState.value.profiles.size < 3) loadMoreProfiles(venue.id)

        viewModelScope.launch {
            runCatching { discoverRepository.swipe(profile.id, venue.id, direction) }
                .onSuccess { response ->
                    if (response.isMatch && response.matchId != null) {
                        _uiState.update { it.copy(matchResult = MatchResult(profile, response.matchId)) }
                    }
                }
        }
    }

    fun undo() {
        val venue = _uiState.value.activeVenue ?: return
        val restored = swipeHistory.removeLastOrNull() ?: return
        swipedIds.remove(restored.id)
        _uiState.update { state ->
            state.copy(
                profiles = listOf(restored) + state.profiles,
                canUndo = swipeHistory.isNotEmpty()
            )
        }
        viewModelScope.launch {
            runCatching { discoverRepository.undoSwipe(restored.id, venue.id) }
        }
    }

    fun dismissMatch() = _uiState.update { it.copy(matchResult = null) }

    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun loadRosterAndQueue(venueId: String) {
        viewModelScope.launch {
            val roster = runCatching { venueRepository.getRoster(venueId) }.getOrElse { emptyList() }
            val queue = runCatching { discoverRepository.discover(venueId) }.getOrElse { emptyList() }
            _uiState.update { it.copy(roster = roster, profiles = queue) }
        }
    }

    private fun loadMoreProfiles(venueId: String) {
        viewModelScope.launch {
            runCatching { discoverRepository.discover(venueId) }
                .onSuccess { newProfiles ->
                    val existing = _uiState.value.profiles.map { it.id }.toSet()
                    val fresh = newProfiles.filter { it.id !in existing && it.id !in swipedIds }
                    if (fresh.isNotEmpty()) {
                        _uiState.update { it.copy(profiles = it.profiles + fresh) }
                    }
                }
        }
    }

    private fun startHeartbeat(venueId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (isActive) {
                delay(90_000L)
                wsClient.send(WsSend(type = "presence.ping", venueId = venueId))
            }
        }
    }

    private fun observeWsEvents() {
        viewModelScope.launch {
            wsClient.events.collect { event ->
                when (event) {
                    is WsEvent.PresenceJoin -> {
                        val venueId = _uiState.value.activeVenue?.id ?: return@collect
                        if (event.venueId == venueId) loadRosterAndQueue(venueId)
                    }
                    is WsEvent.PresenceLeave -> {
                        _uiState.update { state ->
                            state.copy(roster = state.roster.filter { it.id != event.userId })
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun clearVenueEventAfterDelay() {
        viewModelScope.launch {
            delay(3_000)
            _uiState.update { it.copy(venueEvent = "") }
        }
    }
}
