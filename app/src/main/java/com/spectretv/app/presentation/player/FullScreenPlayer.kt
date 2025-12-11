package com.spectretv.app.presentation.player

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.util.Locale

data class TrackInfo(
    val index: Int,
    val groupIndex: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun FullScreenPlayer(
    title: String,
    exoPlayer: ExoPlayer?,
    isPlaying: Boolean,
    onMinimize: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var showControls by remember { mutableStateOf(true) }
    // Start with buffering true until we know the player is ready
    var isBuffering by remember { mutableStateOf(true) }
    var showTrackSelector by remember { mutableStateOf(false) }

    // Track info
    val audioTracks = remember { mutableStateListOf<TrackInfo>() }
    val subtitleTracks = remember { mutableStateListOf<TrackInfo>() }
    var selectedAudioIndex by remember { mutableIntStateOf(0) }
    var selectedSubtitleIndex by remember { mutableIntStateOf(-1) }

    // Handle back button - minimize instead of PiP
    BackHandler {
        onMinimize()
    }

    // Hide system bars for immersive mode
    DisposableEffect(Unit) {
        val window = activity?.window
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            WindowInsetsControllerCompat(it, it.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            window?.let {
                // Show system bars but keep edge-to-edge (don't set decorFitsSystemWindows)
                val controller = WindowInsetsControllerCompat(it, it.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                // Re-apply dark status bar icons for dark theme
                controller.isAppearanceLightStatusBars = false
            }
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls && !showTrackSelector) {
            delay(4000)
            showControls = false
        }
    }

    // Listen for playback state and tracks
    DisposableEffect(exoPlayer) {
        // Check current state immediately
        exoPlayer?.let { player ->
            val state = player.playbackState
            isBuffering = state == Player.STATE_BUFFERING || state == Player.STATE_IDLE
        }

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onTracksChanged(tracks: Tracks) {
                val newAudioTracks = mutableListOf<TrackInfo>()
                val newSubtitleTracks = mutableListOf<TrackInfo>()

                tracks.groups.forEachIndexed { groupIndex, group ->
                    when (group.type) {
                        C.TRACK_TYPE_AUDIO -> {
                            for (i in 0 until group.length) {
                                val format = group.getTrackFormat(i)
                                val label = format.label
                                    ?: format.language?.let { Locale(it).displayLanguage }
                                    ?: "Audio ${newAudioTracks.size + 1}"
                                newAudioTracks.add(
                                    TrackInfo(
                                        index = i,
                                        groupIndex = groupIndex,
                                        label = label,
                                        language = format.language,
                                        isSelected = group.isTrackSelected(i)
                                    )
                                )
                            }
                        }
                        C.TRACK_TYPE_TEXT -> {
                            for (i in 0 until group.length) {
                                val format = group.getTrackFormat(i)
                                val label = format.label
                                    ?: format.language?.let { Locale(it).displayLanguage }
                                    ?: "Subtitle ${newSubtitleTracks.size + 1}"
                                newSubtitleTracks.add(
                                    TrackInfo(
                                        index = i,
                                        groupIndex = groupIndex,
                                        label = label,
                                        language = format.language,
                                        isSelected = group.isTrackSelected(i)
                                    )
                                )
                            }
                        }
                    }
                }

                audioTracks.clear()
                audioTracks.addAll(newAudioTracks)
                subtitleTracks.clear()
                subtitleTracks.addAll(newSubtitleTracks)

                // Update selected indices
                selectedAudioIndex = newAudioTracks.indexOfFirst { it.isSelected }.coerceAtLeast(0)
                selectedSubtitleIndex = newSubtitleTracks.indexOfFirst { it.isSelected }
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
        }
    }

    // Helper to select audio track
    fun selectAudioTrack(index: Int) {
        if (index < audioTracks.size) {
            val track = audioTracks[index]
            val trackGroup = exoPlayer?.currentTracks?.groups?.getOrNull(track.groupIndex)
            trackGroup?.let { group ->
                val trackSelector = (exoPlayer as? ExoPlayer)?.trackSelector as? DefaultTrackSelector
                trackSelector?.setParameters(
                    trackSelector.buildUponParameters()
                        .setOverrideForType(
                            TrackSelectionOverride(group.mediaTrackGroup, track.index)
                        )
                )
            }
            selectedAudioIndex = index
        }
    }

    // Helper to select subtitle track
    fun selectSubtitleTrack(index: Int) {
        val trackSelector = (exoPlayer as? ExoPlayer)?.trackSelector as? DefaultTrackSelector
        if (index == -1) {
            // Disable subtitles
            trackSelector?.setParameters(
                trackSelector.buildUponParameters()
                    .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .setPreferredTextLanguage(null)
            )
        } else if (index < subtitleTracks.size) {
            val track = subtitleTracks[index]
            val trackGroup = exoPlayer?.currentTracks?.groups?.getOrNull(track.groupIndex)
            trackGroup?.let { group ->
                trackSelector?.setParameters(
                    trackSelector.buildUponParameters()
                        .setOverrideForType(
                            TrackSelectionOverride(group.mediaTrackGroup, track.index)
                        )
                )
            }
        }
        selectedSubtitleIndex = index
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                showControls = !showControls
            }
    ) {
        // Video view
        exoPlayer?.let { player ->
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Buffering indicator
        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Controls overlay
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                // Top bar with back, title, and settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onMinimize) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Minimize",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    // Settings button for track selection
                    if (audioTracks.size > 1 || subtitleTracks.isNotEmpty()) {
                        IconButton(onClick = { showTrackSelector = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Track settings",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Center play/pause (hide when buffering)
                if (!isBuffering) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f))
                                .clickable(onClick = onPlayPause),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }
        }

        // Track selection bottom sheet
        if (showTrackSelector) {
            TrackSelectionSheet(
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                selectedAudioIndex = selectedAudioIndex,
                selectedSubtitleIndex = selectedSubtitleIndex,
                onAudioTrackSelect = { selectAudioTrack(it) },
                onSubtitleTrackSelect = { selectSubtitleTrack(it) },
                onDismiss = { showTrackSelector = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackSelectionSheet(
    audioTracks: List<TrackInfo>,
    subtitleTracks: List<TrackInfo>,
    selectedAudioIndex: Int,
    selectedSubtitleIndex: Int,
    onAudioTrackSelect: (Int) -> Unit,
    onSubtitleTrackSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Audio") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Subtitles") }
                )
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        items(audioTracks.indices.toList()) { index ->
                            val track = audioTracks[index]
                            TrackItem(
                                label = track.label,
                                isSelected = index == selectedAudioIndex,
                                onClick = { onAudioTrackSelect(index) }
                            )
                        }
                    }
                }
                1 -> {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        item {
                            TrackItem(
                                label = "Off",
                                isSelected = selectedSubtitleIndex == -1,
                                onClick = { onSubtitleTrackSelect(-1) }
                            )
                        }
                        items(subtitleTracks.indices.toList()) { index ->
                            val track = subtitleTracks[index]
                            TrackItem(
                                label = track.label,
                                isSelected = index == selectedSubtitleIndex,
                                onClick = { onSubtitleTrackSelect(index) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
