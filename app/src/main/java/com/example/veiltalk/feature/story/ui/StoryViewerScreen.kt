package com.example.veiltalk.feature.story.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.veiltalk.common.ui.components.AvatarView
import com.example.veiltalk.feature.story.data.dto.StoryResponseDto
import com.example.veiltalk.feature.story.data.dto.StoryViewerInfoDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerScreen(
    stories: List<StoryResponseDto>,
    myUsername: String,
    onClose: () -> Unit,
    onUserClick: (String) -> Unit = {},
    onReplyStory: (String, String) -> Unit = { _, _ -> },
    onReactStory: (Long, String) -> Unit = { _, _ -> },
    onGetViewers: (Long, (List<StoryViewerInfoDto>) -> Unit) -> Unit = { _, _ -> }
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentStory = stories[currentIndex]
    
    var isLiked by remember(currentStory.id, currentStory.liked) { mutableStateOf(currentStory.liked) }
    var replyText by remember { mutableStateOf("") }
    var isPaused by remember { mutableStateOf(false) }
    var isTextFieldFocused by remember { mutableStateOf(false) }
    var showQuickReactions by remember { mutableStateOf(false) }
    var showViewersBottomSheet by remember { mutableStateOf(false) }
    var viewersList by remember { mutableStateOf<List<StoryViewerInfoDto>>(emptyList()) }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(currentStory.id) {
        onReactStory(currentStory.id, "VIEW_LOG")
        // Reset progress smoothly on story change
        progress.snapTo(0f)
    }

    LaunchedEffect(showViewersBottomSheet, currentStory.id) {
        if (showViewersBottomSheet) {
            isPaused = true
            onGetViewers(currentStory.id) { list ->
                viewersList = list
            }
        }
    }

    // Effectively manage overall pause state combining textfield focus or sheet open
    val effectivePause = isPaused || isTextFieldFocused || showViewersBottomSheet

    LaunchedEffect(currentIndex, effectivePause) {
        if (effectivePause) {
            progress.stop()
        } else {
            val remainingTime = ((1f - progress.value) * 5000).toInt()
            if (remainingTime > 0) {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = remainingTime, easing = LinearEasing)
                )
                if (currentIndex < stories.size - 1) {
                    currentIndex++
                } else {
                    onClose()
                }
            } else {
                // If remaining time is 0 or less, move forward
                if (currentIndex < stories.size - 1) {
                    currentIndex++
                } else {
                    onClose()
                }
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {
        // Main Interaction and Image View Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentIndex) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            val success = tryAwaitRelease()
                            // Only unpause if we didn't open a sheet or text box
                            isPaused = false
                        },
                        onTap = { offset ->
                            // Don't change story if user is typing a reply
                            if (!isTextFieldFocused) {
                                if (offset.x < size.width / 3) {
                                    if (currentIndex > 0) currentIndex-- else onClose()
                                } else {
                                    if (currentIndex < stories.size - 1) currentIndex++ else onClose()
                                }
                            }
                        }
                    )
                }
        ) {
            AsyncImage(
                model = currentStory.mediaUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Overlay UI Content Layers
        Column(modifier = Modifier.fillMaxSize()) {
            // Progress Indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { index, _ ->
                    LinearProgressIndicator(
                        progress = { 
                            when {
                                index < currentIndex -> 1f
                                index == currentIndex -> progress.value
                                else -> 0f
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                }
            }

            // Header (Avatar, Name, Close)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { 
                            onClose()
                            onUserClick(currentStory.creatorUsername) 
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarView(
                        name = currentStory.creatorDisplayName,
                        imageUrl = currentStory.creatorProfilePicture,
                        size = 36.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(currentStory.creatorDisplayName, color = Color.White, fontSize = 14.sp)
                        Text("به تازگی", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }
                
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }

            Spacer(Modifier.weight(1f))

            // Caption
            if (!currentStory.caption.isNullOrBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        currentStory.caption,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Interactive Bottom Bar Layer
            if (currentStory.creatorUsername == myUsername) {
                // Own story: Viewers analytics layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { showViewersBottomSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("👁️ مشاهده بازدیدکنندگان", color = Color.White, fontSize = 14.sp)
                    }
                }
            } else {
                // Peer story: Replies, reactions, and likes layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { 
                            replyText = it
                            showQuickReactions = it.isEmpty()
                        },
                        placeholder = { Text("ارسال پیام...", color = Color.LightGray, fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = CircleShape,
                        singleLine = true,
                        trailingIcon = {
                            if (replyText.isNotBlank()) {
                                IconButton(onClick = {
                                    val formattedReply = "💬 پاسخ به استوری شما:\n\"$replyText\""
                                    onReplyStory(currentStory.creatorUsername, formattedReply)
                                    replyText = ""
                                    isTextFieldFocused = false
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
                                }
                            }
                        },
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect { interaction ->
                                    when (interaction) {
                                        is androidx.compose.foundation.interaction.FocusInteraction.Focus -> {
                                            isTextFieldFocused = true
                                        }
                                        is androidx.compose.foundation.interaction.FocusInteraction.Unfocus -> {
                                            isTextFieldFocused = false
                                        }
                                    }
                                }
                            }
                        }
                    )

                    IconButton(onClick = { showQuickReactions = !showQuickReactions }) {
                        Text("😊", fontSize = 22.sp)
                    }

                    IconButton(onClick = { 
                        isLiked = !isLiked
                        onReactStory(currentStory.id, if (isLiked) "❤️" else "UNLIKE")
                    }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isLiked) Color.Red else Color.White
                        )
                    }
                }
            }
        }

        // Quick Reactions Overlay Panel
        if (showQuickReactions) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures(onTap = { showQuickReactions = false }) }
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 90.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        listOf("❤️", "🙌", "😂", "😮", "😢", "👏", "🔥", "🎉").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 28.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onReactStory(currentStory.id, emoji)
                                        showQuickReactions = false
                                        val quickReactionMessage = "📊 واکنش $emoji به استوری شما"
                                        onReplyStory(currentStory.creatorUsername, quickReactionMessage)
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Viewers Bottom Sheet Content Layout
        if (showViewersBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showViewersBottomSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "آمار بازدیدکنندگان (${viewersList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (viewersList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("هنوز کسی این استوری را ندیده است.", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(viewersList) { viewer ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarView(
                                        name = viewer.displayName,
                                        imageUrl = viewer.profilePictureUrl,
                                        size = 40.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = viewer.displayName, fontSize = 14.sp)
                                        Text(text = "@${viewer.username}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    
                                    if (viewer.reactionEmoji != null && viewer.reactionEmoji != "VIEW_LOG") {
                                        Text(text = viewer.reactionEmoji, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                                    }
                                    
                                    if (viewer.liked) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = Color.Red,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
