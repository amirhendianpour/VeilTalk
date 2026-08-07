package com.example.veiltalk.feature.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.veiltalk.common.model.GroupMessage
import com.example.veiltalk.common.ui.components.AvatarView
import com.example.veiltalk.common.util.formatMessageTime
import com.example.veiltalk.feature.user.data.UserDirectoryRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    viewModel: GroupChatViewModel = hiltViewModel(),
    userDirectory: UserDirectoryRepository, // برای نمایش نام فرستنده هر پیام
    onBack: () -> Unit,
    onOpenInfo: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val listState = rememberLazyListState()
    val myUsername = "" // پایین با LocalContext جایگزین می‌کنیم؛ در عمل از SessionManager می‌خونیم

    LaunchedEffect(uiState.messages.map { it.sender }.distinct()) {
        userDirectory.ensureLoaded(uiState.messages.mapNotNull { it.sender }.distinct())
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarView(
                            name = uiState.groupName,
                            imageUrl = uiState.groupImageUrl,
                            size = 36.dp,
                            colorSeed = "group-${viewModel.groupId}"
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.groupName)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenInfo) {
                        Icon(Icons.Default.Info, contentDescription = "اطلاعات گروه")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F4F6))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = viewModel::onInputChange,
                    placeholder = { Text("پیام خود را بنویسید...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = viewModel::sendMessage, enabled = inputText.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "ارسال")
                }
            }
        }
    ) { padding ->
        val directory by userDirectory.directory.collectAsState()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFEFEAE2))
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                val mine = message.sender == uiState.myUsername
                GroupMessageBubble(
                    message = message,
                    mine = mine,
                    senderDisplayName = message.sender?.let { s ->
                        directory[s]?.let { "${it.firstName} ${it.lastName}" } ?: s
                    } ?: ""
                )
            }
        }
    }
}

@Composable
private fun GroupMessageBubble(message: GroupMessage, mine: Boolean, senderDisplayName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.Start else Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (mine) Color(0xFFDCFCE7) else Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 280.dp)
        ) {
            Column {
                // اسم فرستنده فقط برای پیام‌های بقیه نمایش داده می‌شود — مثل GroupChatArea.tsx در وب
                if (!mine) {
                    Text(
                        senderDisplayName,
                        fontSize = 11.sp,
                        color = Color(0xFF2563EB),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
                Text(message.content)
                Text(formatMessageTime(message.timestamp), fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}