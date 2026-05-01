package com.qrforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.qrforge.app.navigation.QRForgeNavGraph
import com.qrforge.app.navigation.Screen
import com.qrforge.app.ui.components.BottomNavBar
import com.qrforge.app.ui.theme.QRForgeTheme
import dagger.hilt.android.AndroidEntryPoint

private val screensWithoutBottomNav = setOf(
    Screen.Scanner.route,
    Screen.Pro.route
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QRForgeTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = currentRoute !in screensWithoutBottomNav

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    // Use a Box to apply the Scaffold's padding
                    Box(modifier = Modifier.padding(innerPadding)) {
                        QRForgeNavGraph(
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}