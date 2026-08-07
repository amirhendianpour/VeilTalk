package com.example.veiltalk.feature.profile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.veiltalk.common.ui.components.AvatarView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadAvatar(it) }
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

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.clickable(enabled = uiState.mode == ProfileMode.EDIT) {
                    imagePicker.launch("image/*")
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

                if (!profile.phoneNumber.isNullOrBlank()) {
                    ProfileInfoRow(label = "شماره موبایل", value = profile.phoneNumber)
                }
                if (!profile.email.isNullOrBlank()) {
                    ProfileInfoRow(label = "ایمیل", value = profile.email)
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