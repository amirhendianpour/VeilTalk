package com.example.veiltalk.common.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.veiltalk.common.util.MediaCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

@Composable
fun EncryptedImage(
    url: String,
    mediaKey: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    if (mediaKey == null) {
        // اگر کلید نداشت، یعنی رمزنگاری نشده (نسخه‌های قدیمی)
        coil.compose.AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
        return
    }

    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember(url) { mutableStateOf(true) }

    LaunchedEffect(url, mediaKey) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val encryptedBytes = response.body?.bytes()
                
                if (encryptedBytes != null) {
                    val decryptedBytes = MediaCrypto.decrypt(encryptedBytes, mediaKey)
                    bitmap = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isLoading = false
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else if (isLoading) {
        androidx.compose.material3.CircularProgressIndicator(modifier = modifier)
    }
}
