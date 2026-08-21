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
import androidx.compose.ui.draw.clip
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
    var showDeleteDialog by remember { mutableStateOf<List<String>?>(null) }
    var showForwardDialog by remember { mutableStateOf<List<String>?>(null) }
    var isSearchMode by remember { mutableStateOf(false) }

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
                            showDeleteDialog = selectedMessages.toList()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                        }
                        IconButton(onClick = {
                            showForwardDialog = selectedMessages.toList()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = "فوروارد")
                        }
                    }
                )
            } else {
                ChatTopBar(
                    title = if (isSearchMode) "" else uiState.groupName,
                    imageUrl = if (isSearchMode) null else uiState.groupImageUrl,
                    colorSeed = "group-${viewModel.groupId}",
                    onBack = {
                        if (isSearchMode) {
                            isSearchMode = false
                            viewModel.onSearchQueryChange("")
                        } else {
                            onBack()
                        }
                    },
                    onTitleClick = onOpenInfo,
                    actions = {
                        if (isSearchMode) {
                            TextField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::onSearchQueryChange,
                                placeholder = { Text("جستجو در پیام‌ها...") },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                        } else {
                            IconButton(onClick = { isSearchMode = true }) {
                                Icon(Icons.Default.Search, contentDescription = "جستجو")
                            }
                            IconButton(onClick = onOpenInfo) {
                                Icon(Icons.Default.Info, contentDescription = "اطلاعات گروه")
                            }
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
                    onSendSticker = viewModel::sendSticker,
                    onSendGif = viewModel::sendGif,
                    isEditing = uiState.editingMessage != null,
                    onCancelEdit = viewModel::cancelEditing,
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
            onEdit = if (msg.sender == uiState.myUsername) {
                {
                    viewModel.startEditing(msg)
                    showMessageMenu = null
                }
            } else null,
            onTogglePin = {
                viewModel.togglePin(msg.id, msg.isPinned)
            },
            onForward = {
                showForwardDialog = listOf(msg.id)
            },
            onDelete = {
                showDeleteDialog = listOf(msg.id)
            }
        )
    }

    if (showDeleteDialog != null) {
        val ids = showDeleteDialog!!
        val allMine = uiState.messages.filter { it.id in ids }.all { it.sender == uiState.myUsername }

        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("حذف پیام") },
            text = { Text("آیا مایل به حذف این پیام هستید؟") },
            confirmButton = {
                if (allMine) {
                    TextButton(onClick = {
                        viewModel.deleteMessagesForEveryone(ids)
                        selectedMessages = emptySet()
                        showDeleteDialog = null
                    }) {
                        Text("حذف برای همه", color = Color.Red)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.deleteMessages(ids)
                    selectedMessages = emptySet()
                    showDeleteDialog = null
                }) {
                    Text("حذف برای من")
                }
            }
        )
    }

    if (showForwardDialog != null) {
        val ids = showForwardDialog!!
        ForwardDestinationDialog(
            destinations = uiState.allDestinations,
            onDismiss = { showForwardDialog = null },
            onForwardToChat = { target ->
                viewModel.forwardMessages(target, ids)
                showForwardDialog = null
                selectedMessages = emptySet()
            },
            onForwardToGroup = { targetId ->
                viewModel.forwardMessagesToGroup(targetId, ids)
                showForwardDialog = null
                selectedMessages = emptySet()
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
        status = {
            if (mine) {
                Text(
                    text = if (message.status == com.example.veiltalk.common.model.MessageStatus.DELIVERED || message.status == com.example.veiltalk.common.model.MessageStatus.READ) "✓✓" else "✓",
                    fontSize = 10.sp,
                    color = if (message.status == com.example.veiltalk.common.model.MessageStatus.READ) Color(0xFF3B82F6) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        },
        mediaContent = {
            when (message.messageType) {
                com.example.veiltalk.common.model.MessageType.IMAGE -> {
                    if (!message.fileUrl.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = message.fileUrl,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                com.example.veiltalk.common.model.MessageType.GIF, com.example.veiltalk.common.model.MessageType.STICKER -> {
                    if (!message.fileUrl.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = message.fileUrl,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier
                                .size(if (message.messageType == com.example.veiltalk.common.model.MessageType.STICKER) 120.dp else 200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                else -> {}
            }
        },
        onClick = onClick,
        onLongClick = onLongClick,
        onSenderClick = onSenderClick
    )
}