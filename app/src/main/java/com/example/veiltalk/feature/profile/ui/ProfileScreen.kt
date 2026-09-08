package com.example.veiltalk.feature.profile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.veiltalk.common.ui.components.AvatarView
import com.example.veiltalk.common.ui.components.FullScreenImageViewer
import com.example.veiltalk.common.ui.components.ImageCropperDialog
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var imageUriToCrop by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    if (imageUriToCrop != null) {
        ImageCropperDialog(
            uri = imageUriToCrop!!,
            onDismiss = { imageUriToCrop = null },
            onCropped = { croppedUri ->
                imageUriToCrop = null
                viewModel.uploadAvatar(croppedUri)
            }
        )
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUriToCrop = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("پروفایل من") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    if (uiState.mode == ProfileMode.VIEW && uiState.profile != null) {
                        TextButton(onClick = viewModel::enterEditMode) { Text("ویرایش") }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val profile = uiState.profile ?: return@Scaffold

        Box(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.clickable {
                        if (uiState.mode == ProfileMode.EDIT) {
                            imagePicker.launch("image/*")
                        } else if (!profile.profilePictureUrl.isNullOrBlank()) {
                            viewModel.showFullScreenAvatar()
                        }
                    }
                ) {
                    AvatarView(
                        name = "${profile.firstName} ${profile.lastName}",
                        imageUrl = profile.profilePictureUrl,
                        size = 96.dp,
                        colorSeed = profile.username
                    )
                    if (uiState.isUploadingAvatar) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.4f), shape = androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
                if (uiState.mode == ProfileMode.EDIT) {
                    Text("برای تغییر عکس ضربه بزنید", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                Spacer(Modifier.height(16.dp))

                if (uiState.error != null) {
                    Text(
                        uiState.error!!,
                        color = Color(0xFFDC2626),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEE2E2), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }

                if (uiState.mode == ProfileMode.VIEW) {
                    Text(
                        "${profile.firstName} ${profile.lastName}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        profile.bio ?: "بدون بیو",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))

                    ProfileInfoRow(label = "نام کاربری (آیدی)", value = "@${profile.username}")
                    if (!profile.phoneNumber.isNullOrBlank()) {
                        ProfileInfoRow(label = "شماره موبایل", value = profile.phoneNumber)
                    }
                    if (!profile.email.isNullOrBlank()) {
                        ProfileInfoRow(label = "ایمیل", value = profile.email)
                    }

                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        onClick = onChangePassword,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("تغییر رمز عبور")
                    }
                    
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("حذف حساب کاربری")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.firstNameInput,
                            onValueChange = viewModel::onFirstNameChange,
                            label = { Text("نام") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.lastNameInput,
                            onValueChange = viewModel::onLastNameChange,
                            label = { Text("نام‌خانوادگی") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.bioInput,
                        onValueChange = viewModel::onBioChange,
                        label = { Text("بیو") },
                        placeholder = { Text("چند کلمه درباره خودتان بنویسید...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                    Text(
                        "${uiState.bioInput.length}/150",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )

                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = viewModel::cancelEdit,
                            enabled = !uiState.isSaving,
                            modifier = Modifier.weight(1f)
                        ) { Text("انصراف") }
                        Button(
                            onClick = viewModel::save,
                            enabled = !uiState.isSaving,
                            modifier = Modifier.weight(1f)
                        ) { Text(if (uiState.isSaving) "در حال ذخیره..." else "ذخیره") }
                    }
                }
            }

            if (uiState.showFullScreenAvatar && !profile.profilePictureUrl.isNullOrBlank()) {
                FullScreenImageViewer(
                    url = profile.profilePictureUrl!!,
                    mediaKey = null,
                    onDismiss = viewModel::hideFullScreenAvatar,
                    onSave = viewModel::saveProfilePicture
                )
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("حذف حساب کاربری") },
                text = { Text("آیا واقعاً می‌خواهید حساب کاربری خود را حذف کنید؟ این عمل غیرقابل بازگشت است و تمام چت‌ها، گروه‌ها و اطلاعات شما برای همیشه پاک خواهد شد.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteAccount(onLoggedOut)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("حذف دائم حساب", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("انصراف")
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value)
    }
    HorizontalDivider()
}