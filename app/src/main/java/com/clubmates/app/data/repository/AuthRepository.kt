package com.clubmates.app.data.repository

import com.clubmates.app.BuildConfig
import com.clubmates.app.data.local.TokenStore
import com.clubmates.app.data.network.*
import com.clubmates.app.domain.model.ProfilePhoto
import com.clubmates.app.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore
) {
    suspend fun sendOtp(phone: String): SendOtpResponse {
        if (BuildConfig.MOCK_MODE) return SendOtpResponse(ok = true, devCode = "123456")
        return api.sendOtp(SendOtpRequest(phone))
    }

    suspend fun verifyOtp(phone: String, code: String): AuthResponse {
        if (BuildConfig.MOCK_MODE) {
            tokenStore.accessToken = "mock_access_token"
            tokenStore.refreshToken = "mock_refresh_token"
            tokenStore.userId = "mock_user_1"
            return AuthResponse(
                accessToken = "mock_access_token",
                refreshToken = "mock_refresh_token",
                user = AuthUser(id = "mock_user_1", phone = phone, profileCompleted = true),
                isNewUser = false
            )
        }
        val response = api.verifyOtp(VerifyOtpRequest(phone, code))
        tokenStore.accessToken = response.accessToken
        tokenStore.refreshToken = response.refreshToken
        tokenStore.userId = response.user.id
        return response
    }

    suspend fun getMe(): UserProfile {
        if (BuildConfig.MOCK_MODE) return mockProfile
        return api.getMe().user.toDomain()
    }

    suspend fun updateProfile(profile: UserProfile): UserProfile {
        if (BuildConfig.MOCK_MODE) return profile
        val request = UpdateProfileRequest(
            name = profile.name,
            age = profile.age,
            bio = profile.bio,
            jobTitle = profile.jobTitle,
            company = profile.company,
            height = profile.height,
            interests = profile.interests,
            prompts = profile.prompts.map { ApiPrompt(question = it.question, answer = it.answer) }
        )
        return api.updateMe(request).user.toDomain()
    }

    suspend fun registerDeviceToken(token: String) {
        if (BuildConfig.MOCK_MODE) return
        api.registerDeviceToken(DeviceTokenRequest(token))
    }

    suspend fun logout() {
        if (BuildConfig.MOCK_MODE) { tokenStore.clear(); return }
        val refreshToken = tokenStore.refreshToken
        if (refreshToken != null) {
            runCatching { api.logout(LogoutRequest(refreshToken)) }
        }
        tokenStore.clear()
    }

    val isLoggedIn: Boolean get() = tokenStore.hasTokens
    val userId: String? get() = tokenStore.userId
}

val mockProfile = UserProfile(
    id = "mock_user_1",
    name = "Vikram",
    age = 26,
    bio = "Love good music, great venues, and even better people.",
    jobTitle = "Product Designer",
    company = "ClubMates",
    height = "5'11\"",
    interests = listOf("Music", "Dancing", "Travel", "Coffee", "Photography"),
    prompts = listOf(
        UserProfile.Prompt(question = "My go-to karaoke song", answer = "Bohemian Rhapsody, every time."),
        UserProfile.Prompt(question = "Best way to find me", answer = "At the bar, debating cocktail menus.")
    ),
    photos = listOf(ProfilePhoto(cdnUrl = null, position = 0)),
    isVerified = true
)
