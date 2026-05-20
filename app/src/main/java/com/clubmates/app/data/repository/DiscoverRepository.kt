package com.clubmates.app.data.repository

import com.clubmates.app.BuildConfig
import com.clubmates.app.data.network.ApiService
import com.clubmates.app.data.network.SwipeRequest
import com.clubmates.app.data.network.SwipeResponse
import com.clubmates.app.domain.model.ProfilePhoto
import com.clubmates.app.domain.model.SwipeDirection
import com.clubmates.app.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

val mockDiscoverProfiles = listOf(
    UserProfile(id = "u2", name = "Priya", age = 24, bio = "Sunset chaser. Cocktail enthusiast. Will judge your playlist.", jobTitle = "UX Designer", company = "Zomato", height = "5'5\"", interests = listOf("Art", "Travel", "Cocktails", "Photography"), prompts = listOf(UserProfile.Prompt(question = "Ideal Saturday night", answer = "Rooftop bar with good views and better company.")), photos = listOf(ProfilePhoto(cdnUrl = null, position = 0)), isVerified = true, distance = 0.2),
    UserProfile(id = "u3", name = "Arjun", age = 27, bio = "Gym bro by day, jazz lover by night.", jobTitle = "Software Engineer", company = "Swiggy", height = "6'1\"", interests = listOf("Fitness", "Music", "Gaming", "Coffee"), prompts = listOf(UserProfile.Prompt(question = "Change my mind", answer = "Filter coffee > espresso.")), photos = listOf(ProfilePhoto(cdnUrl = null, position = 0)), isVerified = false, distance = 0.5),
    UserProfile(id = "u4", name = "Sneha", age = 25, bio = "Dancer, foodie, and professional overthinker.", jobTitle = "Marketing Lead", company = "Razorpay", height = "5'4\"", interests = listOf("Dancing", "Food", "Yoga", "Books"), prompts = listOf(UserProfile.Prompt(question = "My love language", answer = "Recommending restaurants nobody has heard of.")), photos = listOf(ProfilePhoto(cdnUrl = null, position = 0)), isVerified = true, distance = 0.8),
    UserProfile(id = "u5", name = "Rohan", age = 28, bio = "Startup founder. Coffee addict. Terrible dancer but I try.", jobTitle = "Founder", company = "Stealth", height = "5'10\"", interests = listOf("Tech", "Coffee", "Running", "Podcasts"), prompts = listOf(UserProfile.Prompt(question = "Two truths one lie", answer = "I've been to 12 countries. I speak 3 languages. I like mornings.")), photos = listOf(ProfilePhoto(cdnUrl = null, position = 0)), isVerified = false, distance = 1.1),
    UserProfile(id = "u6", name = "Ananya", age = 23, bio = "Film buff. Bookworm. Always at the wrong bar at the right time.", jobTitle = "Content Writer", company = "Notion", height = "5'6\"", interests = listOf("Movies", "Books", "Comedy", "Wine"), prompts = listOf(UserProfile.Prompt(question = "We'll get along if", answer = "You think Wes Anderson is a vibe, not just a filmmaker.")), photos = listOf(ProfilePhoto(cdnUrl = null, position = 0)), isVerified = true, distance = 0.3),
)

@Singleton
class DiscoverRepository @Inject constructor(private val api: ApiService) {

    suspend fun discover(venueId: String): List<UserProfile> {
        if (BuildConfig.MOCK_MODE) return mockDiscoverProfiles
        return api.discover(venueId).profiles.map { it.toDomain() }
    }

    suspend fun swipe(targetUserId: String, venueId: String, direction: SwipeDirection): SwipeResponse {
        if (BuildConfig.MOCK_MODE) {
            val isMatch = direction == SwipeDirection.LIKE && targetUserId == "u2"
            return SwipeResponse(isMatch = isMatch, matchId = if (isMatch) "match_1" else null)
        }
        val dir = when (direction) {
            SwipeDirection.LIKE -> "like"
            SwipeDirection.NOPE -> "nope"
            SwipeDirection.SUPERLIKE -> "superlike"
        }
        return api.swipe(SwipeRequest(targetUserId, venueId, dir))
    }

    suspend fun undoSwipe(targetUserId: String, venueId: String) {
        if (BuildConfig.MOCK_MODE) return
        api.undoSwipe(targetUserId, venueId)
    }
}
