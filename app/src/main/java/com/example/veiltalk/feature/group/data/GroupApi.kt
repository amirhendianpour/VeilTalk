package com.example.veiltalk.feature.group.data

import com.example.veiltalk.feature.group.data.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface GroupApi {

    @POST("api/groups/create")
    suspend fun createGroup(@Body request: CreateGroupRequestDto): Response<ChatGroupDto>

    @POST("api/groups/{groupId}/add-member")
    suspend fun addMember(
        @Path("groupId") groupId: Long,
        @Body request: AddMemberRequestDto
    ): Response<GroupMemberDto>

    @GET("api/groups/my-groups")
    suspend fun getMyGroups(): Response<List<GroupMemberDto>>

    @GET("api/groups/{groupId}")
    suspend fun getGroupById(@Path("groupId") groupId: Long): Response<ChatGroupDto>

    @GET("api/groups/{groupId}/members/info")
    suspend fun getGroupMembersInfo(@Path("groupId") groupId: Long): Response<List<GroupMemberInfoDto>>

    @PUT("api/groups/{groupId}/name")
    suspend fun updateGroupName(
        @Path("groupId") groupId: Long,
        @Body request: UpdateGroupNameRequestDto
    ): Response<ChatGroupDto>

    @Multipart
    @POST("api/groups/{groupId}/image")
    suspend fun uploadGroupImage(
        @Path("groupId") groupId: Long,
        @Part file: MultipartBody.Part
    ): Response<ChatGroupDto>

    @PUT("api/groups/{groupId}/members/{username}/role")
    suspend fun updateMemberRole(
        @Path("groupId") groupId: Long,
        @Path("username") username: String,
        @Body request: UpdateMemberRoleRequestDto
    ): Response<GroupMemberDto>

    @DELETE("api/groups/{groupId}/members/{username}")
    suspend fun removeMember(
        @Path("groupId") groupId: Long,
        @Path("username") username: String
    ): Response<Unit>

    @DELETE("api/groups/{groupId}")
    suspend fun deleteGroup(@Path("groupId") groupId: Long): Response<Unit>
}