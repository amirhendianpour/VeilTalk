package com.example.veiltalk.feature.story.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veiltalk.common.ui.components.AvatarView
import com.example.veiltalk.feature.story.data.dto.StoryResponseDto

@Composable
fun StoriesRow(
    myUsername: String,
    myProfilePicture: String?,
    stories: Map<String, List<StoryResponseDto>>,
    onAddStory: () -> Unit,
    onViewStory: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AddStoryItem(profilePicture = myProfilePicture, onClick = onAddStory)
        }
        
        items(stories.keys.toList()) { username ->
            val userStories = stories[username].orEmpty()
            if (userStories.isNotEmpty()) {
                val firstStory = userStories.first()
                StoryCircle(
                    name = firstStory.creatorDisplayName,
                    imageUrl = firstStory.creatorProfilePicture,
                    hasUnseen = true, // Simplified
                    onClick = { onViewStory(username) }
                )
            }
        }
    }
}

@Composable
fun AddStoryItem(profilePicture: String?, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AvatarView(name = "من", imageUrl = profilePicture, size = 64.dp)
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(20.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.padding(2.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("استوری من", fontSize = 11.sp)
    }
}

@Composable
fun StoryCircle(
    name: String,
    imageUrl: String?,
    hasUnseen: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (hasUnseen) {
        Brush.sweepGradient(listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFFE91E63)))
    } else {
        Brush.linearGradient(listOf(Color.LightGray, Color.LightGray))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .border(2.dp, borderColor, CircleShape)
                .padding(4.dp)
        ) {
            AvatarView(name = name, imageUrl = imageUrl, size = 60.dp)
        }
        Spacer(Modifier.height(4.dp))
        Text(name.split(" ").firstOrNull() ?: name, fontSize = 11.sp, maxLines = 1)
    }
}
