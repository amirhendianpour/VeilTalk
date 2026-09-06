package com.example.veiltalk.feature.profile.data

import android.content.Context
import android.net.Uri
import com.example.veiltalk.common.util.uriToChatFilePart
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.feature.profile.data.dto.ProfileUpdateRequestDto
import com.example.veiltalk.feature.profile.data.dto.UserProfileResponseDto
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import com.example.veiltalk.feature.user.data.dto.UserInfoDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val api: ProfileApi,
    private val sessionManager: SessionManager,
    private val userDirectory: UserDirectoryRepository,
    private val database: com.example.veiltalk.core.database.AppDatabase,
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

    suspend fun updateProfile(
        firstName: String, 
        lastName: String, 
        bio: String,
        email: String? = null,
        phoneNumber: String? = null,
        username: String? = null
    ): Result<UserProfileResponseDto> {
        return try {
            val response = api.updateMyProfile(ProfileUpdateRequestDto(firstName, lastName, bio, email, phoneNumber, username))
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                syncWithDirectory(updated)
                persistDisplayName(updated)
                persistUsername(updated)
                Result.success(updated)
            } else {
                Result.failure(Exception("خطا در بروزرسانی پروفایل"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAvatar(uri: Uri): Result<UserProfileResponseDto> {
        val pickedFile = uriToChatFilePart(appContext, uri) ?: return Result.failure(Exception("فایل نامعتبر است."))
        return try {
            val response = api.uploadAvatar(pickedFile.part)
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                syncWithDirectory(updated)
                persistDisplayName(updated)
                persistUsername(updated)
                Result.success(updated)
            } else {
                Result.failure(Exception("خطا در آپلود عکس پروفایل: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<String> {
        return try {
            val response = api.deleteAccount()
            if (response.isSuccessful) {
                clearAllLocalData()
                Result.success(response.body()?.get("message") ?: "حساب با موفقیت حذف شد.")
            } else {
                Result.failure(Exception("خطا در حذف حساب کاربری"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun clearAllLocalData() {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            // ۱. پاک کردن دیتابیس محلی
            database.clearAllTables()

            // ۲. پاک کردن فایل‌های دانلود شده و کش
            val downloadsDir = File(appContext.cacheDir, "downloads")
            if (downloadsDir.exists()) {
                downloadsDir.deleteRecursively()
            }
            
            // ۳. پاک کردن سشن و اطلاعات کاربر
            userDirectory.clearAll()
            sessionManager.clearSession()
        }
    }

    private fun syncWithDirectory(profile: UserProfileResponseDto) {
        userDirectory.setUserInfo(
            UserInfoDto(
                username = profile.username,
                firstName = profile.firstName,
                lastName = profile.lastName,
                email = profile.email,
                phoneNumber = profile.phoneNumber,
                bio = profile.bio,
                profilePictureUrl = profile.profilePictureUrl
            )
        )
    }

    private suspend fun persistDisplayName(profile: UserProfileResponseDto) {
        val displayName = "${profile.firstName} ${profile.lastName}".trim()
        sessionManager.updateDisplayName(displayName)
    }

    private suspend fun persistUsername(profile: UserProfileResponseDto) {
        sessionManager.updateUsername(profile.username)
    }
}