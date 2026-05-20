package com.clubmates.app.data.repository

import com.clubmates.app.data.network.ApiService
import com.clubmates.app.domain.model.UserProfile
import com.clubmates.app.domain.model.Venue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VenueRepository @Inject constructor(private val api: ApiService) {

    suspend fun getVenues(): List<Venue> = api.getVenues().venues.map { it.toDomain() }

    suspend fun getRoster(venueId: String): List<UserProfile> =
        api.getRoster(venueId).roster.map { it.toDomain() }

    suspend fun checkIn(venueId: String) = api.checkIn(venueId)

    suspend fun checkOut(venueId: String) = api.checkOut(venueId)
}
