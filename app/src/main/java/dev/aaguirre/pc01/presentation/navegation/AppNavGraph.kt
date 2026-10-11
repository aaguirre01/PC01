package dev.aaguirre.pc01.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.aaguirre.pc01.presentation.auth.LoginScreen
import dev.aaguirre.pc01.presentation.auth.RegisterScreen
import dev.aaguirre.pc01.presentation.play.PlayScreen
import dev.aaguirre.pc01.presentation.tracking.TrackingScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"  // Siempre empieza en login
    ) {
        composable("login") {
            LoginScreen(navController)
        }

        composable("register") {
            RegisterScreen(navController)
        }

        composable("play/{drawId}") { backStackEntry ->
            val drawId = backStackEntry.arguments?.getString("drawId") ?: ""
            DrawerScaffold(navController) {
                PlayScreen(drawId, navController)
            }
        }

        composable("tracking/{drawId}") { backStackEntry ->
            val drawId = backStackEntry.arguments?.getString("drawId") ?: ""
            DrawerScaffold(navController) {
                TrackingScreen(drawId)
            }
        }
    }
}