package com.example.veiltalk.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAttachImage: () -> Unit = {},
    onAttachFile: () -> Unit = {},
    isUploading: Boolean = false,
    uploadError: String? = null,
    onClearUploadError: () -> Unit = {},
    placeholder: String = "پیام..."
) {
    var showAttachMenu by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val inputBackgroundColor = MaterialTheme.colorScheme.surface

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
                IconButton(onClick = { /* Emoji placeholder */ }) {
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
