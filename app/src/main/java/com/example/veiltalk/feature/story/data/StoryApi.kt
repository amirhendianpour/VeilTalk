package com.example.veiltalk.feature.story.data

import com.example.veiltalk.feature.story.data.dto.PostStoryRequest
import com.example.veiltalk.feature.story.data.dto.StoryResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface StoryApi {
    @GET("api/stories")
    suspend fun getStories(): Response<List<StoryResponseDto>>

    @POST("api/stories")
    suspend fun postStory(@Body request: PostStoryRequest): Response<Map<String, String>>

    @POST("api/stories/{storyId}/view")
    suspend fun viewStory(@retrofit2.http.Path("storyId") storyId: Long): Response<Map<String, String>>

    @POST("api/stories/{storyId}/react")
    suspend fun reactStory(
        @retrofit2.http.Path("storyId") storyId: Long,
        @retrofit2.http.Query("emoji") emoji: String
    ): Response<Map<String, String>>

    @GET("api/stories/{storyId}/viewers")
    suspend fun getStoryViewers(@retrofit2.http.Path("storyId") storyId: Long): Response<List<com.example.veiltalk.feature.story.data.dto.StoryViewerInfoDto>>
}
