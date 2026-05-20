package com.clubmates.app.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clubmates.app.ui.auth.AuthViewModel
import com.clubmates.app.ui.auth.OtpScreen
import com.clubmates.app.ui.auth.PhoneInputScreen
import com.clubmates.app.ui.auth.WelcomeScreen
import com.clubmates.app.ui.main.MainTabScreen
import com.clubmates.app.ui.profile.CreateProfileScreen

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object PhoneInput : Screen("phone_input")
    object Otp : Screen("otp/{phone}") {
        fun createRoute(phone: String) = "otp/$phone"
    }
    object CreateProfile : Screen("create_profile")
    object Main : Screen("main")
}

@Composable
fun ClubMatesNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = Screen.Welcome.route) {

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Screen.PhoneInput.route) },
                isLoggedIn = authState.isAuthenticated && !authState.needsProfile,
                onAlreadyLoggedIn = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PhoneInput.route) {
            PhoneInputScreen(
                viewModel = authViewModel,
                onOtpSent = { phone ->
                    navController.navigate(Screen.Otp.createRoute(phone))
                }
            )
        }

        composable(Screen.Otp.route) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            OtpScreen(
                phone = phone,
                viewModel = authViewModel,
                onVerified = {
                    if (authState.needsProfile) {
                        navController.navigate(Screen.CreateProfile.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.CreateProfile.route) {
            CreateProfileScreen(
                viewModel = authViewModel,
                onProfileCreated = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.CreateProfile.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainTabScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
            )
        }
    }
}
