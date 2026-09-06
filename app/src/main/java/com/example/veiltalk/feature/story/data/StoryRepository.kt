package com.example.veiltalk.feature.story.data

import android.net.Uri
import com.example.veiltalk.common.util.ApiResult
import com.example.veiltalk.common.util.safeApiCall
import com.example.veiltalk.feature.chat.data.MediaRepository
import com.example.veiltalk.feature.story.data.dto.PostStoryRequest
import com.example.veiltalk.feature.story.data.dto.StoryResponseDto
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepository @Inject constructor(
    private val api: StoryApi,
    private val mediaRepository: MediaRepository,
    private val json: Json
) {
    suspend fun getStories(): ApiResult<List<StoryResponseDto>> = safeApiCall(json) {
        api.getStories()
    }

    suspend fun postStory(uri: Uri, caption: String): ApiResult<String> {
        // 1. Upload the file first
        val uploadResult = mediaRepository.uploadFile(uri, encrypt = false) // Stories are public/contacts, usually not encrypted the same way
        if (uploadResult.isFailure) return ApiResult.Error(uploadResult.exceptionOrNull()?.message ?: "Upload failed")
        
        val fileUrl = uploadResult.getOrNull()?.fileUrl ?: return ApiResult.Error("No file URL")
        
        // 2. Post metadata to server
        return safeApiCall(json) {
            api.postStory(PostStoryRequest(fileUrl, caption, "IMAGE"))
        }.let { result ->
            when (result) {
                is ApiResult.Success -> ApiResult.Success(result.data["message"] ?: "Success")
                is ApiResult.Error -> ApiResult.Error(result.message)
            }
        }
    }
}
