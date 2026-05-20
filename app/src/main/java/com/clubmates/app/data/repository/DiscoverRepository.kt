package com.clubmates.app.data.repository

import com.clubmates.app.data.network.ApiService
import com.clubmates.app.data.network.SwipeRequest
import com.clubmates.app.data.network.SwipeResponse
import com.clubmates.app.domain.model.SwipeDirection
import com.clubmates.app.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscoverRepository @Inject constructor(private val api: ApiService) {

    suspend fun discover(venueId: String): List<UserProfile> =
        api.discover(venueId).profiles.map { it.toDomain() }

    suspend fun swipe(targetUserId: String, venueId: String, direction: SwipeDirection): SwipeResponse {
        val dir = when (direction) {
            SwipeDirection.LIKE -> "like"
            SwipeDirection.NOPE -> "nope"
            SwipeDirection.SUPERLIKE -> "superlike"
        }
        return api.swipe(SwipeRequest(targetUserId, venueId, dir))
    }

    suspend fun undoSwipe(targetUserId: String, venueId: String) =
        api.undoSwipe(targetUserId, venueId)
}
