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
import androidx.compose.ui.draw.clip
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
    isDark: Boolean,
    senderName: String? = null,
    isPinned: Boolean = false,
    isSelected: Boolean = false,
    isForwarded: Boolean = false,
    isEdited: Boolean = false,
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
                    val isStoryReply = replyToContent.startsWith("[STORY_MEDIA:")
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
                                    .height(if (isStoryReply) 42.dp else 32.dp)
                                    .background(primaryColor)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = replyToName ?: "پیام",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                                val cleanContent = if (isStoryReply) {
                                    val index = replyToContent.indexOf("]")
                                    if (index != -1) replyToContent.substring(index + 1) else replyToContent
                                } else replyToContent
                                
                                Text(
                                    text = cleanContent,
                                    fontSize = 11.sp,
                                    color = contentColor.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            if (isStoryReply) {
                                val mediaUrl = replyToContent.substringAfter("[STORY_MEDIA:").substringBefore("]")
                                coil.compose.AsyncImage(
                                    model = mediaUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                
                mediaContent?.invoke()
                
                val isInlineStoryReply = content.startsWith("[STORY_MEDIA:")
                val (storyMediaUrl, displayContent) = if (isInlineStoryReply) {
                    val closeBracketIndex = content.indexOf("]")
                    if (closeBracketIndex != -1) {
                        val url = content.substring("[STORY_MEDIA:".length, closeBracketIndex)
                        val rawText = content.substring(closeBracketIndex + 1)
                        val cleanText = rawText
                            .replace("🎬 پاسخ به استوری شما", "")
                            .replace("✨ واکنش به استوری شما", "")
                            .replace("💬", "")
                            .trim()
                        url to cleanText
                    } else {
                        null to content
                    }
                } else {
                    null to content
                }

                if (isInlineStoryReply && storyMediaUrl != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        color = contentColor.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(40.dp)
                                    .background(primaryColor, RoundedCornerShape(1.5.dp))
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (content.contains("واکنش")) "✨ واکنش به استوری" else "🎬 پاسخ به استوری",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                                Text(
                                    text = "استوری مخاطب",
                                    fontSize = 10.sp,
                                    color = contentColor.copy(alpha = 0.5f)
                                )
                            }
                            coil.compose.AsyncImage(
                                model = storyMediaUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                }
                
                if (displayContent.isNotBlank()) {
                    Text(
                        displayContent, 
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
