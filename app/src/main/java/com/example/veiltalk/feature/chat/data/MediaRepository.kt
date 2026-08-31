package com.example.veiltalk.feature.chat.data

import android.content.Context
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import com.example.veiltalk.common.util.MediaCrypto
import com.example.veiltalk.core.network.ApiConfig
import com.example.veiltalk.core.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
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
    private val sessionManager: SessionManager,
    @ApplicationContext private val appContext: Context
) {
    private val client = OkHttpClient()

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

    /**
     * دانلود و رمزگشایی فایل (کش در حافظه محلی اپلیکیشن)
     */
    suspend fun downloadAndDecrypt(
        url: String,
        mediaKey: String?,
        fileName: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val absoluteUrl = getAbsoluteUrl(url)
            val hashedUrl = hashString(absoluteUrl)
            val outputDir = File(appContext.cacheDir, "downloads").apply { mkdirs() }
            val outputFile = File(outputDir, "${hashedUrl}_$fileName")

            if (outputFile.exists()) return@withContext Result.success(outputFile)

            val token = sessionManager.getToken()
            val request = Request.Builder()
                .url(absoluteUrl)
                .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Download failed: ${response.code}"))

            val rawBytes = response.body?.bytes() ?: return@withContext Result.failure(Exception("Empty body"))
            
            val finalBytes = if (mediaKey != null) {
                MediaCrypto.decrypt(rawBytes, mediaKey)
            } else {
                rawBytes
            }

            FileOutputStream(outputFile).use { it.write(finalBytes) }
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ذخیره‌سازی فایل در حافظه عمومی گوشی (گالری یا دانلودها)
     */
    suspend fun saveToPublicStorage(
        url: String,
        mediaKey: String?,
        fileName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val downloadResult = downloadAndDecrypt(url, mediaKey, fileName)
        if (downloadResult.isFailure) return@withContext Result.failure(downloadResult.exceptionOrNull()!!)
        
        val file = downloadResult.getOrNull()!!
        
        try {
            val extension = fileName.substringAfterLast('.', "")
            val isImage = extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif")
            val isVideo = extension.lowercase() in listOf("mp4", "mkv", "mov", "avi")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, appContext.contentResolver.getType(Uri.fromFile(file)) ?: "*/*")
                    val relativePath = when {
                        isImage -> Environment.DIRECTORY_PICTURES + "/VeilTalk"
                        isVideo -> Environment.DIRECTORY_MOVIES + "/VeilTalk"
                        else -> Environment.DIRECTORY_DOWNLOADS + "/VeilTalk"
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                }

                val collection = when {
                    isImage -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }

                val uri = appContext.contentResolver.insert(collection, contentValues)
                    ?: return@withContext Result.failure(Exception("Could not create MediaStore entry"))
                
                appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(file).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } else {
                val publicDir = when {
                    isImage -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    isVideo -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                }
                val veilTalkDir = File(publicDir, "VeilTalk").apply { mkdirs() }
                val destFile = File(veilTalkDir, fileName)
                
                FileInputStream(file).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                android.media.MediaScannerConnection.scanFile(appContext, arrayOf(destFile.absolutePath), null, null)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAbsoluteUrl(url: String): String {
        return if (url.startsWith("http")) url 
        else "${ApiConfig.BASE_URL.removeSuffix("/")}/${url.removePrefix("/")}"
    }

    private fun hashString(input: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
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
