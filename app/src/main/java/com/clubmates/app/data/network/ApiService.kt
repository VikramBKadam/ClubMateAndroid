package com.clubmates.app.data.network

import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("auth/otp/send")
    suspend fun sendOtp(@Body request: SendOtpRequest): SendOtpResponse

    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthResponse

    @DELETE("auth/session")
    suspend fun logout(@Body request: LogoutRequest)

    // Profile
    @GET("users/me")
    suspend fun getMe(): UserResponse

    @PUT("users/me")
    suspend fun updateMe(@Body request: UpdateProfileRequest): UserResponse

    @Multipart
    @POST("users/me/photos")
    suspend fun uploadPhoto(@Part photo: MultipartBody.Part): Map<String, String>

    @DELETE("users/me/photos/{photoId}")
    suspend fun deletePhoto(@Path("photoId") photoId: String)

    @POST("users/me/device-token")
    suspend fun registerDeviceToken(@Body request: DeviceTokenRequest)

    // Venues
    @GET("venues")
    suspend fun getVenues(): VenuesResponse

    @GET("venues/{id}/roster")
    suspend fun getRoster(@Path("id") venueId: String): RosterResponse

    @POST("venues/{id}/checkin")
    suspend fun checkIn(@Path("id") venueId: String)

    @DELETE("venues/{id}/checkin")
    suspend fun checkOut(@Path("id") venueId: String)

    // Discovery
    @GET("venues/{id}/discover")
    suspend fun discover(@Path("id") venueId: String): DiscoverResponse

    @POST("swipes")
    suspend fun swipe(@Body request: SwipeRequest): SwipeResponse

    @DELETE("swipes/{targetUserId}/{venueId}")
    suspend fun undoSwipe(
        @Path("targetUserId") targetUserId: String,
        @Path("venueId") venueId: String
    )

    // Matches & Chat
    @GET("matches")
    suspend fun getMatches(): MatchesResponse

    @GET("matches/{id}/messages")
    suspend fun getMessages(@Path("id") matchId: String): MessagesResponse

    @POST("matches/{id}/messages")
    suspend fun sendMessage(
        @Path("id") matchId: String,
        @Body request: SendMessageRequest
    ): MessageResponse

    @PUT("matches/{id}/messages/read")
    suspend fun markRead(
        @Path("id") matchId: String,
        @Body request: MarkReadRequest
    )

    @DELETE("matches/{id}")
    suspend fun unmatch(@Path("id") matchId: String)

    // Safety
    @POST("blocks")
    suspend fun block(@Body request: BlockRequest)

    @POST("reports")
    suspend fun report(@Body request: ReportRequest)
}
