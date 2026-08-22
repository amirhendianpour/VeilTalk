package com.example.veiltalk.feature.group.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.veiltalk.common.model.GroupMessage
import com.example.veiltalk.common.model.MessageType
import com.example.veiltalk.common.ui.components.*
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    userDirectory: UserDirectoryRepository,
    viewModel: GroupChatViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenGroup: (Long) -> Unit // جدید: برای انتقال بعد از فوروارد
) {
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.sendImage(it) } }

    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.sendFile(it) } }

    var selectedMessages by remember { mutableStateOf(setOf<String>()) }
    var showMessageMenu by remember { mutableStateOf<GroupMessage?>(null) }
    var showDeleteDialog by remember { mutableStateOf<List<String>?>(null) }
    var showForwardDialog by remember { mutableStateOf<List<String>?>(null) }
    var isSearchMode by remember { mutableStateOf(false) }

    val isSelectionMode = selectedMessages.isNotEmpty()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ChatBaseLayout(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
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
                        IconButton(onClick = { showDeleteDialog = selectedMessages.toList() }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                        }
                        IconButton(onClick = { showForwardDialog = selectedMessages.toList() }) {
                            Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = "فوروارد")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
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
                    onAttachImage = { imagePicker.launch("image/*") },
                    onAttachFile = { filePicker.launch("*/*") },
                    onSendSticker = viewModel::sendSticker,
                    onSendGif = viewModel::sendGif,
                    isEditing = uiState.editingMessage != null,
                    replyingMessageContent = uiState.replyingMessage?.content,
                    replyingMessageSender = uiState.replyingMessage?.let { if (it.sender == uiState.myUsername) "شما" else userDirectory.getDisplayName(it.sender ?: "") },
                    onCancelEdit = viewModel::cancelEditing,
                    onCancelReply = viewModel::cancelReplying,
                    isUploading = isUploading,
                    uploadError = uploadError,
                    onClearUploadError = viewModel::clearUploadError,
                    placeholder = "پیام خود را بنویسید..."
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    val mine = message.sender == uiState.myUsername
                    val repliedTo = message.replyToId?.let { rid -> uiState.messages.find { it.id == rid } }
                    GroupMessageBubble(
                        message = message,
                        mine = mine,
                        isSelected = message.id in selectedMessages,
                        senderDisplayName = userDirectory.getDisplayName(message.sender ?: ""),
                        replyToName = repliedTo?.let { if (it.sender == uiState.myUsername) "شما" else userDirectory.getDisplayName(it.sender ?: "") },
                        replyToContent = repliedTo?.content,
                        onReplyClick = {
                            val index = uiState.messages.indexOfFirst { it.id == message.replyToId }
                            if (index != -1) {
                                scope.launch { listState.animateScrollToItem(index) }
                            }
                        },
                        onClick = {
                            if (isSelectionMode) {
                                selectedMessages = if (message.id in selectedMessages) selectedMessages - message.id else selectedMessages + message.id
                            } else {
                                showMessageMenu = message
                            }
                        },
                        onLongClick = { selectedMessages = setOf(message.id) },
                        onSenderClick = { message.sender?.let { onOpenProfile(it) } }
                    )
                }
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
            onReply = {
                viewModel.startReplying(msg)
                showMessageMenu = null
            },
            onEdit = if (msg.sender == uiState.myUsername) {
                {
                    viewModel.startEditing(msg)
                    showMessageMenu = null
                }
            } else null,
            onTogglePin = { viewModel.togglePin(msg.id, msg.isPinned) },
            onForward = {
                showForwardDialog = listOf(msg.id)
                showMessageMenu = null
            },
            onDelete = {
                showDeleteDialog = listOf(msg.id)
                showMessageMenu = null
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
                onOpenProfile(target)
            },
            onForwardToGroup = { targetId ->
                viewModel.forwardMessagesToGroup(targetId, ids)
                showForwardDialog = null
                selectedMessages = emptySet()
                onOpenGroup(targetId)
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
    replyToName: String? = null,
    replyToContent: String? = null,
    onReplyClick: () -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSenderClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    ChatMessageBubble(
        content = message.content,
        timestamp = message.timestamp,
        isMine = mine,
        senderName = if (!mine) senderDisplayName else null,
        isPinned = message.isPinned,
        isSelected = isSelected,
        replyToName = replyToName,
        replyToContent = replyToContent,
        onReplyClick = onReplyClick,
        onClick = onClick,
        onLongClick = onLongClick,
        onSenderClick = onSenderClick,
        status = {
            if (mine) {
                Text(
                    text = if (message.status == com.example.veiltalk.common.model.MessageStatus.READ) "✓✓" else "✓",
                    fontSize = 10.sp,
                    color = if (message.status == com.example.veiltalk.common.model.MessageStatus.READ) Color(0xFF3B82F6) else Color.Gray
                )
            }
        },
        mediaContent = {
            if (message.messageType != MessageType.TEXT && !message.fileUrl.isNullOrBlank()) {
                EncryptedImage(
                    url = message.fileUrl,
                    mediaKey = message.mediaKey,
                    contentDescription = null,
                    modifier = Modifier
                        .sizeIn(maxWidth = 200.dp, maxHeight = 200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.fileUrl))
                            context.startActivity(intent)
                        },
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    )
}
