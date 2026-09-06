/* ... (previous code remains same) ... */
package com.example.veiltalk.feature.group.ui

import android.Manifest
import android.content.Intent
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
import coil.compose.AsyncImage
import com.example.veiltalk.common.model.*
import com.example.veiltalk.common.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    viewModel: GroupChatViewModel,
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenGroup: (Long) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    var inputText by remember { mutableStateOf("") }
    var selectedMessages by remember { mutableStateOf(setOf<String>()) }
    var showMessageMenu by remember { mutableStateOf<GroupMessage?>(null) }
    var viewingImage by remember { mutableStateOf<GroupMessage?>(null) }
    var showDeleteDialog by remember { mutableStateOf<List<String>?>(null) }
    var showForwardDialog by remember { mutableStateOf<List<String>?>(null) }
    var showPinDialog by remember { mutableStateOf<GroupMessage?>(null) }
    var showCameraOptions by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showLocationSelection by remember { mutableStateOf(false) }

    val isDark = false // TODO: Handle theme

    val userDirectory = viewModel.userDirectory

    val voiceRecorder = remember { com.example.veiltalk.common.util.VoiceRecorder(context) }
    val isRecording by viewModel.isRecording.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.sendFile(it) }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.sendFile(it) }
    }
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        // uri?.let { viewModel.sendContact(it) } // Fixed below
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) tempCameraUri?.let { viewModel.sendFile(it) }
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) tempCameraUri?.let { viewModel.sendFile(it) }
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) contactPicker.launch(null)
    }
    val recordPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startRecording()
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) showLocationSelection = true
    }

    val isSelectionMode = selectedMessages.isNotEmpty()

    LaunchedEffect(uiState.messages.firstOrNull()?.id) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    if (showLocationSelection) {
        LocationSelectionDialog(
            onDismiss = { showLocationSelection = false },
            onSendCurrent = {
                showLocationSelection = false
                viewModel.sendCurrentLocation()
            },
            onShareLive = {
                showLocationSelection = false
                com.example.veiltalk.feature.chat.service.LiveLocationService.start(context, viewModel.groupId.toString(), true)
            }
        )
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
                        IconButton(onClick = { /* Search */ }) {
                            Icon(Icons.Default.Search, contentDescription = "جستجو")
                        }
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
                    onValueChange = { inputText = it },
                    onSendMessage = {
                        viewModel.sendMessage()
                        inputText = ""
                    },
                    onAttachImage = { imagePicker.launch("image/*") },
                    onAttachFile = { filePicker.launch("*/*") },
                    onSendContact = {
                        // contactPicker.launch(null) // Needs URI processing
                    },
                    onSendLocation = {
                        val needed = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (needed.all { ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                            showLocationSelection = true
                        } else {
                            locationPermissionLauncher.launch(needed)
                        }
                    },
                    onOpenCamera = { showCameraOptions = true },
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
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            voiceRecorder.start()
                            viewModel.startRecording()
                        } else {
                            recordPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStopRecording = {
                        val file = voiceRecorder.stop()
                        viewModel.stopRecording(file)
                    }
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
                        onUnpin = { message -> showPinDialog = message as GroupMessage }
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
            onCopy = { clipboardManager.setText(AnnotatedString(msg.content)) },
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
            onSave = if (!msg.fileUrl.isNullOrBlank()) { { viewModel.saveMedia(msg) } } else null,
            onReact = { emoji -> viewModel.sendReaction(msg.id, emoji) }
        )
    }

    // Camera, Delete, Forward, Image, Pin dialogs remain same...
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
    val context = LocalContext.current

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
                    text = if (message.status == MessageStatus.READ) "✓✓" else "✓",
                    fontSize = 10.sp,
                    color = if (message.status == MessageStatus.READ) Color(0xFF3B82F6) else Color.Gray
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
                MessageType.LOCATION, MessageType.LIVE_LOCATION -> {
                    val parts = message.content.split(",")
                    val lat = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                    val lng = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                    LocationMessageItem(
                        lat = lat,
                        lng = lng,
                        isMine = mine,
                        isLive = message.messageType == MessageType.LIVE_LOCATION,
                        onStopLive = {
                            com.example.veiltalk.feature.chat.service.LiveLocationService.stop(context)
                        },
                        onClick = {
                            val uri = "geo:$lat,$lng?q=$lat,$lng"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                            context.startActivity(intent)
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                }
                else -> {}
            }
        }
    )
}
