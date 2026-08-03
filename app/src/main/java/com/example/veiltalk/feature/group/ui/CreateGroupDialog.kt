package com.example.veiltalk.feature.group.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier.padding(20.dp)
            ) {
                Text("گروه جدید", style = MaterialTheme.typography.titleMedium)
                Spacer(androidx.compose.ui.Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام گروه") },
                    singleLine = true
                )
                Spacer(androidx.compose.ui.Modifier.height(16.dp))
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    Button(
                        onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                        enabled = name.isNotBlank()
                    ) { Text("+ گروه") }
                }
            }
        }
    }
}