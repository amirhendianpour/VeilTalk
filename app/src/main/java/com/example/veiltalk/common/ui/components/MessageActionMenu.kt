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
import androidx.compose.material.icons.filled.Download
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
    onSave: (() -> Unit)? = null,
    onReact: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .width(200.dp) // کاهش عرض برای ظاهر جمع‌وجورتر
                    .clickable(enabled = false) { },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- بخش واکنش‌های سریع (جدا شده به صورت حباب شناور) ---
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val reactions = listOf("❤️", "👍", "👎", "😂", "😮", "😢", "🙏")
                        reactions.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onReact(emoji); onDismiss() }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                // --- لیست عملیات (به صورت کارت جداگانه و باریک‌تر) ---
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        ActionMenuItem(
                            text = "کپی",
                            icon = Icons.Default.ContentCopy,
                            onClick = { onCopy(); onDismiss() }
                        )
                        if (onReply != null) {
                            ActionMenuItem(
                                text = "پاسخ",
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
                            text = if (isPinned) "برداشتن سنجاق" else "سنجاق",
                            icon = Icons.Default.PushPin,
                            onClick = { onTogglePin(); onDismiss() }
                        )
                        ActionMenuItem(
                            text = "فوروارد",
                            icon = Icons.AutoMirrored.Filled.Forward,
                            onClick = { onForward(); onDismiss() }
                        )

                        if (onSave != null) {
                            ActionMenuItem(
                                text = "ذخیره در گوشی",
                                icon = Icons.Default.Download,
                                onClick = { onSave(); onDismiss() }
                            )
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        
                        ActionMenuItem(
                            text = "حذف",
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}
