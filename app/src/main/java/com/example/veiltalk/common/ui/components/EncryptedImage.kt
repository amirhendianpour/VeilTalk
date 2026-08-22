package com.example.veiltalk.common.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.veiltalk.common.di.SessionManagerEntryPoint
import com.example.veiltalk.common.util.MediaCrypto
import com.example.veiltalk.core.network.ApiConfig
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

// کش در حافظه موقت (RAM)
private val memoryCache = LruCache<String, Bitmap>(50)

@Composable
fun EncryptedImage(
    url: String,
    mediaKey: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current

    val absoluteUrl = remember(url) {
        if (url.startsWith("http")) url 
        else "${ApiConfig.BASE_URL.removeSuffix("/")}/${url.removePrefix("/")}"
    }

    if (mediaKey == null) {
        coil.compose.AsyncImage(
            model = absoluteUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
        return
    }

    var bitmap by remember(absoluteUrl) { mutableStateOf<Bitmap?>(memoryCache.get(absoluteUrl)) }
    var isLoading by remember(absoluteUrl) { mutableStateOf(bitmap == null) }
    var hasError by remember(absoluteUrl) { mutableStateOf(false) }

    LaunchedEffect(absoluteUrl, mediaKey) {
        if (bitmap != null) return@LaunchedEffect
        
        isLoading = true
        hasError = false
        withContext(Dispatchers.IO) {
            try {
                // ۱. تولید نام فایل بر اساس هش URL برای کش دیسک
                val fileName = hashString(absoluteUrl)
                val cacheFile = File(context.cacheDir, "decrypted_$fileName")

                // ۲. بررسی وجود فایل در کش دیسک
                if (cacheFile.exists()) {
                    val savedBitmap = decodeSampledBitmapFromFile(cacheFile.absolutePath, 1024, 1024)
                    if (savedBitmap != null) {
                        memoryCache.put(absoluteUrl, savedBitmap)
                        bitmap = savedBitmap
                        isLoading = false
                        return@withContext
                    }
                }

                // ۳. دریافت توکن برای احراز هویت
                val sessionManager = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    SessionManagerEntryPoint::class.java
                ).sessionManager()
                val token = sessionManager.getToken()

                // ۴. اگر در کش نبود، دانلود و رمزگشایی
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val request = Request.Builder()
                    .url(absoluteUrl)
                    .apply {
                        if (token != null) {
                            addHeader("Authorization", "Bearer $token")
                        }
                    }
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val encryptedBytes = response.body?.bytes()
                    if (encryptedBytes != null) {
                        val decryptedBytes = MediaCrypto.decrypt(encryptedBytes, mediaKey)
                        val decodedBitmap = decodeSampledBitmapFromByteArray(decryptedBytes, 1024, 1024)
                        
                        if (decodedBitmap != null) {
                            // ۵. ذخیره در کش دیسک برای دفعات بعد
                            FileOutputStream(cacheFile).use { out ->
                                decodedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                            }
                            
                            memoryCache.put(absoluteUrl, decodedBitmap)
                            bitmap = decodedBitmap
                        } else {
                            hasError = true
                        }
                    }
                } else {
                    hasError = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                hasError = true
            }
        }
        isLoading = false
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier,
                contentScale = contentScale
            )
        } else if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (hasError) {
            androidx.compose.material3.Text("!", color = androidx.compose.ui.graphics.Color.Gray)
        }
    }
}

private fun decodeSampledBitmapFromByteArray(data: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(data, 0, data.size, this)
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
        inJustDecodeBounds = false
        BitmapFactory.decodeByteArray(data, 0, data.size, this)
    }
}

private fun decodeSampledBitmapFromFile(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, this)
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
        inJustDecodeBounds = false
        BitmapFactory.decodeFile(path, this)
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

// تابع کمکی برای تولید نام فایل یکتا
private fun hashString(input: String): String {
    return MessageDigest.getInstance("MD5")
        .digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
