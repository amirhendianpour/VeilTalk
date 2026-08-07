package com.example.veiltalk.feature.chat.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.veiltalk.common.model.ChatMessage
import com.example.veiltalk.common.model.MessageStatus
import com.example.veiltalk.common.model.MessageType
import com.example.veiltalk.common.ui.components.AvatarView
import com.example.veiltalk.common.util.formatMessageTime

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

    var showAttachMenu by remember { mutableStateOf(false) }

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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarView(
                            name = uiState.partnerDisplayName,
                            imageUrl = uiState.partnerProfilePicture,
                            size = 36.dp,
                            colorSeed = viewModel.partner
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.partnerDisplayName)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        callViewModel.startCall(viewModel.partner, com.example.veiltalk.common.model.CallKind.AUDIO)
                    }) {
                        Text("📞")
                    }
                    IconButton(onClick = {
                        callViewModel.startCall(viewModel.partner, com.example.veiltalk.common.model.CallKind.VIDEO)
                    }) {
                        Text("🎥")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                if (uiState.isPartnerTyping) {
                    Text(
                        "${uiState.partnerDisplayName} در حال تایپ است...",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                if (isUploading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
                        .background(Color(0xFFF3F4F6))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(onClick = { showAttachMenu = true }, enabled = !isUploading) {
                            Text("📎", fontSize = 20.sp)
                        }
                        DropdownMenu(
                            expanded = showAttachMenu,
                            onDismissRequest = { showAttachMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("ارسال عکس") },
                                onClick = {
                                    showAttachMenu = false
                                    imagePicker.launch("image/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("ارسال فایل") },
                                onClick = {
                                    showAttachMenu = false
                                    filePicker.launch("*/*")
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = viewModel::onInputChange,
                        placeholder = { Text("پیام خود را بنویسید...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = viewModel::sendMessage,
                        enabled = inputText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "ارسال")
                    }
                }
            }
        }
    ) { padding ->
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
                MessageBubble(message = message, partner = viewModel.partner)
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