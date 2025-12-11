package com.spectretv.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Player : Screen("player/{channelId}") {
        fun createRoute(channelId: String) = "player/$channelId"
    }
    data object Favorites : Screen("favorites")
    data object Search : Screen("search")
    data object Settings : Screen("settings")
}
