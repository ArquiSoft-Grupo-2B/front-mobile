package com.colswe.groupbtwo

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.colswe.groupbtwo.ui.login.LoginScreen
import com.colswe.groupbtwo.ui.register.RegisterScreen
import com.colswe.groupbtwo.ui.recover.RecoverPasswordScreen
import com.colswe.groupbtwo.ui.map.MapScreen
import com.colswe.groupbtwo.ui.profile.ProfileScreen

@Composable
fun GroupBTwoApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onSuccess = {
                    navController.navigate("map") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onRegister = {
                    navController.navigate("register")
                },
                onRecover = {
                    navController.navigate("recover")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onSuccess = {
                    navController.navigate("map") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable("recover") {
            RecoverPasswordScreen(
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable("map") {
            MapScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onProfile = {
                    navController.navigate("profile")
                }
            )
        }

        composable("profile") {
            ProfileScreen(
                onBackToMap = {
                    navController.popBackStack()
                }
            )
        }
    }
}