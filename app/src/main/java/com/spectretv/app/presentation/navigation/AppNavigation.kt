package com.spectretv.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.spectretv.app.presentation.player.ContentType
import com.spectretv.app.presentation.player.FullScreenPlayer
import com.spectretv.app.presentation.player.MiniPlayer
import com.spectretv.app.presentation.player.PlayerManager
import com.spectretv.app.presentation.screens.live.LiveScreen
import com.spectretv.app.presentation.screens.movies.MovieDetailScreen
import com.spectretv.app.presentation.screens.movies.MoviesScreen
import com.spectretv.app.presentation.screens.series.SeriesDetailScreen
import com.spectretv.app.presentation.screens.series.SeriesScreen
import com.spectretv.app.presentation.screens.settings.SettingsScreen

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
fun AppNavigation(playerManager: PlayerManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentStream = playerManager.currentStream
    val isFullScreen = playerManager.isFullScreen
    val isPlaying = playerManager.isPlaying

    // Hide bottom bar on detail screens
    val showBottomBar = currentDestination?.route?.let { route ->
        !route.startsWith("movie/") &&
        !route.startsWith("series/")
    } != false

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content with navigation
        Scaffold(
            bottomBar = {
                if (showBottomBar && !isFullScreen) {
                    Column {
                        // Mini player above bottom nav
                        if (currentStream != null && !isFullScreen) {
                            MiniPlayer(
                                title = currentStream.title,
                                exoPlayer = playerManager.exoPlayer,
                                isPlaying = isPlaying,
                                onExpand = { playerManager.expand() },
                                onPlayPause = { playerManager.togglePlayPause() },
                                onClose = { playerManager.stop() }
                            )
                        }
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
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Live.route,
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                composable(Screen.Live.route) {
                    LiveScreen(
                        onChannelClick = { channel ->
                            playerManager.play(
                                url = channel.streamUrl,
                                title = channel.name,
                                contentType = ContentType.LIVE,
                                contentId = channel.id,
                                posterUrl = channel.logoUrl
                            )
                        },
                        onRecentlyWatchedClick = { historyItem ->
                            playerManager.play(
                                url = historyItem.streamUrl,
                                title = historyItem.name,
                                contentType = ContentType.LIVE,
                                contentId = historyItem.contentId,
                                posterUrl = historyItem.posterUrl
                            )
                        }
                    )
                }

                composable(Screen.Movies.route) {
                    MoviesScreen(
                        onMovieClick = { movie ->
                            navController.navigate(Screen.MovieDetail.createRoute(movie.id))
                        },
                        onContinueWatchingClick = { historyItem ->
                            playerManager.play(
                                url = historyItem.streamUrl,
                                title = historyItem.name,
                                contentType = ContentType.VOD,
                                contentId = historyItem.contentId,
                                posterUrl = historyItem.posterUrl
                            )
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
                            playerManager.play(
                                url = movie.streamUrl,
                                title = movie.name,
                                contentType = ContentType.VOD,
                                contentId = movie.id,
                                posterUrl = movie.posterUrl
                            )
                        }
                    )
                }

                composable(Screen.Series.route) {
                    SeriesScreen(
                        onSeriesClick = { series ->
                            navController.navigate(Screen.SeriesDetail.createRoute(series.id))
                        },
                        onContinueWatchingClick = { historyItem ->
                            playerManager.play(
                                url = historyItem.streamUrl,
                                title = historyItem.name,
                                contentType = ContentType.VOD,
                                contentId = historyItem.contentId,
                                posterUrl = historyItem.posterUrl,
                                seriesId = historyItem.seriesId,
                                seriesName = historyItem.seriesName,
                                seasonNumber = historyItem.seasonNumber,
                                episodeNumber = historyItem.episodeNumber
                            )
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
                        onEpisodeClick = { episode, seriesName ->
                            playerManager.play(
                                url = episode.streamUrl,
                                title = episode.name,
                                contentType = ContentType.VOD,
                                contentId = episode.id,
                                posterUrl = episode.posterUrl,
                                seriesId = episode.seriesId,
                                seriesName = seriesName,
                                seasonNumber = episode.seasonNumber,
                                episodeNumber = episode.episodeNumber
                            )
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
            }
        }

        // Full screen player overlay
        if (currentStream != null && isFullScreen) {
            FullScreenPlayer(
                title = currentStream.title,
                exoPlayer = playerManager.exoPlayer,
                isPlaying = isPlaying,
                contentType = currentStream.contentType,
                onMinimize = { playerManager.minimize() },
                onPlayPause = { playerManager.togglePlayPause() },
                onSeekTo = { playerManager.seekTo(it) },
                onSkipForward = { playerManager.skipForward() },
                onSkipBackward = { playerManager.skipBackward() }
            )
        }
    }
}
