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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    isUploading: Boolean = false,
    uploadError: String? = null,
    onClearUploadError: () -> Unit = {},
    placeholder: String = "پیام..."
) {
    var showAttachMenu by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val inputBackgroundColor = MaterialTheme.colorScheme.surface

    if (showEmojiPicker) {
        EmojiStickerGifPicker(
            onEmojiSelected = { emoji -> onValueChange(value + emoji) },
            onStickerSelected = { stickerUrl ->
                onSendSticker(stickerUrl)
                showEmojiPicker = false
            },
            onGifSelected = { gifUrl ->
                onSendGif(gifUrl)
                showEmojiPicker = false
            },
            onDismiss = { showEmojiPicker = false }
        )
    }

    Column(modifier = Modifier.background(backgroundColor)) {
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
                IconButton(onClick = { showEmojiPicker = true }) {
                    Text("😊", fontSize = 20.sp)
                }

                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
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
                    Icon(Icons.Default.Add, contentDescription = "پیوست", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiStickerGifPicker(
    onEmojiSelected: (String) -> Unit,
    onStickerSelected: (String) -> Unit,
    onGifSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        var selectedTab by remember { mutableIntStateOf(0) }
        
        Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("ایموجی") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("استیکر") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("گیف") })
            }
            
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
    val emojis = listOf("😊", "😂", "🥰", "😍", "😒", "😭", "😘", "🙌", "👍", "🔥", "❤️", "✨", "🤔", "😎", "🥺", "🙏")
    LazyVerticalGrid(columns = GridCells.Adaptive(48.dp), contentPadding = PaddingValues(16.dp)) {
        items(emojis) { emoji ->
            Box(modifier = Modifier.size(48.dp).clickable { onEmojiSelected(emoji) }, contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 24.sp)
            }
        }
    }
}

@Composable
fun StickerGrid(onStickerSelected: (String) -> Unit) {
    // Mock sticker URLs
    val stickers = listOf(
        "https://cdn.pixabay.com/photo/2017/02/10/12/12/volunteer-2055010_1280.png",
        "https://cdn.pixabay.com/photo/2016/03/31/19/58/avatar-1295429_1280.png",
        "https://cdn.pixabay.com/photo/2016/11/18/23/38/child-1837375_1280.png"
    )
    LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(16.dp)) {
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
    // Mock GIF URLs (using static images for demo if needed, but Coil handles GIFs)
    val gifs = listOf(
        "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJidmNueGZyc2V6bmN4Z3V4Z3V4Z3V4Z3V4Z3V4Z3V4Z3V4JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/3o7TKSjPqcKGRZaO3u/giphy.gif",
        "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNHJidmNueGZyc2V6bmN4Z3V4Z3V4Z3V4Z3V4Z3V4Z3V4Z3V4JmVwPXYxX2ludGVybmFsX2dpZl9ieV9pZCZjdD1n/l0HlHFRbmaZtBRhXG/giphy.gif"
    )
    LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp)) {
        items(gifs) { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(100.dp).clickable { onGifSelected(url) }.padding(4.dp)
            )
        }
    }
}
