package com.example.veiltalk.feature.chat.data

import android.content.Context
import android.net.Uri
import com.example.veiltalk.common.util.uriToChatFilePart
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class UploadedFile(val fileUrl: String, val displayName: String, val sizeBytes: Long)

@Singleton
class MediaRepository @Inject constructor(
    private val api: MediaApi,
    @ApplicationContext private val appContext: Context
) {
    suspend fun uploadFile(uri: Uri): Result<UploadedFile> {
        val picked = uriToChatFilePart(appContext, uri)
            ?: return Result.failure(Exception("فایل نامعتبر است."))
        return try {
            val response = api.uploadFile(picked.part)
            if (response.isSuccessful && response.body() != null) {
                Result.success(
                    UploadedFile(
                        fileUrl = response.body()!!.fileUrl,
                        displayName = picked.displayName,
                        sizeBytes = picked.sizeBytes
                    )
                )
            } else {
                Result.failure(Exception("خطا در آپلود فایل"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}