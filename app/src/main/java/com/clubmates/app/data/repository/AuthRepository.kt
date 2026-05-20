package com.clubmates.app.data.repository

import com.clubmates.app.data.local.TokenStore
import com.clubmates.app.data.network.*
import com.clubmates.app.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore
) {
    suspend fun sendOtp(phone: String): SendOtpResponse =
        api.sendOtp(SendOtpRequest(phone))

    suspend fun verifyOtp(phone: String, code: String): AuthResponse {
        val response = api.verifyOtp(VerifyOtpRequest(phone, code))
        tokenStore.accessToken = response.accessToken
        tokenStore.refreshToken = response.refreshToken
        tokenStore.userId = response.user.id
        return response
    }

    suspend fun getMe(): UserProfile = api.getMe().user.toDomain()

    suspend fun updateProfile(profile: UserProfile): UserProfile {
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

    suspend fun registerDeviceToken(token: String) =
        api.registerDeviceToken(DeviceTokenRequest(token))

    suspend fun logout() {
        val refreshToken = tokenStore.refreshToken
        if (refreshToken != null) {
            runCatching { api.logout(LogoutRequest(refreshToken)) }
        }
        tokenStore.clear()
    }

    val isLoggedIn: Boolean get() = tokenStore.hasTokens
    val userId: String? get() = tokenStore.userId
}
