package com.clubmates.app.data.network

import com.clubmates.app.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Auth ────────────────────────────────────────────────────────────────────

@Serializable
data class SendOtpRequest(val phone: String)

@Serializable
data class SendOtpResponse(val ok: Boolean, val devCode: String? = null)

@Serializable
data class VerifyOtpRequest(val phone: String, val code: String)

@Serializable
data class AuthUser(val id: String, val phone: String, val profileCompleted: Boolean)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUser,
    val isNewUser: Boolean? = null
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String)

// ── Profile ──────────────────────────────────────────────────────────────────

@Serializable
data class ApiPrompt(
    val id: String? = null,
    val question: String,
    val answer: String
)

@Serializable
data class ApiUserProfile(
    val id: String,
    val phone: String? = null,
    val name: String,
    val age: Int,
    val bio: String,
    @SerialName("jobTitle") val jobTitle: String = "",
    val company: String = "",
    val height: String = "",
    val interests: List<String> = emptyList(),
    val isVerified: Boolean = false,
    val profileCompleted: Boolean? = null,
    val prompts: List<ApiPrompt>? = null,
    val photoUrl: String? = null,
    val distance: Double? = null,
    val hasSuperLikedMe: Boolean? = null
) {
    fun toDomain() = UserProfile(
        id = id,
        name = name.ifBlank { "New ClubMate" },
        age = age,
        bio = bio,
        jobTitle = jobTitle,
        company = company,
        height = height,
        interests = interests,
        prompts = prompts?.map { UserProfile.Prompt(id = it.id ?: "", question = it.question, answer = it.answer) } ?: emptyList(),
        photos = if (photoUrl != null) listOf(ProfilePhoto(cdnUrl = photoUrl, position = 0)) else emptyList(),
        isVerified = isVerified,
        distance = distance ?: 0.0,
        hasSuperLikedMe = hasSuperLikedMe ?: false
    )
}

@Serializable
data class UserResponse(val user: ApiUserProfile)

@Serializable
data class UpdateProfileRequest(
    val name: String,
    val age: Int,
    val bio: String,
    val jobTitle: String,
    val company: String,
    val height: String,
    val interests: List<String>,
    val prompts: List<ApiPrompt>
)

@Serializable
data class DeviceTokenRequest(val token: String)

// ── Venues ───────────────────────────────────────────────────────────────────

@Serializable
data class ApiVenue(
    val id: String,
    val name: String,
    val neighborhood: String,
    val vibe: String = "",
    val music: String = "",
    val symbolName: String = "",
    val gradientHex: List<String> = emptyList(),
    val occupancy: Int = 0
) {
    fun toDomain() = Venue(
        id = id, name = name, neighborhood = neighborhood,
        vibe = vibe, music = music, symbolName = symbolName,
        gradientHex = gradientHex, occupancy = occupancy
    )
}

@Serializable
data class VenuesResponse(val venues: List<ApiVenue>)

@Serializable
data class RosterResponse(val roster: List<ApiUserProfile>)

// ── Discovery ────────────────────────────────────────────────────────────────

@Serializable
data class DiscoverResponse(val profiles: List<ApiUserProfile>)

@Serializable
data class SwipeRequest(
    val targetUserId: String,
    val venueId: String,
    val direction: String
)

@Serializable
data class SwipeResponse(val isMatch: Boolean, val matchId: String? = null)

// ── Matches & Chat ───────────────────────────────────────────────────────────

@Serializable
data class ApiMatch(
    val id: String,
    val venueId: String? = null,
    val profile: ApiUserProfile,
    val lastMessage: String = "",
    val lastMessageAt: String = "",
    val unreadCount: Int = 0,
    val isNew: Boolean = false
) {
    fun toDomain() = Match(
        id = id, profile = profile.toDomain(), venueId = venueId,
        lastMessage = lastMessage, lastMessageAt = lastMessageAt,
        unreadCount = unreadCount, isNew = isNew
    )
}

@Serializable
data class MatchesResponse(val matches: List<ApiMatch>)

@Serializable
data class ApiMessage(
    val id: String,
    val matchId: String,
    val body: String,
    val isFromMe: Boolean,
    val senderId: String,
    val isRead: Boolean,
    val createdAt: String
) {
    fun toDomain() = ChatMessage(
        id = id, matchId = matchId, body = body,
        isFromMe = isFromMe, senderId = senderId,
        isRead = isRead, createdAt = createdAt
    )
}

@Serializable
data class MessagesResponse(val messages: List<ApiMessage>)

@Serializable
data class MessageResponse(val message: ApiMessage)

@Serializable
data class SendMessageRequest(val body: String)

@Serializable
data class MarkReadRequest(val upToMessageId: String? = null)

// ── Safety ───────────────────────────────────────────────────────────────────

@Serializable
data class BlockRequest(val blockedUserId: String)

@Serializable
data class ReportRequest(
    val reportedUserId: String,
    val reason: String,
    val details: String? = null
)

// ── WebSocket ────────────────────────────────────────────────────────────────

@Serializable
data class WsEnvelope(val type: String)

@Serializable
data class WsMessageNew(
    val type: String,
    val matchId: String,
    val message: ApiMessage
)

@Serializable
data class WsMessageAck(
    val type: String,
    val tempId: String,
    val serverId: String,
    val createdAt: String
)

@Serializable
data class WsTyping(val type: String, val matchId: String, val userId: String)

@Serializable
data class WsMatchCreated(val type: String, val match: ApiMatch)

@Serializable
data class WsPresence(
    val type: String,
    val venueId: String,
    val userId: String,
    val name: String? = null,
    val photoUrl: String? = null
)

@Serializable
data class WsSend(
    val type: String,
    val matchId: String? = null,
    val tempId: String? = null,
    val body: String? = null,
    val upToMessageId: String? = null,
    val venueId: String? = null
)
