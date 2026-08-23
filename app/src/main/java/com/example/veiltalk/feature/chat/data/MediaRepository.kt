package com.example.veiltalk.feature.chat.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.veiltalk.common.util.MediaCrypto
import com.example.veiltalk.common.util.uriToChatFilePart
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class UploadedFile(
    val fileUrl: String,
    val mediaKey: String?,
    val displayName: String,
    val sizeBytes: Long,
    val thumbnail: String? = null
)

@Singleton
class MediaRepository @Inject constructor(
    private val api: MediaApi,
    @ApplicationContext private val appContext: Context
) {
    suspend fun uploadFile(uri: Uri, encrypt: Boolean = true): Result<UploadedFile> {
        val contentResolver = appContext.contentResolver
        val fileName = getFileName(uri) ?: "file_${System.currentTimeMillis()}"
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        
        val inputStream = contentResolver.openInputStream(uri) ?: return Result.failure(Exception("Could not open file"))
        val rawBytes = inputStream.use { it.readBytes() }
        
        return uploadBytes(rawBytes, mimeType, encrypt, fileName)
    }

    suspend fun uploadBytes(
        rawBytes: ByteArray, 
        mimeType: String, 
        encrypt: Boolean = true,
        fileName: String = "file"
    ): Result<UploadedFile> {
        return try {
            var thumbnail: String? = null
            if (mimeType.startsWith("image/")) {
                thumbnail = generateThumbnail(rawBytes)
            }

            val (bytesToUpload, mediaKey) = if (encrypt) {
                val result = MediaCrypto.encrypt(rawBytes)
                result.encryptedData to result.mediaKey
            } else {
                rawBytes to null
            }

            val requestBody = bytesToUpload.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            // ارسال نام فایل اصلی با پسوند .enc به سرور
            val part = MultipartBody.Part.createFormData("file", "$fileName.enc", requestBody)

            val response = api.uploadFile(part)
            if (response.isSuccessful && response.body() != null) {
                Result.success(
                    UploadedFile(
                        fileUrl = response.body()!!.fileUrl,
                        mediaKey = mediaKey,
                        displayName = fileName, // حالا نام واقعی فایل است
                        sizeBytes = bytesToUpload.size.toLong(),
                        thumbnail = thumbnail
                    )
                )
            } else {
                Result.failure(Exception("خطا در آپلود فایل"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = appContext.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) name = it.getString(index)
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/') ?: -1
            if (cut != -1) name = name?.substring(cut + 1)
        }
        return name
    }

    private fun generateThumbnail(imageBytes: ByteArray): String? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
            
            val targetSize = 40
            var sampleSize = 1
            while (options.outHeight / sampleSize > targetSize || options.outWidth / sampleSize > targetSize) {
                sampleSize *= 2
            }
            
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOptions) ?: return null
            
            val scaled = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            val thumbBytes = outputStream.toByteArray()
            Base64.encodeToString(thumbBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}
