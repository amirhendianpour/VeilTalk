package com.example.veiltalk.feature.chat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.veiltalk.common.ui.components.AvatarView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = hiltViewModel(),
    onOpenChat: (username: String) -> Unit,
    onLoggedOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var newChatInput by remember { mutableStateOf("") }
    var showNewChatField by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("پیام‌رسان من") },
                actions = {
                    IconButton(onClick = { viewModel.logout(onLoggedOut) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "خروج")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showNewChatField) {
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
                } else {
                    OutlinedButton(
                        onClick = { showNewChatField = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ شروع چت جدید")
                    }
                }
            }

            if (uiState.lookupError != null) {
                Text(
                    uiState.lookupError!!,
                    color = Color(0xFFDC2626),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("هیچ چتی ندارید. یک مخاطب پیدا کنید!", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.items, key = { it.username }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenChat(item.username) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarView(
                                name = item.displayName,
                                imageUrl = item.profilePictureUrl,
                                size = 48.dp,
                                colorSeed = item.username
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(item.displayName, fontWeight = FontWeight.Bold)
                                Text("آنلاین", fontSize = androidx.compose.ui.unit.TextUnit.Unspecified, color = Color(0xFF22C55E))
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}