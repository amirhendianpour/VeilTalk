package com.example.veiltalk.feature.profile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    var showRestoreInput by remember { mutableStateOf(false) }
    var restoreCode by remember { mutableStateOf("") }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.executeBackup(it) }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { 
            pendingRestoreUri = it
            showRestoreInput = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بک‌آپ و بازیابی") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Backup,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                "پیام‌های شما فقط در این دستگاه ذخیره شده‌اند. با ایجاد بک‌آپ، می‌توانید در صورت تعویض گوشی، پیام‌ها را بازیابی کنید.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { viewModel.startBackupFlow() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Backup, null)
                Spacer(Modifier.width(8.dp))
                Text("ایجاد بک‌آپ جدید", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { restoreBackupLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Restore, null)
                Spacer(Modifier.width(8.dp))
                Text("بازیابی از فایل", fontWeight = FontWeight.Bold)
            }
        }
    }

    // دیالوگ نمایش کد ۳۰ رقمی برای بک‌آپ
    if (uiState.showRecoveryCodeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRecoveryDialog() },
            title = { Text("کد بازیابی شما") },
            text = {
                Column {
                    Text("این کد ۳۰ رقمی را در جایی بسیار امن یادداشت کنید. بدون این کد، بک‌آپ شما قابل بازیابی نخواهد بود.", color = Color.Red)
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            uiState.recoveryCode ?: "",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = { 
                            clipboardManager.setText(AnnotatedString(uiState.recoveryCode ?: ""))
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("کپی کد")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.dismissRecoveryDialog()
                    createBackupLauncher.launch("VeilTalk_Backup.veiltalk")
                }) {
                    Text("کد را یادداشت کردم")
                }
            }
        )
    }

    // دیالوگ ورودی کد برای بازیابی
    if (showRestoreInput) {
        AlertDialog(
            onDismissRequest = { showRestoreInput = false },
            title = { Text("کد بازیابی را وارد کنید") },
            text = {
                OutlinedTextField(
                    value = restoreCode,
                    onValueChange = { restoreCode = it },
                    label = { Text("کد ۳۰ رقمی") },
                    placeholder = { Text("XXXXX-XXXXX-...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    showRestoreInput = false
                    pendingRestoreUri?.let { viewModel.executeRestore(it, restoreCode) }
                }) {
                    Text("شروع بازیابی")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreInput = false }) { Text("انصراف") }
            }
        )
    }

    if (uiState.isLoading) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("در حال پردازش...")
                }
            }
        }
    }
}
