package com.clubmates.app.data.repository

import com.clubmates.app.data.network.ApiService
import com.clubmates.app.data.network.MarkReadRequest
import com.clubmates.app.data.network.SendMessageRequest
import com.clubmates.app.domain.model.ChatMessage
import com.clubmates.app.domain.model.Match
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchesRepository @Inject constructor(private val api: ApiService) {

    suspend fun getMatches(): List<Match> =
        api.getMatches().matches.map { it.toDomain() }

    suspend fun getMessages(matchId: String): List<ChatMessage> =
        api.getMessages(matchId).messages.map { it.toDomain() }

    suspend fun sendMessage(matchId: String, body: String): ChatMessage =
        api.sendMessage(matchId, SendMessageRequest(body)).message.toDomain()

    suspend fun markRead(matchId: String, upToMessageId: String? = null) =
        api.markRead(matchId, MarkReadRequest(upToMessageId))

    suspend fun unmatch(matchId: String) = api.unmatch(matchId)
}
