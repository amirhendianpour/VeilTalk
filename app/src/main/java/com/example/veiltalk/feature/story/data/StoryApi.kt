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
}
