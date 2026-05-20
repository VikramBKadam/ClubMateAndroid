package com.clubmates.app.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubmates.app.data.network.WebSocketClient
import com.clubmates.app.data.network.WsEvent
import com.clubmates.app.data.network.WsSend
import com.clubmates.app.data.repository.MatchesRepository
import com.clubmates.app.domain.model.ChatMessage
import com.clubmates.app.domain.model.Match
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class MatchesUiState(
    val matches: List<Match> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ChatUiState(
    val match: Match? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class MatchesViewModel @Inject constructor(
    private val matchesRepository: MatchesRepository,
    private val wsClient: WebSocketClient
) : ViewModel() {

    private val _matchesState = MutableStateFlow(MatchesUiState())
    val matchesState: StateFlow<MatchesUiState> = _matchesState.asStateFlow()

    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private val messageThreads = mutableMapOf<String, MutableList<ChatMessage>>()

    val totalUnread: Int get() = _matchesState.value.matches.sumOf { it.unreadCount }

    init {
        loadMatches()
        observeWsEvents()
    }

    fun loadMatches() {
        viewModelScope.launch {
            _matchesState.update { it.copy(isLoading = true) }
            runCatching { matchesRepository.getMatches() }
                .onSuccess { matches -> _matchesState.update { it.copy(matches = matches, isLoading = false) } }
                .onFailure { e -> _matchesState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun openChat(match: Match) {
        _chatState.update { it.copy(match = match, messages = messageThreads[match.id] ?: emptyList(), isLoading = true) }
        viewModelScope.launch {
            runCatching { matchesRepository.getMessages(match.id) }
                .onSuccess { messages ->
                    messageThreads[match.id] = messages.toMutableList()
                    _chatState.update { it.copy(messages = messages, isLoading = false) }
                    matchesRepository.markRead(match.id)
                    _matchesState.update { state ->
                        state.copy(matches = state.matches.map { m ->
                            if (m.id == match.id) m.copy(unreadCount = 0, isNew = false) else m
                        })
                    }
                }
                .onFailure { _chatState.update { it.copy(isLoading = false) } }
        }
    }

    fun closeChat() = _chatState.update { ChatUiState() }

    fun sendMessage(body: String) {
        val match = _chatState.value.match ?: return
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return

        val tempId = UUID.randomUUID().toString()
        val optimistic = ChatMessage(
            id = tempId, matchId = match.id, body = trimmed,
            isFromMe = true, senderId = "", isRead = false,
            createdAt = "", tempId = tempId
        )
        appendMessage(match.id, optimistic)

        viewModelScope.launch {
            runCatching { matchesRepository.sendMessage(match.id, trimmed) }
                .onSuccess { sent ->
                    val thread = messageThreads[match.id] ?: return@onSuccess
                    val idx = thread.indexOfFirst { it.tempId == tempId }
                    if (idx >= 0) thread[idx] = sent
                    else thread.add(sent)
                    if (_chatState.value.match?.id == match.id) {
                        _chatState.update { it.copy(messages = thread.toList()) }
                    }
                    updateLastMessage(match.id, trimmed)
                }
        }
    }

    fun sendTypingStart() {
        val matchId = _chatState.value.match?.id ?: return
        wsClient.send(WsSend(type = "typing.start", matchId = matchId))
    }

    fun sendTypingStop() {
        val matchId = _chatState.value.match?.id ?: return
        wsClient.send(WsSend(type = "typing.stop", matchId = matchId))
    }

    fun unmatch(matchId: String) {
        viewModelScope.launch {
            runCatching { matchesRepository.unmatch(matchId) }
            _matchesState.update { state -> state.copy(matches = state.matches.filter { it.id != matchId }) }
            if (_chatState.value.match?.id == matchId) closeChat()
        }
    }

    fun addMatch(match: Match) {
        _matchesState.update { state ->
            if (state.matches.any { it.id == match.id }) state
            else state.copy(matches = listOf(match) + state.matches)
        }
        messageThreads.getOrPut(match.id) { mutableListOf() }
    }

    fun clearError() = _matchesState.update { it.copy(error = null) }

    private fun observeWsEvents() {
        viewModelScope.launch {
            wsClient.events.collect { event ->
                when (event) {
                    is WsEvent.MessageNew -> {
                        val msg = event.message.toDomain()
                        appendMessage(event.matchId, msg)
                        if (_chatState.value.match?.id == event.matchId) {
                            matchesRepository.markRead(event.matchId, msg.id)
                        } else {
                            incrementUnread(event.matchId)
                        }
                        updateLastMessage(event.matchId, msg.body)
                    }
                    is WsEvent.MessageAck -> {
                        val matchId = _chatState.value.match?.id ?: return@collect
                        val thread = messageThreads[matchId] ?: return@collect
                        val idx = thread.indexOfFirst { it.tempId == event.tempId }
                        if (idx >= 0) {
                            thread[idx] = thread[idx].copy(id = event.serverId, createdAt = event.createdAt, tempId = null)
                            _chatState.update { it.copy(messages = thread.toList()) }
                        }
                    }
                    is WsEvent.TypingStart -> {
                        if (_chatState.value.match?.id == event.matchId) {
                            _chatState.update { it.copy(isTyping = true) }
                        }
                    }
                    is WsEvent.TypingStop -> {
                        if (_chatState.value.match?.id == event.matchId) {
                            _chatState.update { it.copy(isTyping = false) }
                        }
                    }
                    is WsEvent.MatchCreated -> addMatch(event.match.toDomain())
                    else -> Unit
                }
            }
        }
    }

    private fun appendMessage(matchId: String, message: ChatMessage) {
        val thread = messageThreads.getOrPut(matchId) { mutableListOf() }
        thread.add(message)
        if (_chatState.value.match?.id == matchId) {
            _chatState.update { it.copy(messages = thread.toList()) }
        }
    }

    private fun updateLastMessage(matchId: String, text: String) {
        _matchesState.update { state ->
            state.copy(matches = state.matches.map { m ->
                if (m.id == matchId) m.copy(lastMessage = text) else m
            })
        }
    }

    private fun incrementUnread(matchId: String) {
        _matchesState.update { state ->
            state.copy(matches = state.matches.map { m ->
                if (m.id == matchId) m.copy(unreadCount = m.unreadCount + 1) else m
            })
        }
    }
}
