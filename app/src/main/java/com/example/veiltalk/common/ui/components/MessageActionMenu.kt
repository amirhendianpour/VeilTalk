package com.example.veiltalk.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
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
    onReact: ((String) -> Unit)? = null // جدید: برای واکنش سریع با ایموجی
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // --- بخش واکنش‌های سریع (مشابه سیگنال) ---
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { 
                                onReact?.invoke(emoji)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 22.sp)
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // --- لیست عملیات ---
            ActionMenuItem(
                text = "کپی کردن",
                icon = Icons.Default.ContentCopy,
                onClick = { onCopy(); onDismiss() }
            )
            
            if (onReply != null) {
                ActionMenuItem(
                    text = "پاسخ (Reply)",
                    icon = Icons.AutoMirrored.Filled.Reply,
                    onClick = { onReply(); onDismiss() }
                )
            }
            
            if (onEdit != null) {
                ActionMenuItem(
                    text = "ویرایش",
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
                text = "فوروارد",
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
