package com.example.veiltalk.feature.chat.data

import android.content.Context
import android.net.Uri
import com.example.veiltalk.common.util.MediaCrypto
import com.example.veiltalk.common.util.uriToChatFilePart
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

data class UploadedFile(val fileUrl: String, val mediaKey: String?, val displayName: String, val sizeBytes: Long)

@Singleton
class MediaRepository @Inject constructor(
    private val api: MediaApi,
    @ApplicationContext private val appContext: Context
) {
    suspend fun uploadFile(uri: Uri, encrypt: Boolean = true): Result<UploadedFile> {
        return try {
            val contentResolver = appContext.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return Result.failure(Exception("Could not open file"))
            val rawBytes = inputStream.use { it.readBytes() }
            
            val (bytesToUpload, mediaKey) = if (encrypt) {
                val result = MediaCrypto.encrypt(rawBytes)
                result.encryptedData to result.mediaKey
            } else {
                rawBytes to null
            }

            val requestBody = bytesToUpload.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            // استفاده از پسوند .enc برای فایل‌های رمزنگاری شده
            val part = MultipartBody.Part.createFormData("file", "encrypted_file.enc", requestBody)

            val response = api.uploadFile(part)
            if (response.isSuccessful && response.body() != null) {
                Result.success(
                    UploadedFile(
                        fileUrl = response.body()!!.fileUrl,
                        mediaKey = mediaKey,
                        displayName = "file",
                        sizeBytes = bytesToUpload.size.toLong()
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
