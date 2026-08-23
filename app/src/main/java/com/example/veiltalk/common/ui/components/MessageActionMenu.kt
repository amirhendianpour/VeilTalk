package com.example.veiltalk.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun MessageActionMenu(
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onReply: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onTogglePin: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    onReact: ((String) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // اجازه می‌دهد عرض سفارشی داشته باشیم
        )
    ) {
        // پس‌زمینه تیره (Scrim) که با کلیک روی آن منو بسته می‌شود
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            // کارت منو (شناور مشابه سیگنال)
            Surface(
                modifier = Modifier
                    .padding(16.dp) // فاصله از لبه‌های صفحه
                    .fillMaxWidth()
                    .clickable(enabled = false) { }, // جلوگیری از بسته شدن با کلیک روی خودِ منو
                shape = RoundedCornerShape(28.dp), // گوشه‌های کاملاً گرد
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    // --- بخش واکنش‌های سریع (Reactions) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val reactions = listOf("❤️", "👍", "👎", "😂", "😮", "😢", "🙏")
                        reactions.forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable { 
                                        onReact?.invoke(emoji)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 24.sp)
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // --- لیست عملیات ---
                    ActionMenuItem(
                        text = "کپی کردن",
                        icon = Icons.Default.ContentCopy,
                        onClick = { onCopy(); onDismiss() }
                    )
                    
                    if (onReply != null) {
                        ActionMenuItem(
                            text = "پاسخ دادن",
                            icon = Icons.AutoMirrored.Filled.Reply,
                            onClick = { onReply(); onDismiss() }
                        )
                    }
                    
                    if (onEdit != null) {
                        ActionMenuItem(
                            text = "ویرایش پیام",
                            icon = Icons.Default.Edit,
                            onClick = { onEdit(); onDismiss() }
                        )
                    }
                    
                    ActionMenuItem(
                        text = if (isPinned) "برداشتن سنجاق" else "سنجاق کردن",
                        icon = Icons.Default.PushPin,
                        onClick = { onTogglePin(); onDismiss() }
                    )
                    
                    ActionMenuItem(
                        text = "فوروارد (ارسال به دیگران)",
                        icon = Icons.AutoMirrored.Filled.Forward,
                        onClick = { onForward(); onDismiss() }
                    )
                    
                    ActionMenuItem(
                        text = "حذف پیام",
                        icon = Icons.Default.Delete,
                        textColor = MaterialTheme.colorScheme.error,
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = { onDelete(); onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionMenuItem(
    text: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}
