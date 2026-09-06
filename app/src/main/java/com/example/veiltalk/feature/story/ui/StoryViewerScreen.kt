package com.example.veiltalk.feature.story.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import kotlinx.coroutines.delay

@Composable
fun StoryViewerScreen(
    stories: List<StoryResponseDto>,
    onClose: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentStory = stories[currentIndex]
    
    // Progress for current segment
    val progress = remember { Animatable(0f) }

    LaunchedEffect(currentIndex) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
        )
        if (currentIndex < stories.size - 1) {
            currentIndex++
        } else {
            onClose()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset ->
                    if (offset.x < size.width / 3) {
                        // Previous
                        if (currentIndex > 0) currentIndex-- else onClose()
                    } else {
                        // Next
                        if (currentIndex < stories.size - 1) currentIndex++ else onClose()
                    }
                }
            )
        }
    ) {
        // Main Media
        AsyncImage(
            model = currentStory.mediaUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Overlay UI
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
                AvatarView(
                    name = currentStory.creatorDisplayName,
                    imageUrl = currentStory.creatorProfilePicture,
                    size = 32.dp
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(currentStory.creatorDisplayName, color = Color.White, fontSize = 14.sp)
                    Text("به تازگی", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }

            Spacer(Modifier.weight(1f))

            // Caption
            if (!currentStory.caption.isNullOrBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        currentStory.caption,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
