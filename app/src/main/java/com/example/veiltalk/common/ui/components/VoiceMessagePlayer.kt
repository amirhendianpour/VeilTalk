package com.example.veiltalk.common.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veiltalk.common.di.SessionManagerEntryPoint
import com.example.veiltalk.common.util.MediaCrypto
import com.example.veiltalk.common.util.VoicePlayer
import com.example.veiltalk.core.network.ApiConfig
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

@Composable
fun VoiceMessagePlayer(
    url: String,
    mediaKey: String?,
    isMine: Boolean
) {
    val context = LocalContext.current
    val voicePlayer = remember { VoicePlayer(context) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // تشخیص تم تیره
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    
    // تعیین رنگ محتوا مشابه ChatMessageBubble برای کنتراست صحیح
    val accentColor = if (isDark) {
        Color.White // در تم تیره همه حباب‌ها تیره هستند، پس متن سفید باشد
    } else {
        // در تم روشن، حباب‌های ما بسیار روشن هستند، پس متن باید تیره باشد
        if (isMine) MaterialTheme.colorScheme.primary else Color.Black
    }
    
    val secondaryTextColor = accentColor.copy(alpha = 0.6f)

    val absoluteUrl = remember(url) {
        if (url.startsWith("http")) url 
        else "${ApiConfig.BASE_URL.removeSuffix("/")}/${url.removePrefix("/")}"
    }

    DisposableEffect(Unit) {
        onDispose { voicePlayer.stop() }
    }

    fun playVoice() {
        if (mediaKey == null) {
            voicePlayer.play(absoluteUrl) { isPlaying = false }
            isPlaying = true
            return
        }

        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val fileName = hashString(absoluteUrl)
                val cacheFile = File(context.cacheDir, "decrypted_voice_$fileName.m4a")

                if (!cacheFile.exists()) {
                    val sessionManager = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        SessionManagerEntryPoint::class.java
                    ).sessionManager()
                    val token = sessionManager.getToken()

                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url(absoluteUrl)
                        .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
                        .build()
                    
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val encryptedBytes = response.body?.bytes()
                        if (encryptedBytes != null) {
                            val decryptedBytes = MediaCrypto.decrypt(encryptedBytes, mediaKey)
                            FileOutputStream(cacheFile).use { it.write(decryptedBytes) }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (cacheFile.exists()) {
                        voicePlayer.play(cacheFile.absolutePath) { isPlaying = false }
                        isPlaying = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp).width(220.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp).padding(6.dp),
                strokeWidth = 2.dp,
                color = accentColor
            )
        } else {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        voicePlayer.stop()
                        isPlaying = false
                    } else {
                        playVoice()
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = accentColor
                )
            }
        }
        
        Spacer(Modifier.width(4.dp))
        
        // نوار پیشرفت با استایل مدرن‌تر
        LinearProgressIndicator(
            progress = { if (isPlaying) 0.5f else 0f },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(CircleShape),
            color = accentColor,
            trackColor = accentColor.copy(alpha = 0.2f)
        )
        
        Spacer(Modifier.width(10.dp))
        
        Text(
            "Voice",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = secondaryTextColor
        )
    }
}


private fun hashString(input: String): String {
    return MessageDigest.getInstance("MD5")
        .digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
