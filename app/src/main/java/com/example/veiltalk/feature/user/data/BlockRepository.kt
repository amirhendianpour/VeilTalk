package com.example.veiltalk.feature.user.data

import com.example.veiltalk.common.util.ApiResult
import com.example.veiltalk.common.util.safeApiCall
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockRepository @Inject constructor(
    private val api: BlockApi,
    private val json: Json
) {
    suspend fun blockUser(username: String): ApiResult<Map<String, String>> = safeApiCall(json) {
        api.blockUser(username)
    }

    suspend fun unblockUser(username: String): ApiResult<Map<String, String>> = safeApiCall(json) {
        api.unblockUser(username)
    }

    suspend fun getBlockedUsers(): ApiResult<List<String>> = safeApiCall(json) {
        api.getBlockedUsers()
    }

    suspend fun isBlocked(username: String): ApiResult<Map<String, Boolean>> = safeApiCall(json) {
        api.checkBlocked(username)
    }
}
