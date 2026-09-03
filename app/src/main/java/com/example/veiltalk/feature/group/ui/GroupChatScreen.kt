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
import androidx.compose.ui.graphics.luminance
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
import com.example.veiltalk.common.util.VoiceRecorder
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
    onOpenChat: (String) -> Unit,
    onOpenGroup: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // بهینه‌سازی: تشخیص حالت تیره یک‌بار در سطح صفحه
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val voiceRecorder = remember { VoiceRecorder(context) }
    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { viewModel.sendImage(it) }
        }
    }

    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = com.example.veiltalk.common.util.CameraCaptureManager.createTempImageUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val recordPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceRecorder.start()
            viewModel.startRecording()
        }
    }

    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.sendImage(it) } }

    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.sendFile(it) } }

    val contactPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            val projection = arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(it.lastPathSegment),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val name = if (nameIndex != -1) cursor.getString(nameIndex) else "Unknown"
                    val number = if (numberIndex != -1) cursor.getString(numberIndex) else ""
                    viewModel.sendContact(name, number)
                }
            }
        }
    }

    val contactPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactPicker.launch(null)
        }
    }

    var selectedMessages by remember { mutableStateOf(setOf<String>()) }
    var showMessageMenu by remember { mutableStateOf<GroupMessage?>(null) }
    var showDeleteDialog by remember { mutableStateOf<List<String>?>(null) }
    var showForwardDialog by remember { mutableStateOf<List<String>?>(null) }
    var isSearchMode by remember { mutableStateOf(false) }
    var viewingImage by remember { mutableStateOf<GroupMessage?>(null) }
    var showCameraOptions by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf<GroupMessage?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val videoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            tempCameraUri?.let { viewModel.sendFile(it) }
        }
    }

    val isSelectionMode = selectedMessages.isNotEmpty()

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
                    onSendContact = {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            contactPicker.launch(null)
                        } else {
                            contactPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                        }
                    },
                    onOpenCamera = {
                        showCameraOptions = true
                    },
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
                    isRecording = isRecording,
                    onStartRecording = {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            voiceRecorder.start()
                            viewModel.startRecording()
                        } else {
                            recordPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStopRecording = {
                        val file = voiceRecorder.stop()
                        viewModel.stopRecording(file)
                    },
                    placeholder = "پیام خود را بنویسید..."
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.pinnedMessages.isNotEmpty()) {
                    PinnedMessagesBar(
                        messages = uiState.pinnedMessages,
                        onMessageClick = { message ->
                            val index = uiState.messages.indexOfFirst { it.id == message.id }
                            if (index != -1) {
                                scope.launch { listState.animateScrollToItem(index) }
                            }
                        },
                        onUnpin = { message ->
                            showPinDialog = message as GroupMessage
                        }
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(top = 8.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        val mine = message.sender == uiState.myUsername
                        val repliedTo = message.replyToId?.let { rid -> uiState.messages.find { it.id == rid } }
                        GroupMessageBubble(
                            message = message,
                            mine = mine,
                            isSelected = message.id in selectedMessages,
                            isDark = isDark,
                            senderDisplayName = userDirectory.getDisplayName(message.sender ?: ""),
                            replyToName = repliedTo?.let { if (it.sender == uiState.myUsername) "شما" else userDirectory.getDisplayName(it.sender ?: "") },
                            replyToContent = repliedTo?.content,
                            onReplyClick = {
                                val index = uiState.messages.indexOfFirst { it.id == message.replyToId }
                                if (index != -1) {
                                    scope.launch { listState.animateScrollToItem(index) }
                                }
                            },
                            onViewImage = { viewingImage = it },
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
            onTogglePin = { 
                showPinDialog = msg
                showMessageMenu = null 
            },
            onForward = {
                showForwardDialog = listOf(msg.id)
                showMessageMenu = null
            },
            onDelete = {
                showDeleteDialog = listOf(msg.id)
                showMessageMenu = null
            },
            onSave = if (!msg.fileUrl.isNullOrBlank()) {
                { viewModel.saveMedia(msg) }
            } else null,
            onReact = { emoji ->
                viewModel.sendReaction(msg.id, emoji)
            }
        )
    }

    if (showCameraOptions) {
        AlertDialog(
            onDismissRequest = { showCameraOptions = false },
            title = { Text("انتخاب دوربین") },
            text = { Text("آیا مایل به گرفتن عکس هستید یا ضبط ویدیو؟") },
            confirmButton = {
                TextButton(onClick = {
                    showCameraOptions = false
                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        val uri = com.example.veiltalk.common.util.CameraCaptureManager.createTempImageUri(context)
                        tempCameraUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                }) {
                    Text("عکس")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCameraOptions = false
                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        val uri = com.example.veiltalk.common.util.CameraCaptureManager.createTempVideoUri(context)
                        tempCameraUri = uri
                        videoLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                }) {
                    Text("ویدیو")
                }
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
                onOpenChat(target)
            },
            onForwardToGroup = { targetId ->
                viewModel.forwardMessagesToGroup(targetId, ids)
                showForwardDialog = null
                selectedMessages = emptySet()
                onOpenGroup(targetId)
            }
        )
    }

    viewingImage?.let { msg ->
        msg.fileUrl?.let { url ->
            FullScreenImageViewer(
                url = url,
                mediaKey = msg.mediaKey,
                thumbnailBase64 = msg.content,
                onDismiss = { viewingImage = null },
                onSave = { viewModel.saveMedia(msg) }
            )
        }
    }

    if (showPinDialog != null) {
        val msg = showPinDialog!!
        PinActionDialog(
            isUnpinning = msg.isPinned,
            onConfirm = { forEveryone ->
                viewModel.togglePin(msg.id, msg.isPinned, forEveryone)
                showPinDialog = null
            },
            onDismiss = { showPinDialog = null },
            isGroup = true
        )
    }
}

