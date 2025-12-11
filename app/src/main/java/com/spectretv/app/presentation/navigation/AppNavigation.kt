package com.spectretv.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.spectretv.app.presentation.screens.live.LiveScreen
import com.spectretv.app.presentation.screens.movies.MovieDetailScreen
import com.spectretv.app.presentation.screens.movies.MoviesScreen
import com.spectretv.app.presentation.screens.player.PlayerScreen
import com.spectretv.app.presentation.screens.series.SeriesDetailScreen
import com.spectretv.app.presentation.screens.series.SeriesScreen
import com.spectretv.app.presentation.screens.settings.SettingsScreen
import java.net.URLDecoder

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Live, Icons.Default.LiveTv, "Live"),
    BottomNavItem(Screen.Movies, Icons.Default.Movie, "Movies"),
    BottomNavItem(Screen.Series, Icons.Default.Tv, "Series"),
    BottomNavItem(Screen.Settings, Icons.Default.Settings, "Settings")
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom bar on player and detail screens
    val showBottomBar = currentDestination?.route?.let { route ->
        !route.startsWith("player") &&
        !route.startsWith("movie/") &&
        !route.startsWith("series/")
    } != false

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Live.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Live.route) {
                LiveScreen(
                    onChannelClick = { channel ->
                        navController.navigate(
                            Screen.Player.createRoute(channel.streamUrl, channel.name)
                        )
                    }
                )
            }

            composable(Screen.Movies.route) {
                MoviesScreen(
                    onMovieClick = { movie ->
                        navController.navigate(Screen.MovieDetail.createRoute(movie.id))
                    }
                )
            }

            composable(
                route = Screen.MovieDetail.route,
                arguments = listOf(
                    navArgument("movieId") { type = NavType.StringType }
                )
            ) {
                MovieDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onPlayClick = { movie ->
                        navController.navigate(
                            Screen.Player.createRoute(movie.streamUrl, movie.name)
                        )
                    }
                )
            }

            composable(Screen.Series.route) {
                SeriesScreen(
                    onSeriesClick = { series ->
                        navController.navigate(Screen.SeriesDetail.createRoute(series.id))
                    }
                )
            }

            composable(
                route = Screen.SeriesDetail.route,
                arguments = listOf(
                    navArgument("seriesId") { type = NavType.StringType }
                )
            ) {
                SeriesDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onEpisodeClick = { episode ->
                        navController.navigate(
                            Screen.Player.createRoute(episode.streamUrl, episode.name)
                        )
                    }
                )
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("streamUrl") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("streamUrl") ?: ""
                val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
                val streamUrl = URLDecoder.decode(encodedUrl, "UTF-8")
                val title = URLDecoder.decode(encodedTitle, "UTF-8")

                PlayerScreen(
                    streamUrl = streamUrl,
                    title = title,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
