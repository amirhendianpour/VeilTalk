package com.example.veiltalk.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.veiltalk.common.ui.components.AvatarView
import com.example.veiltalk.feature.group.ui.CreateGroupDialog
import com.example.veiltalk.feature.profile.ui.ProfileViewModel
import com.example.veiltalk.feature.profile.ui.ProfileMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    onOpenChat: (username: String) -> Unit,
    onOpenGroup: (groupId: Long) -> Unit,
    onOpenProfile: (username: String) -> Unit, // تغییر یافته برای پروفایل سایرین
    onOpenMyProfile: () -> Unit, // نام جدید برای پروفایل خود کاربر
    onLoggedOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var tab by remember { mutableStateOf(HomeTab.ALL) }
    var bottomNavTab by remember { mutableIntStateOf(0) }
    var newChatInput by remember { mutableStateOf("") }
    var showNewChatField by remember { mutableStateOf(false) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (bottomNavTab == 0) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    ),
                    title = {
                        AsyncImage(
                            model = "file:///android_asset/logo-text-veil-talk.png",
                            contentDescription = "VeilTalk",
                            modifier = Modifier.height(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    },
                    actions = {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "بیشتر")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AvatarView(
                                            name = uiState.myDisplayName,
                                            imageUrl = uiState.myProfilePictureUrl,
                                            size = 32.dp,
                                            colorSeed = uiState.myUsername
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(uiState.myDisplayName)
                                    }
                                },
                                onClick = { 
                                    showMenu = false
                                    onOpenMyProfile()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                leadingIcon = { 
                                    Icon(
                                        if (uiState.isDarkMode == true) Icons.Default.BrightnessHigh else Icons.Default.Brightness4, 
                                        null
                                    ) 
                                },
                                text = { Text(if (uiState.isDarkMode == true) "حالت روز" else "حالت شب") },
                                onClick = { 
                                    showMenu = false
                                    viewModel.toggleDarkMode(uiState.isDarkMode != true)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Bookmark, null) },
                                text = { Text("پیام‌های ذخیره شده") },
                                onClick = { 
                                    showMenu = false
                                    onOpenChat(uiState.myUsername) 
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Language, null) },
                                text = { Text("تغییر زبان") },
                                onClick = { showMenu = false }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                                text = { Text("خروج", color = Color.Red) },
                                onClick = { 
                                    showMenu = false
                                    viewModel.logout(onLoggedOut) 
                                }
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = bottomNavTab == 0,
                    onClick = { bottomNavTab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "چت‌ها") },
                    label = { Text("چت‌ها") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = bottomNavTab == 1,
                    onClick = { bottomNavTab = 1 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "مخاطبین") },
                    label = { Text("مخاطبین") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = bottomNavTab == 2,
                    onClick = { bottomNavTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "تنظیمات") },
                    label = { Text("تنظیمات") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = bottomNavTab == 3,
                    onClick = { bottomNavTab = 3 },
                    icon = {
                        AvatarView(
                            name = uiState.myDisplayName,
                            imageUrl = uiState.myProfilePictureUrl,
                            size = 24.dp,
                            colorSeed = uiState.myUsername
                        )
                    },
                    label = { Text("پروفایل") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
            }
        },
        floatingActionButton = {
            if (bottomNavTab == 0) {
                FloatingActionButton(
                    onClick = {
                        if (tab == HomeTab.GROUPS) showCreateGroup = true else showNewChatField = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "جدید")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (bottomNavTab == 0) {
                PrimaryTabRow(
                    selectedTabIndex = tab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(selected = tab == HomeTab.ALL, onClick = { tab = HomeTab.ALL }, text = { Text("همه") })
                    Tab(selected = tab == HomeTab.CHATS, onClick = { tab = HomeTab.CHATS }, text = { Text("خصوصی") })
                    Tab(selected = tab == HomeTab.GROUPS, onClick = { tab = HomeTab.GROUPS }, text = { Text("گروه‌ها") })
                }

                if (showNewChatField && tab != HomeTab.GROUPS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newChatInput,
                            onValueChange = { newChatInput = it },
                            placeholder = { Text("شماره موبایل یا ایمیل مخاطب...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.startNewChat(newChatInput) { username ->
                                    newChatInput = ""
                                    showNewChatField = false
                                    onOpenChat(username)
                                }
                            },
                            enabled = !uiState.isLookingUp,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (uiState.isLookingUp) "..." else "+ چت")
                        }
                    }
                    if (uiState.lookupError != null) {
                        Text(uiState.lookupError!!, color = Color(0xFFDC2626), modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }

                when (tab) {
                    HomeTab.ALL -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.allItems, key = { it.key }) { item ->
                            when (item) {
                                is HomeListItem.ChatItem -> ChatRow(
                                    displayName = item.displayName,
                                    imageUrl = item.profilePictureUrl,
                                    colorSeed = item.username,
                                    subtitle = item.lastMessage,
                                    time = item.time,
                                    unreadCount = item.unreadCount,
                                    onClick = { onOpenChat(item.username) }
                                )
                                is HomeListItem.GroupItem -> ChatRow(
                                    displayName = item.group.name,
                                    imageUrl = item.group.imageUrl,
                                    colorSeed = "group-${item.group.id}",
                                    subtitle = item.lastMessage.ifBlank { "گروه" },
                                    time = item.time,
                                    unreadCount = item.unreadCount,
                                    onClick = { onOpenGroup(item.group.id) }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(start = 76.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                    HomeTab.CHATS -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.chatItems, key = { it.key }) { item ->
                            ChatRow(
                                displayName = item.displayName,
                                imageUrl = item.profilePictureUrl,
                                colorSeed = item.username,
                                subtitle = item.lastMessage,
                                time = item.time,
                                unreadCount = item.unreadCount,
                                onClick = { onOpenChat(item.username) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 76.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                    HomeTab.GROUPS -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.groups, key = { it.id }) { group ->
                            val summary = uiState.allItems.filterIsInstance<HomeListItem.GroupItem>().find { it.group.id == group.id }
                            ChatRow(
                                displayName = group.name,
                                imageUrl = group.imageUrl,
                                colorSeed = "group-${group.id}",
                                subtitle = summary?.lastMessage ?: (if (group.role == "ADMIN") "ادمین" else "عضو"),
                                time = summary?.time ?: "",
                                unreadCount = summary?.unreadCount ?: 0,
                                onClick = { onOpenGroup(group.id) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 76.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            } else if (bottomNavTab == 1) {
                ContactsTab(
                    allItems = uiState.allItems.filterIsInstance<HomeListItem.ChatItem>(),
                    onOpenChat = onOpenChat,
                    onOpenProfile = onOpenProfile
                )
            } else if (bottomNavTab == 2) {
                SettingsTab(
                    displayName = uiState.myDisplayName,
                    username = uiState.myUsername,
                    profilePicture = uiState.myProfilePictureUrl,
                    isDarkMode = uiState.isDarkMode == true,
                    onToggleDarkMode = viewModel::toggleDarkMode,
                    onLogout = { viewModel.logout(onLoggedOut) }
                )
            } else if (bottomNavTab == 3) {
                ProfileTab(viewModel = profileViewModel)
            }
        }
    }

    if (showCreateGroup) {
        CreateGroupDialog(
            onDismiss = { showCreateGroup = false },
            onConfirm = { name ->
                showCreateGroup = false
                viewModel.createGroup(name) { groupId -> onOpenGroup(groupId) }
            }
        )
    }
}

@Composable
private fun ContactsTab(
    allItems: List<HomeListItem.ChatItem>,
    onOpenChat: (String) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ListItem(
                headlineContent = { Text("مخاطب جدید", color = primaryColor) },
                leadingContent = { Icon(Icons.Default.Add, null, tint = primaryColor) },
                modifier = Modifier.clickable { /* logic to add contact */ }
            )
        }
        items(allItems, key = { it.username }) { item ->
            ListItem(
                headlineContent = { Text(item.displayName, fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text("آخرین بازدید اخیراً", fontSize = 12.sp) },
                leadingContent = {
                    AvatarView(
                        name = item.displayName,
                        imageUrl = item.profilePictureUrl,
                        size = 40.dp,
                        colorSeed = item.username,
                        modifier = Modifier.clickable { onOpenProfile(item.username) }
                    )
                },
                modifier = Modifier.clickable { onOpenChat(item.username) }
            )
        }
    }
}

@Composable
private fun SettingsTab(
    displayName: String,
    username: String,
    profilePicture: String?,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AvatarView(
                        name = displayName,
                        imageUrl = profilePicture,
                        size = 100.dp,
                        colorSeed = username
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        displayName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "@$username",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    Text(
                        "تنظیمات ظاهری",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    ListItem(
                        headlineContent = { Text("حالت شب") },
                        leadingContent = { Icon(Icons.Default.Brightness4, null) },
                        trailingContent = {
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = onToggleDarkMode
                            )
                        }
                    )
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            SettingsSection(
                title = "تنظیمات اپلیکیشن",
                items = listOf(
                    SettingsItemData("اعلان‌ها و صداها", Icons.Default.Notifications),
                    SettingsItemData("حریم خصوصی و امنیت", Icons.Default.PrivacyTip),
                    SettingsItemData("داده‌ها و ذخیره‌سازی", Icons.Default.Storage),
                    SettingsItemData("تغییر زبان", Icons.Default.Language)
                )
            )
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            Surface(color = MaterialTheme.colorScheme.surface) {
                ListItem(
                    headlineContent = { Text("خروج از حساب", color = Color.Red) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.Red) },
                    modifier = Modifier.clickable(onClick = onLogout)
                )
            }
        }
    }
}

private data class SettingsItemData(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun SettingsSection(title: String, items: List<SettingsItemData>) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Text(
                title,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            items.forEach { item ->
                ListItem(
                    headlineContent = { Text(item.title) },
                    leadingContent = { Icon(item.icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                    modifier = Modifier.clickable { /* action */ }
                )
            }
        }
    }
}

@Composable
private fun ProfileTab(viewModel: ProfileViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.uploadAvatar(it) } }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val profile = uiState.profile ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
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
                            size = 120.dp,
                            colorSeed = profile.username
                        )
                        if (uiState.isUploadingAvatar) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.4f), shape = androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))

                    if (uiState.mode == ProfileMode.VIEW) {
                        Text(
                            "${profile.firstName} ${profile.lastName}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "@${profile.username}",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = viewModel::enterEditMode) {
                            Text("ویرایش اطلاعات", color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    label = { Text("نام خانوادگی") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = uiState.bioInput,
                                onValueChange = viewModel::onBioChange,
                                label = { Text("بیو") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = viewModel::cancelEdit, modifier = Modifier.weight(1f)) {
                                    Text("انصراف", color = Color.Gray)
                                }
                                Button(
                                    onClick = viewModel::save,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    enabled = !uiState.isSaving
                                ) {
                                    Text(if (uiState.isSaving) "در حال ذخیره..." else "ذخیره")
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    ProfileDetailRow(label = "آیدی", value = "@${profile.username}")
                    ProfileDetailRow(label = "شماره موبایل", value = profile.phoneNumber ?: "تنظیم نشده")
                    ProfileDetailRow(label = "ایمیل", value = profile.email ?: "تنظیم نشده")
                    ProfileDetailRow(label = "بیو", value = profile.bio ?: "توضیحی وجود ندارد")
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    ListItem(
        headlineContent = { Text(value, fontSize = 16.sp) },
        supportingContent = { Text(label, fontSize = 13.sp, color = Color.Gray) }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
}

@Composable
private fun ChatRow(
    displayName: String,
    imageUrl: String?,
    colorSeed: String,
    subtitle: String,
    time: String,
    unreadCount: Int = 0,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarView(name = displayName, imageUrl = imageUrl, size = 52.dp, colorSeed = colorSeed)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    com.example.veiltalk.common.util.formatMessageTime(time),
                    fontSize = 12.sp,
                    color = if (unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (unreadCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.sizeIn(minWidth = 20.dp, minHeight = 20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                            Text(
                                text = unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
