package com.example.veiltalk.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veiltalk.common.util.formatMessageTime

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import com.example.veiltalk.ui.theme.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    content: String,
    timestamp: String?,
    isMine: Boolean,
    senderName: String? = null,
    isPinned: Boolean = false,
    isSelected: Boolean = false,
    isForwarded: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {},
    onSenderClick: () -> Unit = {},
    replyToName: String? = null,
    replyToContent: String? = null,
    onReplyClick: () -> Unit = {},
    status: @Composable (() -> Unit)? = null,
    mediaContent: @Composable (() -> Unit)? = null,
    reactionsContent: @Composable (() -> Unit)? = null
) {
    // تشخیص دقیق حالت تیره بر اساس رنگ‌های تم فعلی (نه لزوماً تم سیستم)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    
    val bubbleColor = if (isMine) {
        if (isDark) DarkBubbleMine else LightBubbleMine
    } else {
        if (isDark) DarkBubbleOthers else LightBubbleOthers
    }
    val contentColor = if (isDark) VeilWhite else Color.Black
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) primaryColor.copy(alpha = 0.2f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .shadow(
                    elevation = if (isDark) 0.dp else 1.dp,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp
                    )
                )
                .background(
                    color = bubbleColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 300.dp)
        ) {
            Column {
                if (isForwarded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            modifier = Modifier
                                .size(12.dp)
                                .graphicsLayer(scaleX = -1f), // برعکس کردن جهت آیکون ریپلای برای فوروارد
                            tint = contentColor.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "فوروارد شده",
                            fontSize = 10.sp,
                            color = contentColor.copy(alpha = 0.6f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }

                if (isPinned) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PushPin, null, modifier = Modifier.size(12.dp), tint = primaryColor)
                        Spacer(Modifier.width(4.dp))
                        Text("سنجاق شده", fontSize = 10.sp, color = primaryColor)
                    }
                    Spacer(Modifier.height(4.dp))
                }

                if (!isMine && senderName != null) {
                    Text(
                        senderName,
                        fontSize = 11.sp,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onSenderClick() }
                    )
                    Spacer(Modifier.height(2.dp))
                }

                if (replyToContent != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clickable { onReplyClick() },
                        color = contentColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(32.dp)
                                    .background(primaryColor)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = replyToName ?: "پیام",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                                Text(
                                    text = replyToContent,
                                    fontSize = 11.sp,
                                    color = contentColor.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                
                mediaContent?.invoke()
                
                if (content.isNotBlank()) {
                    Text(
                        content, 
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                }

                reactionsContent?.let {
                    Spacer(Modifier.height(4.dp))
                    it()
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatMessageTime(timestamp),
                        fontSize = 10.sp,
                        color = contentColor.copy(alpha = 0.6f)
                    )
                    if (isMine && status != null) {
                        Spacer(Modifier.width(4.dp))
                        status()
                    }
                }
            }
        }
    }
}
