package com.example.veiltalk.feature.group.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.veiltalk.common.model.GroupMessage
import com.example.veiltalk.common.ui.components.*
import com.example.veiltalk.common.util.formatMessageTime
import com.example.veiltalk.feature.user.data.UserDirectoryRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    viewModel: GroupChatViewModel = hiltViewModel(),
    userDirectory: UserDirectoryRepository,
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    var selectedMessages by remember { mutableStateOf(setOf<String>()) }
    var showMessageMenu by remember { mutableStateOf<GroupMessage?>(null) }

    val isSelectionMode = selectedMessages.isNotEmpty()

    LaunchedEffect(uiState.messages.map { it.sender }.distinct()) {
        userDirectory.ensureLoaded(uiState.messages.mapNotNull { it.sender }.distinct())
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.size - 1)
    }

    ChatBaseLayout(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    title = { Text(selectedMessages.size.toString()) },
                    navigationIcon = {
                        IconButton(onClick = { selectedMessages = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "بستن")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val text = uiState.messages.filter { it.id in selectedMessages }
                                .joinToString("\n") { it.content }
                            clipboardManager.setText(AnnotatedString(text))
                            selectedMessages = emptySet()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "کپی")
                        }
                        IconButton(onClick = {
                            viewModel.deleteMessages(selectedMessages.toList())
                            selectedMessages = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                        }
                        IconButton(onClick = { /* Forward logic */ }) {
                            Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = "فوروارد")
                        }
                    }
                )
            } else {
                ChatTopBar(
                    title = uiState.groupName,
                    imageUrl = uiState.groupImageUrl,
                    colorSeed = "group-${viewModel.groupId}",
                    onBack = onBack,
                    onTitleClick = onOpenInfo,
                    actions = {
                        IconButton(onClick = onOpenInfo) {
                            Icon(Icons.Default.Info, contentDescription = "اطلاعات گروه")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isSelectionMode) {
                ChatInputBar(
                    value = inputText,
                    onValueChange = viewModel::onInputChange,
                    onSendMessage = viewModel::sendMessage,
                    placeholder = "پیام خود را بنویسید..."
                )
            }
        }
    ) { paddingValues ->
        val directory by userDirectory.directory.collectAsState()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = paddingValues
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                val mine = message.sender == uiState.myUsername
                GroupMessageBubble(
                    message = message,
                    mine = mine,
                    isSelected = message.id in selectedMessages,
                    senderDisplayName = message.sender?.let { s ->
                        directory[s]?.let { "${it.firstName} ${it.lastName}" } ?: s
                    } ?: "",
                    onClick = {
                        if (isSelectionMode) {
                            selectedMessages = if (message.id in selectedMessages) {
                                selectedMessages - message.id
                            } else {
                                selectedMessages + message.id
                            }
                        } else {
                            showMessageMenu = message
                        }
                    },
                    onLongClick = {
                        selectedMessages = setOf(message.id)
                    },
                    onSenderClick = {
                        message.sender?.let { onOpenProfile(it) }
                    }
                )
            }
        }
    }

    if (showMessageMenu != null) {
        val msg = showMessageMenu!!
        MessageActionMenu(
            isPinned = msg.isPinned,
            onDismiss = { showMessageMenu = null },
            onCopy = {
                clipboardManager.setText(AnnotatedString(msg.content))
            },
            onTogglePin = {
                viewModel.togglePin(msg.id, msg.isPinned)
            },
            onForward = {
                // Forward logic
            },
            onDelete = {
                viewModel.deleteMessages(listOf(msg.id))
            }
        )
    }
}

@Composable
private fun GroupMessageBubble(
    message: GroupMessage,
    mine: Boolean,
    isSelected: Boolean,
    senderDisplayName: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSenderClick: () -> Unit
) {
    ChatMessageBubble(
        content = message.content,
        timestamp = message.timestamp,
        isMine = mine,
        isPinned = message.isPinned,
        isSelected = isSelected,
        senderName = if (!mine) senderDisplayName else null,
        onClick = onClick,
        onLongClick = onLongClick,
        onSenderClick = onSenderClick
    )
}