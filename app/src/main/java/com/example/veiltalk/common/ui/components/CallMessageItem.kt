package com.example.veiltalk.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veiltalk.ui.theme.VeilWhite

@Composable
fun CallMessageItem(
    content: String,
    isMine: Boolean,
    onCallBack: (isVideo: Boolean) -> Unit
) {
    val parts = content.split("|")
    val callType = parts.getOrNull(0) ?: "AUDIO" // AUDIO, VIDEO
    val direction = parts.getOrNull(1) ?: "INCOMING" // INCOMING, OUTGOING
    val status = parts.getOrNull(2) ?: "CONNECTED" // CONNECTED, MISSED, REJECTED, BUSY, NO_ANSWER
    val durationSec = parts.getOrNull(3)?.toLongOrNull() ?: 0L

    val isVideo = callType == "VIDEO"
    val isMissed = status in listOf("MISSED", "REJECTED", "BUSY", "NO_ANSWER")

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val contentColor = if (isDark) VeilWhite else Color.Black

    val statusColor = if (isMissed) Color(0xFFEF4444) else Color(0xFF10B981)

    val title = when {
        isVideo -> {
            if (direction == "OUTGOING") {
                if (isMissed) "تماس تصویری بی‌پاسخ" else "تماس تصویری خروجی"
            } else {
                if (isMissed) "تماس تصویری از دست رفته" else "تماس تصویری ورودی"
            }
        }
        else -> {
            if (direction == "OUTGOING") {
                if (isMissed) "تماس صوتی بی‌پاسخ" else "تماس صوتی خروجی"
            } else {
                if (isMissed) "تماس صوتی از دست رفته" else "تماس صوتی ورودی"
            }
        }
    }

    val subtitle = when {
        durationSec > 0 -> formatCallDuration(durationSec)
        status == "REJECTED" -> "رد تماس"
        status == "BUSY" -> "مشغول"
        isMissed -> "پاسخ داده نشده"
        else -> ""
    }

    Surface(
        modifier = Modifier
            .width(230.dp)
            .padding(vertical = 2.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, contentColor.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(statusColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        @Suppress("DEPRECATION")
                        Icon(
                            imageVector = when {
                                isVideo -> if (isMissed) Icons.Default.VideocamOff else Icons.Default.Videocam
                                isMissed -> Icons.Default.PhoneMissed
                                direction == "OUTGOING" -> Icons.AutoMirrored.Filled.CallMade
                                else -> Icons.AutoMirrored.Filled.CallReceived
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = contentColor
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                fontSize = 12.sp,
                                color = if (isMissed) statusColor else contentColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = contentColor.copy(alpha = 0.1f))
            TextButton(
                onClick = { onCallBack(isVideo) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("تماس مجدد", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

private fun formatCallDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) {
        "$mins دقیقه و $secs ثانیه"
    } else {
        "$secs ثانیه"
    }
}
