package com.example.veiltalk.feature.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.veiltalk.common.ui.components.AvatarView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    viewModel: BlockedUsersViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لیست مسدودی‌ها") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.blockedUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (uiState.blockedUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("لیست مسدودی‌های شما خالی است.", color = Color.Gray)
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(uiState.blockedUsers, key = { it.username }) { user ->
                ListItem(
                    headlineContent = { Text("${user.firstName} ${user.lastName}") },
                    supportingContent = { Text("@${user.username}") },
                    leadingContent = {
                        AvatarView(
                            name = "${user.firstName} ${user.lastName}",
                            imageUrl = user.profilePictureUrl,
                            size = 40.dp,
                            colorSeed = user.username
                        )
                    },
                    trailingContent = {
                        TextButton(onClick = { viewModel.unblockUser(user.username) }) {
                            Text("رفع مسدودیت")
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
