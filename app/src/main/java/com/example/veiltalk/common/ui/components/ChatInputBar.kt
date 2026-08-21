package com.example.veiltalk.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachImage: () -> Unit = {},
    onAttachFile: () -> Unit = {},
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
    placeholder: String = "پیام..."
) {
    var showAttachMenu by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val inputBackgroundColor = MaterialTheme.colorScheme.surface

    Column(modifier = Modifier.background(backgroundColor)) {
        if (isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryColor.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("ویرایش پیام", color = primaryColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onCancelEdit, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "انصراف", tint = Color.Gray)
                }
            }
        }
        
        if (replyingMessageContent != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryColor.copy(alpha = 0.05f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(32.dp)
                        .background(primaryColor, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = replyingMessageSender ?: "پیام",
                        color = primaryColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = replyingMessageContent,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onCancelReply, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "لغو ریپلای", tint = Color.Gray)
                }
            }
        }

        if (isUploading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = primaryColor)
        }
        if (uploadError != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(uploadError, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                TextButton(onClick = onClearUploadError) { Text("باشه") }
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
                    .background(inputBackgroundColor, RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    Icon(
                        imageVector = if (showEmojiPicker) Icons.Default.Keyboard else Icons.Default.SentimentSatisfiedAlt,
                        contentDescription = "ایموجی",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    BasicTextField(
                        value = value,
                        onValueChange = { 
                            onValueChange(it)
                            // اگر کاربر شروع به تایپ کرد، پنل ایموجی را ببند (رفتار واتس‌اپ)
                            if (showEmojiPicker) showEmojiPicker = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { innerTextField ->
                            if (value.isEmpty()) {
                                Text(placeholder, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 16.sp)
                            }
                            innerTextField()
                        }
                    )
                }

                IconButton(onClick = { showAttachMenu = true }, enabled = !isUploading) {
                    Icon(Icons.Default.AttachFile, contentDescription = "پیوست", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }

                DropdownMenu(
                    expanded = showAttachMenu,
                    onDismissRequest = { showAttachMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("گالری") },
                        onClick = {
                            showAttachMenu = false
                            onAttachImage()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("فایل") },
                        onClick = {
                            showAttachMenu = false
                            onAttachFile()
                        }
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            FloatingActionButton(
                onClick = {
                    if (value.isNotBlank()) onSendMessage()
                    else { /* Voice record placeholder */ }
                },
                containerColor = primaryColor,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                elevation = FloatingActionButtonDefaults.elevation(2.dp)
            ) {
                Icon(
                    if (value.isBlank()) Icons.Default.Mic else Icons.Default.Send,
                    contentDescription = "ارسال",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (showEmojiPicker) {
            EmojiStickerGifPicker(
                onEmojiSelected = { emoji -> onValueChange(value + emoji) },
                onStickerSelected = { stickerUrl ->
                    onSendSticker(stickerUrl)
                },
                onGifSelected = { gifUrl ->
                    onSendGif(gifUrl)
                },
                onBackspace = {
                    if (value.isNotEmpty()) {
                        onValueChange(value.dropLast(1))
                    }
                }
            )
        }
    }
}

@Composable
fun EmojiStickerGifPicker(
    onEmojiSelected: (String) -> Unit,
    onStickerSelected: (String) -> Unit,
    onGifSelected: (String) -> Unit,
    onBackspace: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Column(modifier = Modifier.fillMaxWidth().height(320.dp).background(MaterialTheme.colorScheme.surface)) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("ایموجی") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("استیکر") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("گیف") })
            
            // دکمه پاک کردن در کنار تب‌ها
            IconButton(onClick = onBackspace) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "پاک کردن", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
    val emojis = listOf(
        "😊", "😂", "🥰", "😍", "😒", "😭", "😘", "🙌", "👍", "🔥", "❤️", "✨", "🤔", "😎", "🥺", "🙏",
        "👏", "🎉", "🤣", "😂", "😅", "😊", "😋", "😎", "😍", "😘", "🥰", "😗", "😙", "😚", "☺️", "🙂",
        "🤗", "🤩", "🤔", "🤨", "😐", "😑", "😶", "🙄", "😏", "😣", "😥", "😮", "🤐", "😯", "😪", "😫"
    )
    LazyVerticalGrid(columns = GridCells.Adaptive(48.dp), contentPadding = PaddingValues(8.dp)) {
        items(emojis) { emoji ->
            Box(modifier = Modifier.size(48.dp).clickable { onEmojiSelected(emoji) }, contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 24.sp)
            }
        }
    }
}

@Composable
fun StickerGrid(onStickerSelected: (String) -> Unit) {
    val stickers = listOf(
        "https://cdn.pixabay.com/photo/2017/02/10/12/12/volunteer-2055010_1280.png",
        "https://cdn.pixabay.com/photo/2016/03/31/19/58/avatar-1295429_1280.png",
        "https://cdn.pixabay.com/photo/2016/11/18/23/38/child-1837375_1280.png",
        "https://cdn.pixabay.com/photo/2017/02/10/12/12/volunteer-2055010_1280.png"
    )
    LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(8.dp)) {
        items(stickers) { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.size(80.dp).clickable { onStickerSelected(url) }.padding(8.dp)
            )
        }
    }
}

@Composable
fun GifGrid(onGifSelected: (String) -> Unit) {
    val gifs = listOf(
        "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJidmNueGZyc2V6bmN4Z3V4Z3V4Z3V4Z3V4Z3V4Z3V4JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/3o7TKSjPqcKGRZaO3u/giphy.gif",
        "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJidmNueGZyc2V6bmN4Z3V4Z3V4Z3V4Z3V4Z3V4Z3V4JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/l0HlHFRbmaZtBRhXG/giphy.gif"
    )
    LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(8.dp)) {
        items(gifs) { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(100.dp).clickable { onGifSelected(url) }.padding(4.dp)
            )
        }
    }
}
