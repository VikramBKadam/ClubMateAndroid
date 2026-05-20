package com.clubmates.app.domain.model

import java.util.UUID

data class UserProfile(
    val id: String,
    val name: String,
    val age: Int,
    val bio: String,
    val jobTitle: String,
    val company: String,
    val height: String,
    val interests: List<String>,
    val prompts: List<Prompt>,
    val photos: List<ProfilePhoto>,
    val isVerified: Boolean,
    val distance: Double = 0.0,
    val hasSuperLikedMe: Boolean = false
) {
    data class Prompt(
        val id: String = UUID.randomUUID().toString(),
        val question: String,
        val answer: String
    )
}

data class ProfilePhoto(
    val id: String = UUID.randomUUID().toString(),
    val cdnUrl: String? = null,
    val position: Int = 0
)

data class Venue(
    val id: String,
    val name: String,
    val neighborhood: String,
    val vibe: String,
    val music: String,
    val symbolName: String,
    val gradientHex: List<String>,
    val occupancy: Int = 0
)

data class Match(
    val id: String,
    val profile: UserProfile,
    val venueId: String?,
    val lastMessage: String,
    val lastMessageAt: String,
    val unreadCount: Int,
    val isNew: Boolean
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val matchId: String,
    val body: String,
    val isFromMe: Boolean,
    val senderId: String,
    val isRead: Boolean,
    val createdAt: String,
    val tempId: String? = null
)

enum class SwipeDirection { LIKE, NOPE, SUPERLIKE }
