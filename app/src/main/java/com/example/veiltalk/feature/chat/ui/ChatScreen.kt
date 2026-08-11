package com.example.veiltalk.feature.chat.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.veiltalk.common.model.CallKind
import com.example.veiltalk.common.model.ChatMessage
import com.example.veiltalk.common.model.MessageStatus
import com.example.veiltalk.common.model.MessageType
import com.example.veiltalk.common.ui.components.AvatarView
import com.example.veiltalk.common.util.formatMessageTime
import com.example.veiltalk.ui.theme.WaChatBg
import com.example.veiltalk.ui.theme.WaLightGreen
import com.example.veiltalk.ui.theme.WaTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    callViewModel: com.example.veiltalk.feature.call.ui.CallViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var showAttachMenu by remember { mutableStateOf(false) }
    var pendingCallKind by remember { mutableStateOf<CallKind?>(null) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WaTeal,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { /* Partner Profile */ }
                    ) {
                        AvatarView(
                            name = uiState.partnerDisplayName,
                            imageUrl = uiState.partnerProfilePicture,
                            size = 36.dp,
                            colorSeed = viewModel.partner
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                uiState.partnerDisplayName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.isPartnerTyping) {
                                Text(
                                    "در حال تایپ...",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = { requestCallStart(CallKind.VIDEO) }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call")
                    }
                    IconButton(onClick = { requestCallStart(CallKind.AUDIO) }) {
                        Icon(Icons.Default.Call, contentDescription = "Audio Call")
                    }
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(WaChatBg)) {
                if (isUploading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = WaTeal)
                }
                if (uploadError != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEE2E2))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(uploadError!!, color = Color(0xFFDC2626), fontSize = 12.sp)
                        TextButton(onClick = { viewModel.clearUploadError() }) { Text("باشه") }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White, RoundedCornerShape(24.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Emoji */ }) {
                            Text("😊", fontSize = 20.sp)
                        }
                        
                        Box(modifier = Modifier.weight(1f)) {
                            BasicTextField(
                                value = inputText,
                                onValueChange = viewModel::onInputChange,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                                decorationBox = { innerTextField ->
                                    if (inputText.isEmpty()) {
                                        Text("پیام...", color = Color.Gray, fontSize = 16.sp)
                                    }
                                    innerTextField()
                                }
                            )
                        }

                        IconButton(onClick = { showAttachMenu = true }, enabled = !isUploading) {
                            Icon(Icons.Default.Add, contentDescription = "پیوست", tint = Color.Gray)
                        }
                        
                        DropdownMenu(
                            expanded = showAttachMenu,
                            onDismissRequest = { showAttachMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("گالری") },
                                onClick = {
                                    showAttachMenu = false
                                    imagePicker.launch("image/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("فایل") },
                                onClick = {
                                    showAttachMenu = false
                                    filePicker.launch("*/*")
                                }
                            )
                        }
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    FloatingActionButton(
                        onClick = { 
                            if (inputText.isNotBlank()) viewModel.sendMessage() 
                            else { /* Voice record placeholder */ }
                        },
                        containerColor = WaTeal,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                        elevation = FloatingActionButtonDefaults.elevation(2.dp)
                    ) {
                        Icon(
                            if (inputText.isBlank()) Icons.Default.Mic else Icons.Default.Send,
                            contentDescription = "ارسال",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(WaChatBg)
        ) {
            // WhatsApp Doodle Background Pattern (simplified)
            Canvas(modifier = Modifier.fillMaxSize()) {
                // We could draw some faint patterns here if we wanted to be fancy
            }
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    MessageBubble(message = message, partner = viewModel.partner)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, partner: String) {
    val mine = message.recipient == partner
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (mine) 12.dp else 2.dp,
                    bottomEnd = if (mine) 2.dp else 12.dp
                ))
                .background(
                    color = if (mine) WaLightGreen else Color.White,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (mine) 12.dp else 2.dp,
                        bottomEnd = if (mine) 2.dp else 12.dp
                    )
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .widthIn(max = 300.dp)
        ) {
            Column {
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
                            if (message.content.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(message.content)
                            }
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
                        }
                    }
                    else -> {
                        Text(message.content)
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        formatMessageTime(message.timestamp),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    if (mine) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (message.status == MessageStatus.DELIVERED || message.status == MessageStatus.READ) "✓✓" else "✓",
                            fontSize = 10.sp,
                            color = if (message.status == MessageStatus.READ) Color(0xFF3B82F6) else Color.Gray
                        )
                    }
                }
            }
        }
    }
}