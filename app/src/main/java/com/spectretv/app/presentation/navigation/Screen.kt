package com.spectretv.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Live : Screen("live")
    data object Movies : Screen("movies")
    data object Series : Screen("series")
    data object Settings : Screen("settings")

    data object Player : Screen("player/{streamUrl}/{title}") {
        fun createRoute(streamUrl: String, title: String): String {
            val encodedUrl = java.net.URLEncoder.encode(streamUrl, "UTF-8")
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            return "player/$encodedUrl/$encodedTitle"
        }
    }

    data object MovieDetail : Screen("movie/{movieId}") {
        fun createRoute(movieId: String) = "movie/$movieId"
    }

    data object SeriesDetail : Screen("series/{seriesId}") {
        fun createRoute(seriesId: String) = "series/$seriesId"
    }
}
