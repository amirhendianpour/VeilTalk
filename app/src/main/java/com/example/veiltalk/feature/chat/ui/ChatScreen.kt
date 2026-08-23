package com.example.veiltalk.feature.chat.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.veiltalk.common.model.CallKind
import com.example.veiltalk.common.model.ChatMessage
import com.example.veiltalk.common.model.MessageStatus
import com.example.veiltalk.common.model.MessageType
import com.example.veiltalk.common.ui.components.*

import com.example.veiltalk.common.util.VoiceRecorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    callViewModel: com.example.veiltalk.feature.call.ui.CallViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val voiceRecorder = remember { VoiceRecorder(context) }
    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { viewModel.sendImage(it) }
        }
    }

    var showCameraOptions by remember { mutableStateOf(false) }

    val videoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            tempCameraUri?.let { viewModel.sendFile(it) }
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
    
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceRecorder.start()
            viewModel.startRecording()
        }
    }
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var pendingCallKind by remember { mutableStateOf<CallKind?>(null) }
    var selectedMessages by remember { mutableStateOf(setOf<String>()) }
    var showMessageMenu by remember { mutableStateOf<ChatMessage?>(null) }
    var showDeleteDialog by remember { mutableStateOf<List<String>?>(null) }
    var showForwardDialog by remember { mutableStateOf<List<String>?>(null) }
    var isSearchMode by remember { mutableStateOf(false) }
    var viewingImage by remember { mutableStateOf<ChatMessage?>(null) }

    val isSelectionMode = selectedMessages.isNotEmpty()

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            pendingCallKind?.let { kind ->
                callViewModel.startCall(viewModel.partner, kind)
                pendingCallKind = null
            }
        }
    }

    fun requestCallStart(kind: CallKind) {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (kind == CallKind.VIDEO) needed.add(Manifest.permission.CAMERA)
        
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missing.isEmpty()) {
            callViewModel.startCall(viewModel.partner, kind)
        } else {
            pendingCallKind = kind
            callPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.sendImage(it) }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.sendFile(it) }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
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
                    title = if (isSearchMode) "" else uiState.partnerDisplayName,
                    subtitle = if (isSearchMode) null else when {
                        uiState.isPartnerTyping -> "در حال تایپ..."
                        uiState.isPartnerOnline -> "آنلاین"
                        uiState.partnerLastSeen != null -> "آخرین بازدید: ${com.example.veiltalk.common.util.formatMessageTime(uiState.partnerLastSeen)}"
                        else -> null
                    },
                    imageUrl = if (isSearchMode) null else uiState.partnerProfilePicture,
                    colorSeed = viewModel.partner,
                    onBack = {
                        if (isSearchMode) {
                            isSearchMode = false
                            viewModel.onSearchQueryChange("")
                        } else {
                            onBack()
                        }
                    },
                    onTitleClick = { onOpenProfile(viewModel.partner) },
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
                            IconButton(onClick = { requestCallStart(CallKind.VIDEO) }) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Video Call"
                                )
                            }
                            IconButton(onClick = { requestCallStart(CallKind.AUDIO) }) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Audio Call"
                                )
                            }
                            IconButton(onClick = { /* Menu */ }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More"
                                )
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
                    onOpenCamera = {
                        showCameraOptions = true
                    },
                    onSendSticker = viewModel::sendSticker,
                    onSendGif = viewModel::sendGif,
                    isEditing = uiState.editingMessage != null,
                    replyingMessageContent = uiState.replyingMessage?.content,
                    replyingMessageSender = uiState.replyingMessage?.let { if (it.sender == viewModel.partner) uiState.partnerDisplayName else "شما" },
                    onCancelEdit = viewModel::cancelEditing,
                    onCancelReply = viewModel::cancelReplying,
                    isUploading = isUploading,
                    uploadError = uploadError,
                    onClearUploadError = { viewModel.clearUploadError() },
                    isRecording = isRecording,
                    onStartRecording = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            voiceRecorder.start()
                            viewModel.startRecording()
                        } else {
                            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStopRecording = {
                        val file = voiceRecorder.stop()
                        viewModel.stopRecording(file)
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // WhatsApp Doodle Background Pattern (simplified)
            Canvas(modifier = Modifier.fillMaxSize()) { }
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    val repliedTo = message.replyToId?.let { rid -> uiState.messages.find { it.id == rid } }
                    MessageBubble(
                        message = message,
                        partner = viewModel.partner,
                        isSelected = message.id in selectedMessages,
                        replyToName = repliedTo?.let { if (it.sender == viewModel.partner) uiState.partnerDisplayName else "شما" },
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
                            onOpenProfile(viewModel.partner)
                        }
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
            onEdit = if (msg.sender != viewModel.partner) {
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
        // چک کردن اینکه آیا تمام پیام‌های انتخاب شده متعلق به من هستند یا خیر
        val allMine = uiState.messages.filter { it.id in ids }.all { it.sender != viewModel.partner }

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
                // اگر خواستیم به گروه منتقل شویم:
                // onOpenGroup(targetId) -- باید به ChatScreen پاس داده شود
            }
        )
    }

    viewingImage?.let { msg ->
        msg.fileUrl?.let { url ->
            FullScreenImageViewer(
                url = url,
                mediaKey = msg.mediaKey,
                thumbnailBase64 = msg.content,
                onDismiss = { viewingImage = null }
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    partner: String,
    isSelected: Boolean,
    replyToName: String? = null,
    replyToContent: String? = null,
    onReplyClick: () -> Unit = {},
    onViewImage: (ChatMessage) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSenderClick: () -> Unit
) {
    val mine = message.recipient == partner
    val context = LocalContext.current

    ChatMessageBubble(
        content = if (message.messageType == MessageType.TEXT) message.content else "",
        timestamp = message.timestamp,
        isMine = mine,
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
                    text = if (message.status == MessageStatus.DELIVERED || message.status == MessageStatus.READ) "✓✓" else "✓",
                    fontSize = 10.sp,
                    color = if (message.status == MessageStatus.READ) Color(0xFF3B82F6) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
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
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onViewImage(message)
                                }
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
                            isMine = mine
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
                else -> {}
            }
        }
    )
}