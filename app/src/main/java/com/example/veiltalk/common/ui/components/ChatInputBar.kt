package com.example.veiltalk.common.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachImage: () -> Unit = {},
    onAttachFile: () -> Unit = {},
    onOpenCamera: () -> Unit = {}, // اضافه شد
    onSendSticker: (String) -> Unit = {},
    onSendGif: (String) -> Unit = {},
    isEditing: Boolean = false,
    replyingMessageContent: String? = null,
    replyingMessageSender: String? = null,
    onCancelEdit: () -> Unit = {},
    onCancelReply: () -> Unit = {},
    isUploading: Boolean = false,
    uploadError: String? = null,
    onClearUploadError: () -> Unit = {},
    isRecording: Boolean = false,
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    placeholder: String = "پیام..."
) {
    var showAttachMenu by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordSeconds = 0
            while (isRecording) {
                delay(1000)
                recordSeconds++
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val inputBackgroundColor = MaterialTheme.colorScheme.surface

    // برای جلوگیری از Capture شدن مقادیر قدیمی در pointerInput
    val currentOnSendMessage by rememberUpdatedState(onSendMessage)
    val currentOnStartRecording by rememberUpdatedState(onStartRecording)
    val currentOnStopRecording by rememberUpdatedState(onStopRecording)
    val currentIsTextEmpty by rememberUpdatedState(value.isEmpty())

    Column(modifier = Modifier.background(backgroundColor)) {
        if (isEditing) EditHeader(primaryColor, onCancelEdit)
        if (replyingMessageContent != null) ReplyHeader(primaryColor, replyingMessageSender, replyingMessageContent, onCancelReply)
        if (isUploading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = primaryColor)
        if (uploadError != null) UploadErrorHeader(uploadError, onClearUploadError)

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // کادر ورودی یا وضعیت ضبط
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(inputBackgroundColor, RoundedCornerShape(24.dp))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (!isRecording) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            if (showEmojiPicker) {
                                showEmojiPicker = false
                                keyboardController?.show()
                            } else {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                showEmojiPicker = true
                            }
                        }) {
                            Icon(imageVector = if (showEmojiPicker) Icons.Default.Keyboard else Icons.Default.SentimentSatisfiedAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }

                        BasicTextField(
                            value = value,
                            onValueChange = { onValueChange(it); if (showEmojiPicker) showEmojiPicker = false },
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            decorationBox = { innerTextField ->
                                if (value.isEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 16.sp)
                                innerTextField()
                            }
                        )

                        IconButton(onClick = { showAttachMenu = true }, enabled = !isUploading) {
                            Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }

                        IconButton(onClick = onOpenCamera, enabled = !isUploading) {
                            Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }

                        AttachmentMenu(showAttachMenu, { showAttachMenu = false }, onAttachImage, onAttachFile)
                    }
                } else {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = String.format("%02d:%02d", recordSeconds / 60, recordSeconds % 60), color = Color.Red, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        Text("در حال ضبط...", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // دکمه ارسال/ضبط (ثابت در جای خود برای جلوگیری از قطع لمس)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) Color.Red else primaryColor)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { 
                                if (!currentIsTextEmpty) {
                                    currentOnSendMessage()
                                }
                            },
                            onPress = {
                                if (currentIsTextEmpty) {
                                    try {
                                        currentOnStartRecording()
                                        awaitRelease()
                                    } finally {
                                        currentOnStopRecording()
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (value.isEmpty()) Icons.Default.Mic else Icons.Default.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (showEmojiPicker) {
            EmojiStickerGifPicker(
                onEmojiSelected = { emoji -> onValueChange(value + emoji) },
                onStickerSelected = onSendSticker,
                onGifSelected = onSendGif,
                onBackspace = { if (value.isNotEmpty()) onValueChange(value.dropLast(1)) }
            )
        }
    }
}

@Composable
private fun EditHeader(primaryColor: Color, onCancel: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(primaryColor.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Edit, null, tint = primaryColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("ویرایش پیام", color = primaryColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        IconButton(onClick = onCancel, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
    }
}

@Composable
private fun ReplyHeader(primaryColor: Color, sender: String?, content: String, onCancel: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(primaryColor.copy(alpha = 0.05f)).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(4.dp).height(32.dp).background(primaryColor, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(sender ?: "پیام", color = primaryColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(content, color = Color.Gray, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
    }
}

@Composable
private fun UploadErrorHeader(message: String, onClear: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp); TextButton(onClick = onClear) { Text("باشه") }
    }
}

@Composable
private fun AttachmentMenu(expanded: Boolean, onDismiss: () -> Unit, onImage: () -> Unit, onFile: () -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("گالری") }, onClick = { onDismiss(); onImage() })
        DropdownMenuItem(text = { Text("فایل") }, onClick = { onDismiss(); onFile() })
    }
}

@Composable
fun EmojiStickerGifPicker(onEmojiSelected: (String) -> Unit, onStickerSelected: (String) -> Unit, onGifSelected: (String) -> Unit, onBackspace: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxWidth().height(320.dp).background(MaterialTheme.colorScheme.surface)) {
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("ایموجی") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("استیکر") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("گیف") })
            IconButton(onClick = onBackspace) { Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> EmojiGrid(onEmojiSelected)
                1 -> StickerGrid(onStickerSelected)
                2 -> GifGrid(onGifSelected)
            }
        }
    }
}

@Composable
fun EmojiGrid(onEmojiSelected: (String) -> Unit) {
    val emojis = listOf("😊", "😂", "🥰", "😍", "😒", "😭", "😘", "🙌", "👍", "🔥", "❤️", "✨", "🤔", "😎", "🥺", "🙏", "👏", "🎉", "🤣", "😅", "😋", "😗", "😙", "😚", "☺️", "🙂", "🤗", "🤩", "🤨", "😐", "😑", "😶", "🙄", "😏", "😣", "😥", "😮", "🤐", "😯", "😪", "😫")
    LazyVerticalGrid(columns = GridCells.Adaptive(48.dp), contentPadding = PaddingValues(8.dp)) {
        items(emojis) { emoji ->
            Box(modifier = Modifier.size(48.dp).clickable { onEmojiSelected(emoji) }, contentAlignment = Alignment.Center) { Text(emoji, fontSize = 24.sp) }
        }
    }
}

@Composable
fun StickerGrid(onStickerSelected: (String) -> Unit) {
    val stickers = listOf("https://cdn.pixabay.com/photo/2017/02/10/12/12/volunteer-2055010_1280.png", "https://cdn.pixabay.com/photo/2016/03/31/19/58/avatar-1295429_1280.png", "https://cdn.pixabay.com/photo/2016/11/18/23/38/child-1837375_1280.png")
    LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(8.dp)) {
        items(stickers) { url ->
            AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(80.dp).clickable { onStickerSelected(url) }.padding(8.dp))
        }
    }
}

@Composable
fun GifGrid(onGifSelected: (String) -> Unit) {
    val gifs = listOf("https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJidmNueGZyc2V6bmN4Z3V4Z3V4Z3V4Z3V4JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/3o7TKSjPqcKGRZaO3u/giphy.gif", "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJidmNueGZyc2V6bmN4Z3V4Z3V4Z3V4Z3V4JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/l0HlHFRbmaZtBRhXG/giphy.gif")
    LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(8.dp)) {
        items(gifs) { url ->
            AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxWidth().height(100.dp).clickable { onGifSelected(url) }.padding(4.dp))
        }
    }
}
