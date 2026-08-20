package com.example.veiltalk.common.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.veiltalk.ui.theme.WaTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionMenu(
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onTogglePin: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = WaTeal.copy(alpha = 0.3f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            ActionMenuItem(
                text = "کپی کردن متن",
                icon = Icons.Default.ContentCopy,
                onClick = {
                    onCopy()
                    onDismiss()
                }
            )
            if (onEdit != null) {
                ActionMenuItem(
                    text = "ویرایش پیام",
                    icon = Icons.Default.Edit,
                    onClick = {
                        onEdit()
                        onDismiss()
                    }
                )
            }
            ActionMenuItem(
                text = if (isPinned) "برداشتن سنجاق" else "سنجاق کردن پیام",
                icon = Icons.Default.PushPin,
                onClick = {
                    onTogglePin()
                    onDismiss()
                }
            )
            ActionMenuItem(
                text = "فوروارد کردن",
                icon = Icons.AutoMirrored.Filled.Forward,
                onClick = {
                    onForward()
                    onDismiss()
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color.LightGray.copy(alpha = 0.5f)
            )
            ActionMenuItem(
                text = "حذف پیام",
                icon = Icons.Default.Delete,
                iconTint = Color.Red,
                textColor = Color.Red,
                onClick = {
                    onDelete()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun ActionMenuItem(
    text: String,
    icon: ImageVector,
    iconTint: Color = WaTeal,
    textColor: Color = Color.Black,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
