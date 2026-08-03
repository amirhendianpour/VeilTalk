package com.example.veiltalk.feature.group.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.veiltalk.common.model.GroupMemberInfo
import com.example.veiltalk.common.ui.components.AvatarView
import com.example.veiltalk.feature.user.data.UserDirectoryRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    viewModel: GroupInfoViewModel = hiltViewModel(),
    userDirectory: UserDirectoryRepository,
    onBack: () -> Unit,
    onGroupDeleted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isEditingName by remember { mutableStateOf(false) }
    var nameInput by remember(uiState.groupName) { mutableStateOf(uiState.groupName) }
    var showAddMember by remember { mutableStateOf(false) }
    var newMemberInput by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf<GroupMemberInfo?>(null) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onGroupDeleted()
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadImage(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("اطلاعات گروه") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.clickable(enabled = uiState.isAdmin) { imagePicker.launch("image/*") }
                ) {
                    AvatarView(
                        name = uiState.groupName,
                        imageUrl = uiState.groupImageUrl,
                        size = 96.dp,
                        colorSeed = "group-${viewModel.groupId}"
                    )
                }
                Spacer(Modifier.height(8.dp))

                if (isEditingName) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            viewModel.updateGroupName(nameInput.trim())
                            isEditingName = false
                        }) { Text("ذخیره") }
                        TextButton(onClick = { isEditingName = false; nameInput = uiState.groupName }) { Text("انصراف") }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(uiState.groupName, style = MaterialTheme.typography.titleLarge)
                        if (uiState.isAdmin) {
                            TextButton(onClick = { isEditingName = true }) { Text("ویرایش") }
                        }
                    }
                }
                Text("${uiState.members.size} عضو", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }

            if (uiState.error != null) {
                Text(
                    uiState.error!!,
                    color = Color(0xFFDC2626),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("اعضای گروه", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                if (uiState.isAdmin) {
                    TextButton(onClick = { showAddMember = !showAddMember }) { Text("+ افزودن عضو") }
                }
            }

            if (showAddMember && uiState.isAdmin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newMemberInput,
                        onValueChange = { newMemberInput = it },
                        placeholder = { Text("شماره موبایل یا ایمیل عضو...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        viewModel.addMember(newMemberInput.trim()) { identifier ->
                            userDirectory.lookupUser(identifier).map { it.username }
                        }
                        newMemberInput = ""
                        showAddMember = false
                    }) { Text("افزودن") }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (uiState.isLoadingMembers) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.members, key = { it.username }) { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarView(
                                name = "${member.firstName} ${member.lastName}",
                                imageUrl = member.profilePictureUrl,
                                size = 44.dp,
                                colorSeed = member.username
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${member.firstName} ${member.lastName}" + if (viewModel.isMe(member.username)) " (شما)" else ""
                                )
                                if (member.role == "ADMIN") {
                                    Text("ادمین گروه", color = Color(0xFF16A34A), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (uiState.isAdmin && !viewModel.isMe(member.username)) {
                                val busy = uiState.busyUsername == member.username
                                Column(horizontalAlignment = Alignment.End) {
                                    TextButton(onClick = { viewModel.toggleAdmin(member) }, enabled = !busy) {
                                        Text(
                                            if (member.role == "ADMIN") "حذف ادمین" else "ارتقا به ادمین",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    TextButton(onClick = { showRemoveConfirm = member }, enabled = !busy) {
                                        Text("حذف از گروه", color = Color(0xFFDC2626), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }

            if (uiState.isAdmin) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("🗑️ حذف گروه", color = Color(0xFFDC2626))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف گروه") },
            text = { Text("آیا از حذف این گروه مطمئن هستید؟ این عملیات غیرقابل بازگشت است.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteGroup(onGroupDeleted)
                }) { Text("حذف", color = Color(0xFFDC2626)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("انصراف") }
            }
        )
    }

    showRemoveConfirm?.let { member ->
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = null },
            title = { Text("حذف عضو") },
            text = { Text("آیا از حذف ${member.firstName} ${member.lastName} از گروه مطمئن هستید؟") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeMember(member)
                    showRemoveConfirm = null
                }) { Text("حذف", color = Color(0xFFDC2626)) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = null }) { Text("انصراف") }
            }
        )
    }
}