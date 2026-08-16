package com.example.veiltalk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.veiltalk.core.service.ChatConnectionService
import com.example.veiltalk.feature.user.ui.UserDirectoryEntryPointViewModel
import com.example.veiltalk.navigation.VeilTalkNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* نتیجه رو نادیده می‌گیریم؛ نوتیف صرفاً برای زنده‌نگه‌داشتن سرویسه */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        RootContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun RootContent(
    sessionViewModel: SessionCheckViewModel = hiltViewModel()
) {
    val startDestination by sessionViewModel.startDestination.collectAsState()
    val token by sessionViewModel.tokenFlow.collectAsState(initial = null)
    
    val navController = rememberNavController()

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(token) {
        if (token != null) {
            ChatConnectionService.start(context)
        } else {
            ChatConnectionService.stop(context)
        }
    }
    
    // مدیریت ناوبری از طریق نوتیفیکیشن
    val activity = context as? androidx.activity.ComponentActivity
    LaunchedEffect(activity?.intent) {
        val chatUsername = activity?.intent?.getStringExtra("chat_username")
        val groupId = activity?.intent?.getLongExtra("group_id", -1L)?.takeIf { it != -1L }
        
        if (startDestination != null) {
            if (chatUsername != null) {
                navController.navigate(com.example.veiltalk.navigation.Routes.chatRoute(chatUsername))
                activity.intent.removeExtra("chat_username")
            } else if (groupId != null) {
                navController.navigate(com.example.veiltalk.navigation.Routes.groupChatRoute(groupId))
                activity.intent.removeExtra("group_id")
            }
        }
    }

    if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            VeilTalkNavGraph(
                navController = navController,
                startDestination = startDestination!!,
                userDirectoryRepository = hiltViewModel<com.example.veiltalk.feature.user.ui.UserDirectoryEntryPointViewModel>().repository
            )
            // لایه‌ی سراسری تماس — روی هر صفحه‌ای که باشیم نمایش داده می‌شود
            com.example.veiltalk.feature.call.ui.CallOverlay()
        }
    }
}
