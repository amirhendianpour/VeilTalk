package com.example.veiltalk.common.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veiltalk.feature.chat.ui.HomeListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardDestinationDialog(
    destinations: List<HomeListItem>,
    onDismiss: () -> Unit,
    onForwardToChat: (username: String) -> Unit,
    onForwardToGroup: (groupId: Long) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "ارسال به...",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                items(destinations) { item ->
                    when (item) {
                        is HomeListItem.ChatItem -> {
                            ListItem(
                                headlineContent = { Text(item.displayName) },
                                supportingContent = { Text("@${item.username}") },
                                leadingContent = {
                                    AvatarView(
                                        name = item.displayName,
                                        imageUrl = item.profilePictureUrl,
                                        size = 40.dp,
                                        colorSeed = item.username
                                    )
                                },
                                modifier = Modifier.clickable { onForwardToChat(item.username) }
                            )
                        }
                        is HomeListItem.GroupItem -> {
                            ListItem(
                                headlineContent = { Text(item.group.name) },
                                supportingContent = { Text("گروه") },
                                leadingContent = {
                                    AvatarView(
                                        name = item.group.name,
                                        imageUrl = item.group.imageUrl,
                                        size = 40.dp,
                                        colorSeed = "group-${item.group.id}"
                                    )
                                },
                                modifier = Modifier.clickable { onForwardToGroup(item.group.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
