package com.example.veiltalk.feature.chat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.veiltalk.common.ui.components.AvatarView
import com.example.veiltalk.feature.group.ui.CreateGroupDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenChat: (username: String) -> Unit,
    onOpenGroup: (groupId: Long) -> Unit,
    onOpenProfile: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var tab by remember { mutableStateOf(HomeTab.ALL) }
    var newChatInput by remember { mutableStateOf("") }
    var showNewChatField by remember { mutableStateOf(false) }
    var showCreateGroup by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onOpenProfile)
                    ) {
                        AvatarView(
                            name = uiState.myDisplayName.ifBlank { "?" },
                            size = 32.dp,
                            colorSeed = uiState.myDisplayName
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("پیام‌رسان من")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout(onLoggedOut) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "خروج")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (tab == HomeTab.GROUPS) showCreateGroup = true else showNewChatField = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "جدید")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            TabRow(selectedTabIndex = tab.ordinal) {
                Tab(selected = tab == HomeTab.ALL, onClick = { tab = HomeTab.ALL }, text = { Text("همه") })
                Tab(selected = tab == HomeTab.CHATS, onClick = { tab = HomeTab.CHATS }, text = { Text("چت‌های خصوصی") })
                Tab(selected = tab == HomeTab.GROUPS, onClick = { tab = HomeTab.GROUPS }, text = { Text("گروه‌ها") })
            }

            if (showNewChatField && tab != HomeTab.GROUPS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newChatInput,
                        onValueChange = { newChatInput = it },
                        placeholder = { Text("شماره موبایل یا ایمیل مخاطب...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.startNewChat(newChatInput) { username ->
                                newChatInput = ""
                                showNewChatField = false
                                onOpenChat(username)
                            }
                        },
                        enabled = !uiState.isLookingUp
                    ) {
                        Text(if (uiState.isLookingUp) "..." else "+ چت")
                    }
                }
                if (uiState.lookupError != null) {
                    Text(uiState.lookupError!!, color = Color(0xFFDC2626), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            when (tab) {
                HomeTab.ALL -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.allItems, key = { it.key }) { item ->
                        when (item) {
                            is HomeListItem.ChatItem -> ChatRow(
                                displayName = item.displayName,
                                imageUrl = item.profilePictureUrl,
                                colorSeed = item.username,
                                subtitle = "آنلاین",
                                subtitleColor = Color(0xFF22C55E),
                                onClick = { onOpenChat(item.username) }
                            )
                            is HomeListItem.GroupItem -> ChatRow(
                                displayName = item.group.name,
                                imageUrl = item.group.imageUrl,
                                colorSeed = "group-${item.group.id}",
                                subtitle = "گروه",
                                subtitleColor = Color.Gray,
                                onClick = { onOpenGroup(item.group.id) }
                            )
                        }
                        HorizontalDivider()
                    }
                }
                HomeTab.CHATS -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.chatItems, key = { it.key }) { item ->
                        ChatRow(
                            displayName = item.displayName,
                            imageUrl = item.profilePictureUrl,
                            colorSeed = item.username,
                            subtitle = "آنلاین",
                            subtitleColor = Color(0xFF22C55E),
                            onClick = { onOpenChat(item.username) }
                        )
                        HorizontalDivider()
                    }
                }
                HomeTab.GROUPS -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.groups, key = { it.id }) { group ->
                        ChatRow(
                            displayName = group.name,
                            imageUrl = group.imageUrl,
                            colorSeed = "group-${group.id}",
                            subtitle = if (group.role == "ADMIN") "ادمین" else "عضو",
                            subtitleColor = Color.Gray,
                            onClick = { onOpenGroup(group.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showCreateGroup) {
        CreateGroupDialog(
            onDismiss = { showCreateGroup = false },
            onConfirm = { name ->
                showCreateGroup = false
                viewModel.createGroup(name) { groupId -> onOpenGroup(groupId) }
            }
        )
    }
}

@Composable
private fun ChatRow(
    displayName: String,
    imageUrl: String?,
    colorSeed: String,
    subtitle: String,
    subtitleColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarView(name = displayName, imageUrl = imageUrl, size = 48.dp, colorSeed = colorSeed)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(displayName, fontWeight = FontWeight.Bold)
            Text(subtitle, color = subtitleColor, style = MaterialTheme.typography.bodySmall)
        }
    }
}