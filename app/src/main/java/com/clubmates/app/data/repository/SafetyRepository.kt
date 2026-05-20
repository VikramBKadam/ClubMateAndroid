package com.clubmates.app.data.repository

import com.clubmates.app.data.network.ApiService
import com.clubmates.app.data.network.BlockRequest
import com.clubmates.app.data.network.ReportRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafetyRepository @Inject constructor(private val api: ApiService) {

    suspend fun block(blockedUserId: String) = api.block(BlockRequest(blockedUserId))

    suspend fun report(reportedUserId: String, reason: String, details: String? = null) =
        api.report(ReportRequest(reportedUserId, reason, details))
}