@Composable
private fun GroupMessageBubble(
    message: GroupMessage,
    mine: Boolean,
    isSelected: Boolean,
    isDark: Boolean,
    senderDisplayName: String,
    replyToName: String? = null,
    replyToContent: String? = null,
    onReplyClick: () -> Unit = {},
    onViewImage: (GroupMessage) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSenderClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    ChatMessageBubble(
        content = if (message.messageType == MessageType.TEXT) message.content else "",
        timestamp = message.timestamp,
        isMine = mine,
        isDark = isDark,
        senderName = if (!mine) senderDisplayName else null,
        isPinned = message.isPinned,
        isSelected = isSelected,
        isForwarded = message.isForwarded,
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
        reactionsContent = {
            ReactionsRow(reactions = message.reactions, isMine = mine)
        },
        mediaContent = {
            when (message.messageType) {
                MessageType.IMAGE -> {
                    if (!message.fileUrl.isNullOrBlank()) {
                        EncryptedImage(
                            url = message.fileUrl,
                            mediaKey = message.mediaKey,
                            thumbnailBase64 = message.content,
                            contentDescription = null,
                            modifier = Modifier
                                .sizeIn(maxWidth = 200.dp, maxHeight = 200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onViewImage(message) },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                MessageType.VOICE -> {
                    if (!message.fileUrl.isNullOrBlank()) {
                        VoiceMessagePlayer(
                            url = message.fileUrl,
                            mediaKey = message.mediaKey,
                            isMine = mine
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                MessageType.FILE -> {
                    if (!message.fileUrl.isNullOrBlank()) {
                        FileMessageItem(
                            url = message.fileUrl,
                            mediaKey = message.mediaKey,
                            fileName = message.content.ifBlank { "فایل پیوست" },
                            isMine = mine,
                            onLongClick = onLongClick
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                MessageType.GIF, MessageType.STICKER -> {
                    if (!message.fileUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = message.fileUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(if (message.messageType == MessageType.STICKER) 120.dp else 200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                MessageType.CONTACT -> {
                    val parts = message.content.split("\n")
                    val name = parts.getOrNull(0) ?: "مخاطب"
                    val phone = parts.getOrNull(1) ?: ""
                    ContactMessageItem(name = name, phoneNumber = phone, isMine = mine)
                    Spacer(Modifier.height(4.dp))
                }
                else -> {}
            }
        }
    )
}
