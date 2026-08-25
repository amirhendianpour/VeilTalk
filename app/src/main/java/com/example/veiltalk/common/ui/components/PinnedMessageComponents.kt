package com.example.veiltalk.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.veiltalk.common.model.BaseMessage
import com.example.veiltalk.common.model.MessageType

@Composable
fun PinnedMessagesBar(
    messages: List<BaseMessage>,
    onMessageClick: (BaseMessage) -> Unit,
    onUnpin: (BaseMessage) -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(messages.size - 1) }
    
    LaunchedEffect(messages.size) {
        currentIndex = (messages.size - 1).coerceAtLeast(0)
    }
    
    val message = if (messages.isNotEmpty()) messages[currentIndex.coerceIn(0, messages.size - 1)] else return
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (messages.size > 1) {
                    currentIndex = if (currentIndex <= 0) messages.size - 1 else currentIndex - 1
                }
                onMessageClick(messages[currentIndex])
            },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (messages.size > 1) "پیام‌های سنجاق شده (${messages.size})" else "پیام سنجاق شده",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when (message.messageType) {
                        MessageType.TEXT -> message.content
                        MessageType.IMAGE -> "تصویر"
                        MessageType.VOICE -> "پیام صوتی"
                        MessageType.FILE -> "فایل"
                        MessageType.STICKER -> "استیکر"
                        MessageType.GIF -> "گیف"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = { onUnpin(message) }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "برداشتن سنجاق",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PinActionDialog(
    isUnpinning: Boolean,
    onConfirm: (forEveryone: Boolean) -> Unit,
    onDismiss: () -> Unit,
    isGroup: Boolean = false
) {
    var forEveryone by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isUnpinning) "برداشتن سنجاق" else "سنجاق کردن پیام") },
        text = {
            Column {
                Text(if (isUnpinning) "آیا مایل به برداشتن سنجاق این پیام هستید؟" else "آیا مایل به سنجاق کردن این پیام هستید؟")
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { forEveryone = !forEveryone }
                ) {
                    Checkbox(checked = forEveryone, onCheckedChange = { forEveryone = it })
                    Text(
                        if (isGroup) {
                            if (isUnpinning) "حذف سنجاق برای همه اعضا" else "سنجاق برای همه اعضا"
                        } else {
                            if (isUnpinning) "حذف برای هر دو طرف" else "سنجاق برای هر دو طرف"
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(forEveryone) }) {
                Text("تایید")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
