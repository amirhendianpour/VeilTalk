package com.example.veiltalk.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val avatarColors = listOf(
    Color(0xFFBFDBFE) to Color(0xFF1D4ED8), // blue
    Color(0xFFBBF7D0) to Color(0xFF15803D), // green
    Color(0xFFE9D5FF) to Color(0xFF7E22CE), // purple
    Color(0xFFFBCFE8) to Color(0xFFBE185D), // pink
    Color(0xFFFEF08A) to Color(0xFFA16207), // yellow
    Color(0xFF99F6E4) to Color(0xFF0F766E), // teal
    Color(0xFFFED7AA) to Color(0xFFC2410C), // orange
    Color(0xFFFECACA) to Color(0xFFB91C1C), // red
)

private fun pickColor(seed: String): Pair<Color, Color> {
    var hash = 0
    for (c in seed) hash = c.code + ((hash shl 5) - hash)
    val index = kotlin.math.abs(hash) % avatarColors.size
    return avatarColors[index]
}

@Composable
fun AvatarView(
    name: String,
    imageUrl: String? = null,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    colorSeed: String? = null,
    modifier: Modifier = Modifier
) {
    if (imageUrl == "special://saved_messages") {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    } else if (!imageUrl.isNullOrBlank()) {
        androidx.compose.runtime.key(imageUrl) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
            )
        }
    } else {
        val (bg, fg) = pickColor(colorSeed ?: name)
        val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "؟"
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = fg,
                fontSize = (size.value * 0.4).sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}