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
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var pendingCallKind by remember { mutableStateOf<CallKind?>(null) }
    var selectedMessages by remember { mutableStateOf(setOf<String>()) }
    var showMessageMenu by remember { mutableStateOf<ChatMessage?>(null) }

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
                    title = uiState.partnerDisplayName,
                    subtitle = when {
                        uiState.isPartnerTyping -> "در حال تایپ..."
                        uiState.isPartnerOnline -> "آنلاین"
                        uiState.partnerLastSeen != null -> "آخرین بازدید: ${com.example.veiltalk.common.util.formatMessageTime(uiState.partnerLastSeen)}"
                        else -> null
                    },
                    imageUrl = uiState.partnerProfilePicture,
                    colorSeed = viewModel.partner,
                    onBack = onBack,
                    onTitleClick = { onOpenProfile(viewModel.partner) },
                    actions = {
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
                    onCancelEdit = viewModel::cancelEditing,
                    isUploading = isUploading,
                    uploadError = uploadError,
                    onClearUploadError = { viewModel.clearUploadError() }
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
                    MessageBubble(
                        message = message,
                        partner = viewModel.partner,
                        isSelected = message.id in selectedMessages,
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
                // Forward logic
            },
            onDelete = {
                viewModel.deleteMessages(listOf(msg.id))
            }
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    partner: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSenderClick: () -> Unit
) {
    val mine = message.recipient == partner
    val context = LocalContext.current

    ChatMessageBubble(
        content = message.content,
        timestamp = message.timestamp,
        isMine = mine,
        isPinned = message.isPinned,
        isSelected = isSelected,
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
                        AsyncImage(
                            model = message.fileUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.fileUrl))
                                    context.startActivity(intent)
                                }
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                MessageType.FILE -> {
                    if (!message.fileUrl.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.fileUrl))
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text("📄", fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                message.content.ifBlank { "فایل پیوست" },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
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