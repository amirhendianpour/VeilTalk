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
        modifier = Modifier.padding(vertical = 4.dp).width(200.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp).padding(8.dp),
                strokeWidth = 2.dp,
                color = if (isMine) Color.White else MaterialTheme.colorScheme.primary
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
                    contentDescription = if (isPlaying) "توقف" else "پخش",
                    tint = if (isMine) Color.White else MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(Modifier.width(8.dp))
        
        LinearProgressIndicator(
            progress = { if (isPlaying) 0.5f else 0f },
            modifier = Modifier.weight(1f).height(4.dp),
            color = if (isMine) Color.White else MaterialTheme.colorScheme.primary,
            trackColor = (if (isMine) Color.White else MaterialTheme.colorScheme.primary).copy(alpha = 0.2f)
        )
        
        Spacer(Modifier.width(8.dp))
        
        Text(
            "Voice",
            fontSize = 12.sp,
            color = if (isMine) Color.White.copy(alpha = 0.7f) else Color.Gray
        )
    }
}

private fun hashString(input: String): String {
    return MessageDigest.getInstance("MD5")
        .digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
