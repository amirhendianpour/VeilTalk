package com.example.veiltalk.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veiltalk.common.util.FileDownloader
import com.example.veiltalk.core.network.ApiConfig
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FileMessageItem(
    url: String,
    mediaKey: String?,
    fileName: String,
    isMine: Boolean,
    onLongClick: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    
    // چک کردن اینکه آیا فایل قبلاً دانلود شده یا خیر
    val hashedUrl = remember(url) { 
        java.security.MessageDigest.getInstance("MD5")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    val localFile = remember { File(File(context.cacheDir, "downloads"), "${hashedUrl}_$fileName") }
    var fileExists by remember { mutableStateOf(localFile.exists()) }

    val absoluteUrl = remember(url) {
        if (url.startsWith("http")) url 
        else "${ApiConfig.BASE_URL.removeSuffix("/")}/${url.removePrefix("/")}"
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accentColor = if (isDark) Color.White else (if (isMine) MaterialTheme.colorScheme.primary else Color.Black)

    Surface(
        modifier = Modifier
            .width(250.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = {
                    if (fileExists) {
                        FileDownloader.openFile(context, localFile)
                    } else {
                        isDownloading = true
                        scope.launch {
                            val file = FileDownloader.downloadAndDecrypt(context, absoluteUrl, mediaKey, fileName)
                            isDownloading = false
                            if (file != null) fileExists = true
                        }
                    }
                },
                onLongClick = onLongClick
            ),
        color = accentColor.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = accentColor)
                } else {
                    Icon(
                        imageVector = if (fileExists) Icons.Default.OpenInNew else Icons.Default.Download,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (fileExists) "آماده باز کردن" else "کلیک برای دانلود",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}
