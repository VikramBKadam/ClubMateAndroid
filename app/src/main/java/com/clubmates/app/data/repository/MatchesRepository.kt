package com.clubmates.app.data.repository

import com.clubmates.app.BuildConfig
import com.clubmates.app.data.network.ApiService
import com.clubmates.app.data.network.MarkReadRequest
import com.clubmates.app.data.network.SendMessageRequest
import com.clubmates.app.domain.model.ChatMessage
import com.clubmates.app.domain.model.Match
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

private val mockMatches = listOf(
    Match(id = "match_1", profile = mockDiscoverProfiles[0], venueId = "v1", lastMessage = "Hey! Great meeting you at Harbour Social 👋", lastMessageAt = "2025-05-20T20:30:00Z", unreadCount = 2, isNew = true),
    Match(id = "match_2", profile = mockDiscoverProfiles[2], venueId = "v2", lastMessage = "We should grab coffee sometime!", lastMessageAt = "2025-05-19T18:00:00Z", unreadCount = 0, isNew = false),
)

private val mockMessages = mapOf(
    "match_1" to listOf(
        ChatMessage(id = "msg_1", matchId = "match_1", body = "Hey! Great meeting you at Harbour Social 👋", isFromMe = false, senderId = "u2", isRead = true, createdAt = "2025-05-20T20:28:00Z"),
        ChatMessage(id = "msg_2", matchId = "match_1", body = "You too! That set was 🔥", isFromMe = true, senderId = "mock_user_1", isRead = true, createdAt = "2025-05-20T20:29:00Z"),
        ChatMessage(id = "msg_3", matchId = "match_1", body = "Are you coming back next weekend?", isFromMe = false, senderId = "u2", isRead = false, createdAt = "2025-05-20T20:30:00Z"),
    ),
    "match_2" to listOf(
        ChatMessage(id = "msg_4", matchId = "match_2", body = "We should grab coffee sometime!", isFromMe = false, senderId = "u4", isRead = true, createdAt = "2025-05-19T18:00:00Z"),
    )
)

@Singleton
class MatchesRepository @Inject constructor(private val api: ApiService) {

    private val localMessages = mockMessages.mapValues { it.value.toMutableList() }.toMutableMap()

    suspend fun getMatches(): List<Match> {
        if (BuildConfig.MOCK_MODE) return mockMatches
        return api.getMatches().matches.map { it.toDomain() }
    }

    suspend fun getMessages(matchId: String): List<ChatMessage> {
        if (BuildConfig.MOCK_MODE) return localMessages[matchId] ?: emptyList()
        return api.getMessages(matchId).messages.map { it.toDomain() }
    }

    suspend fun sendMessage(matchId: String, body: String): ChatMessage {
        if (BuildConfig.MOCK_MODE) {
            val msg = ChatMessage(id = UUID.randomUUID().toString(), matchId = matchId, body = body, isFromMe = true, senderId = "mock_user_1", isRead = true, createdAt = "now")
            localMessages.getOrPut(matchId) { mutableListOf() }.add(msg)
            return msg
        }
        return api.sendMessage(matchId, SendMessageRequest(body)).message.toDomain()
    }

    suspend fun markRead(matchId: String, upToMessageId: String? = null) {
        if (BuildConfig.MOCK_MODE) return
        api.markRead(matchId, MarkReadRequest(upToMessageId))
    }

    suspend fun unmatch(matchId: String) {
        if (BuildConfig.MOCK_MODE) return
        api.unmatch(matchId)
    }
}
