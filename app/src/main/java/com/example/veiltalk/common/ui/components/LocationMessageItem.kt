package com.example.veiltalk.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veiltalk.ui.theme.*

@Composable
fun LocationMessageItem(
    lat: Double,
    lng: Double,
    isMine: Boolean,
    isLive: Boolean = false,
    onStopLive: () -> Unit = {},
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .width(240.dp)
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, contentColor.copy(alpha = 0.2f))
    ) {
        Column {
            // Placeholder for map static image (in a real app, you'd use Google Static Maps API)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(40.dp)
                )
                if (isLive) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                        color = Color.Red,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "LIVE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    "مشاهده روی نقشه",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                    fontSize = 12.sp,
                    color = primaryColor
                )
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    if (isLive) "مکان زنده" else "مکان ارسالی",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = contentColor
                )
                Text(
                    "$lat, $lng",
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.7f)
                )
                
                if (isLive && isMine) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onStopLive,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("توقف اشتراک‌گذاری", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
