package com.example.veiltalk.feature.profile.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.veiltalk.feature.notification.data.ActiveSessionDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionsScreen(
    viewModel: ActiveSessionsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var sessionToTerminate by remember { mutableStateOf<ActiveSessionDto?>(null) }
    var showTerminateOthersDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مدیریت نشست‌های فعال") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val currentSession = uiState.sessions.find { it.isCurrent }
        val otherSessions = uiState.sessions.filter { !it.isCurrent }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.error != null) {
                item {
                    Text(
                        text = uiState.error!!,
                        color = Color(0xFFDC2626),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEE2E2), MaterialTheme.shapes.small)
                            .padding(12.dp)
                    )
                }
            }

            // Current Session Section
            if (currentSession != null) {
                item {
                    Text(
                        text = "این دستگاه (نشست جاری)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    SessionItemCard(session = currentSession, isCurrent = true)
                }
            }

            // Terminate Others Button
            if (otherSessions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showTerminateOthersDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("خاتمه دادن به سایر نشست‌ها", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Text(
                        text = "سایر دستگاه‌های متصل",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(otherSessions, key = { it.id }) { session ->
                    SessionItemCard(
                        session = session,
                        isCurrent = false,
                        onTerminate = { sessionToTerminate = session }
                    )
                }
            } else if (currentSession != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "هیچ دستگاه دیگری به حساب شما متصل نیست.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog for single session termination
    if (sessionToTerminate != null) {
        AlertDialog(
            onDismissRequest = { sessionToTerminate = null },
            title = { Text("خاتمه نشست") },
            text = { Text("آیا واقعاً می‌خواهید نشست دستگاه ${sessionToTerminate!!.deviceName} (${sessionToTerminate!!.deviceModel}) را خاتمه دهید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.terminateSession(sessionToTerminate!!.id)
                        sessionToTerminate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("خاتمه دادن", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToTerminate = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Confirmation dialog for all other sessions termination
    if (showTerminateOthersDialog) {
        AlertDialog(
            onDismissRequest = { showTerminateOthersDialog = false },
            title = { Text("خاتمه تمام نشست‌های دیگر") },
            text = { Text("آیا مطمئن هستید که می‌خواهید تمام دستگاه‌های متصل دیگر را از حساب خود خارج کنید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.terminateOtherSessions()
                        showTerminateOthersDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تایید و خاتمه همه", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTerminateOthersDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun SessionItemCard(
    session: ActiveSessionDto,
    isCurrent: Boolean,
    onTerminate: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${session.deviceName} ${session.deviceModel}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = session.osVersion,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "آدرس آی‌پی: ${session.ipAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isCurrent && onTerminate != null) {
                IconButton(onClick = onTerminate) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف نشست",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}