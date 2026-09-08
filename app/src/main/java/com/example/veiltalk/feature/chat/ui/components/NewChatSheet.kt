package com.example.veiltalk.feature.chat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veiltalk.common.ui.components.AvatarView
import com.example.veiltalk.feature.chat.ui.HomeListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatSheet(
    contacts: List<HomeListItem.ChatItem>,
    onClose: () -> Unit,
    onNewGroup: () -> Unit,
    onNewContact: () -> Unit,
    onInvite: () -> Unit,
    onSelectContact: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            Text(
                "چت جدید",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    ActionItem(
                        icon = Icons.Default.GroupAdd,
                        title = "گروه جدید",
                        onClick = onNewGroup
                    )
                }
                item {
                    ActionItem(
                        icon = Icons.Default.PersonAdd,
                        title = "مخاطب جدید",
                        onClick = onNewContact
                    )
                }
                item {
                    ActionItem(
                        icon = Icons.Default.Share,
                        title = "دعوت از دوستان",
                        onClick = onInvite
                    )
                }

                item {
                    Text(
                        "مخاطبین شما",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(contacts, key = { it.username }) { contact ->
                    ListItem(
                        headlineContent = { Text(contact.displayName, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("@${contact.username}", fontSize = 12.sp) },
                        leadingContent = {
                            AvatarView(
                                name = contact.displayName,
                                imageUrl = contact.profilePictureUrl,
                                size = 40.dp,
                                colorSeed = contact.username
                            )
                        },
                        modifier = Modifier.clickable { onSelectContact(contact.username) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        leadingContent = {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}
