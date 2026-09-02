package com.example.veiltalk.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContactMessageItem(
    name: String,
    phoneNumber: String,
    isMine: Boolean
) {
    val contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .width(220.dp)
            .padding(vertical = 4.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, contentColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(primaryColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = primaryColor)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = contentColor
                    )
                    if (phoneNumber.isNotBlank()) {
                        Text(
                            text = phoneNumber,
                            fontSize = 13.sp,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = contentColor.copy(alpha = 0.1f))
            TextButton(
                onClick = { /* Save contact or view logic */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("افزودن به مخاطبین", color = primaryColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}
