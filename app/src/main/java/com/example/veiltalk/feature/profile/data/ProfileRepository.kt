package com.example.veiltalk.feature.profile.data

import android.content.Context
import android.net.Uri
import com.example.veiltalk.common.util.uriToMultipart
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.feature.profile.data.dto.ProfileUpdateRequestDto
import com.example.veiltalk.feature.profile.data.dto.UserProfileResponseDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val api: ProfileApi,
    private val sessionManager: SessionManager,
    @ApplicationContext private val appContext: Context
) {
    suspend fun getMyProfile(): Result<UserProfileResponseDto> {
        return try {
            val response = api.getMyProfile()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("خطا در دریافت پروفایل"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(firstName: String, lastName: String, bio: String): Result<UserProfileResponseDto> {
        return try {
            val response = api.updateMyProfile(ProfileUpdateRequestDto(firstName, lastName, bio))
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                persistDisplayName(updated)
                Result.success(updated)
            } else {
                Result.failure(Exception("خطا در بروزرسانی پروفایل"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAvatar(uri: Uri): Result<UserProfileResponseDto> {
        val part = uriToMultipart(appContext, uri) ?: return Result.failure(Exception("فایل نامعتبر است."))
        return try {
            val response = api.uploadAvatar(part)
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                persistDisplayName(updated)
                Result.success(updated)
            } else {
                Result.failure(Exception("خطا در آپلود عکس پروفایل"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // معادل handleProfileUpdated در App.tsx — نام نمایشی در سراسر اپ رو هم‌زمان به‌روز نگه می‌داره
    private suspend fun persistDisplayName(profile: UserProfileResponseDto) {
        val displayName = "${profile.firstName} ${profile.lastName}".trim()
        sessionManager.updateDisplayName(displayName)
    }
}