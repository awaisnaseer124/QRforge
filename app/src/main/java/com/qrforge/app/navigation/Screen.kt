package com.qrforge.app.navigation

sealed class Screen(val route: String) {
    object Home     : Screen("home")
    object Generate : Screen("generate")
    object Scanner  : Screen("scanner")
    object History  : Screen("history")
    object Pro      : Screen("pro")
    object Profile  : Screen("profile")
}
