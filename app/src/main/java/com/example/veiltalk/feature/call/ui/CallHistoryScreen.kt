package com.example.veiltalk.feature.call.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.veiltalk.common.ui.components.AvatarView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CallHistoryScreen(
    viewModel: CallHistoryViewModel = hiltViewModel(),
    onCallClick: (String, com.example.veiltalk.common.model.CallKind) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var logToDelete by remember { mutableStateOf<CallLogItem?>(null) }

    if (uiState.logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(16.dp))
                Text("تاریخچه تماسی وجود ندارد", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(uiState.logs, key = { it.id }) { log ->
                CallHistoryRow(
                    item = log,
                    onClick = { onCallClick(log.remoteUsername, com.example.veiltalk.common.model.CallKind.valueOf(log.callType)) },
                    onLongClick = { logToDelete = log }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    thickness = 0.5.dp,
                    color = Color.LightGray.copy(alpha = 0.5f)
                )
            }
        }
    }

    if (logToDelete != null) {
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("حذف تاریخچه تماس") },
            text = { Text("آیا مایل به حذف این مورد از تاریخچه تماس هستید؟") },
            confirmButton = {
                TextButton(onClick = {
                    logToDelete?.let { viewModel.deleteLog(it.id) }
                    logToDelete = null
                }) {
                    Text("حذف", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CallHistoryRow(
    item: CallLogItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        leadingContent = {
            AvatarView(
                name = item.remoteDisplayName,
                imageUrl = item.remoteProfilePicture,
                size = 48.dp,
                colorSeed = item.remoteUsername
            )
        },
        headlineContent = {
            Text(
                item.remoteDisplayName,
                fontWeight = FontWeight.SemiBold,
                color = if (item.status == "MISSED") Color.Red else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (item.direction == "INCOMING") Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (item.status == "MISSED") Color.Red else Color.Gray
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${formatCallTime(item.startTime)} ${if (item.duration > 0) "(${formatDuration(item.duration)})" else ""}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        trailingContent = {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = if (item.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

private fun formatCallTime(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Calendar.getInstance()
    val callDate = Calendar.getInstance().apply { time = date }

    return if (now.get(Calendar.DATE) == callDate.get(Calendar.DATE)) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    } else {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(date)
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}
