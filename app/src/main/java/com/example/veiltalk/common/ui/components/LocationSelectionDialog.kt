package com.example.veiltalk.common.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LocationSelectionDialog(
    onDismiss: () -> Unit,
    onSendCurrent: () -> Unit,
    onShareLive: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ارسال مکان") },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("ارسال مکان فعلی") },
                    leadingContent = { Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { onSendCurrent() }
                )
                ListItem(
                    headlineContent = { Text("اشتراک‌گذاری مکان زنده") },
                    leadingContent = { Icon(Icons.Default.Timer, null, tint = Color(0xFFE91E63)) },
                    modifier = Modifier.clickable { onShareLive() }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
