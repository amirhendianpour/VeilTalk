package com.example.veiltalk.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.veiltalk.feature.auth.ui.LoginScreen
import com.example.veiltalk.feature.auth.ui.OtpVerifyScreen
import com.example.veiltalk.feature.auth.ui.RegisterScreen
import com.example.veiltalk.feature.chat.ui.ChatScreen
import com.example.veiltalk.feature.chat.ui.HomeScreen
import com.example.veiltalk.feature.group.ui.GroupChatScreen
import com.example.veiltalk.feature.group.ui.GroupInfoScreen
import com.example.veiltalk.feature.user.data.UserDirectoryRepository

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val OTP = "otp/{identifier}"
    const val PROFILE = "profile"
    const val HOME = "home"
    const val CHAT = "chat/{username}"
    const val GROUP_CHAT = "groupChat/{groupId}"
    const val GROUP_INFO = "groupInfo/{groupId}"

    fun otpRoute(identifier: String) = "otp/$identifier"
    fun chatRoute(username: String) = "chat/$username"
    fun groupChatRoute(groupId: Long) = "groupChat/$groupId"
    fun groupInfoRoute(groupId: Long) = "groupInfo/$groupId"
}

@Composable
fun VeilTalkNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.LOGIN,
    userDirectoryRepository: UserDirectoryRepository
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onSwitchToRegister = { navController.navigate(Routes.REGISTER) },
                onOtpRequested = { identifier -> navController.navigate(Routes.otpRoute(identifier)) },
                onAuthenticated = { navController.navigate(Routes.HOME) { popUpTo(0) } }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onSwitchToLogin = { navController.popBackStack() },
                onRegistered = { identifier -> navController.navigate(Routes.otpRoute(identifier)) }
            )
        }

        composable(
            route = Routes.OTP,
            arguments = listOf(navArgument("identifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val identifier = backStackEntry.arguments?.getString("identifier") ?: ""
            OtpVerifyScreen(
                identifier = identifier,
                onBack = { navController.popBackStack() },
                onVerified = { navController.navigate(Routes.HOME) { popUpTo(0) } }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onOpenChat = { username -> navController.navigate(Routes.chatRoute(username)) },
                onOpenGroup = { groupId -> navController.navigate(Routes.groupChatRoute(groupId)) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onLoggedOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) {
            ChatScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.GROUP_CHAT,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
            GroupChatScreen(
                userDirectory = userDirectoryRepository,
                onBack = { navController.popBackStack() },
                onOpenInfo = { navController.navigate(Routes.groupInfoRoute(groupId)) }
            )
        }

        composable(
            route = Routes.GROUP_INFO,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) {
            GroupInfoScreen(
                userDirectory = userDirectoryRepository,
                onBack = { navController.popBackStack() },
                onGroupDeleted = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(Routes.PROFILE) {
            com.example.veiltalk.feature.profile.ui.ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}