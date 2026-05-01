package com.qrforge.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.qrforge.app.feature.generate.GenerateScreen
import com.qrforge.app.feature.home.HomeScreen
import com.qrforge.app.feature.scanner.ScannerScreen
import com.qrforge.app.feature.history.HistoryScreen
import com.qrforge.app.feature.pro.ProScreen
import com.qrforge.app.feature.profile.ProfileScreen

@Composable
fun QRForgeNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToGenerate = { navController.navigate(Screen.Generate.route) },
                onNavigateToScanner  = { navController.navigate(Screen.Scanner.route) },
                onNavigateToPro      = { navController.navigate(Screen.Pro.route) }
            )
        }

        composable(Screen.Generate.route) {
            GenerateScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPro = { navController.navigate(Screen.Pro.route) }
            )
        }

        composable(Screen.Scanner.route) {
            ScannerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateToGenerate = { navController.navigate(Screen.Generate.route) },
                onNavigateToPro      = { navController.navigate(Screen.Pro.route) }
            )
        }

        composable(Screen.Pro.route) {
            ProScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToPro = { navController.navigate(Screen.Pro.route) }
            )
        }
    }
}
