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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class AspectRatioMode(val label: String, val value: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FIXED_WIDTH("Fixed Width", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH),
    FIXED_HEIGHT("Fixed Height", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT)
}

private fun formatDuration(ms: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

data class TrackInfo(
    val index: Int,
    val groupIndex: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean,
    val trackType: Int
)

@androidx.annotation.OptIn(UnstableApi::class)
private fun parseTracksFromGroups(tracks: Tracks): Pair<List<TrackInfo>, List<TrackInfo>> {
    val audioTracks = mutableListOf<TrackInfo>()
    val subtitleTracks = mutableListOf<TrackInfo>()

    tracks.groups.forEachIndexed { groupIndex, group ->
        when (group.type) {
            C.TRACK_TYPE_AUDIO -> {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val label = format.label
                        ?: format.language?.let { Locale(it).displayLanguage }
                        ?: "Audio ${audioTracks.size + 1}"
                    audioTracks.add(
                        TrackInfo(
                            index = i,
                            groupIndex = groupIndex,
                            label = label,
                            language = format.language,
                            isSelected = group.isTrackSelected(i),
                            trackType = C.TRACK_TYPE_AUDIO
                        )
                    )
                }
            }
            C.TRACK_TYPE_TEXT -> {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val label = format.label
                        ?: format.language?.let { Locale(it).displayLanguage }
                        ?: "Subtitle ${subtitleTracks.size + 1}"
                    subtitleTracks.add(
                        TrackInfo(
                            index = i,
                            groupIndex = groupIndex,
                            label = label,
                            language = format.language,
                            isSelected = group.isTrackSelected(i),
                            trackType = C.TRACK_TYPE_TEXT
                        )
                    )
                }
            }
        }
    }

    return Pair(audioTracks, subtitleTracks)
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun updateTracks(
    tracks: Tracks,
    audioTracksList: MutableList<TrackInfo>,
    subtitleTracksList: MutableList<TrackInfo>,
    onIndicesUpdated: (Int, Int) -> Unit
) {
    val (newAudio, newSubtitles) = parseTracksFromGroups(tracks)

    audioTracksList.clear()
    audioTracksList.addAll(newAudio)
    subtitleTracksList.clear()
    subtitleTracksList.addAll(newSubtitles)

    val audioIdx = newAudio.indexOfFirst { it.isSelected }.coerceAtLeast(0)
    val subIdx = newSubtitles.indexOfFirst { it.isSelected }
    onIndicesUpdated(audioIdx, subIdx)
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun FullScreenPlayer(
    title: String,
    exoPlayer: ExoPlayer?,
    isPlaying: Boolean,
    contentType: ContentType = ContentType.LIVE,
    isInPipMode: Boolean = false,
    onMinimize: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit = {},
    onSkipForward: () -> Unit = {},
    onSkipBackward: () -> Unit = {},
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var showControls by remember { mutableStateOf(true) }

    // Hide controls when entering PiP mode
    LaunchedEffect(isInPipMode) {
        if (isInPipMode) {
            showControls = false
        }
    }
    // Start with buffering true until we know the player is ready
    var isBuffering by remember { mutableStateOf(true) }
    var showTrackSelector by remember { mutableStateOf(false) }

    // Track info - keyed by title to reset when stream changes
    val audioTracks = remember(title) { mutableStateListOf<TrackInfo>() }
    val subtitleTracks = remember(title) { mutableStateListOf<TrackInfo>() }
    var selectedAudioIndex by remember(title) { mutableIntStateOf(0) }
    var selectedSubtitleIndex by remember(title) { mutableIntStateOf(-1) }

    // Playback position state for VOD content
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    // Aspect ratio state
    var aspectRatioMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    var showAspectRatioMenu by remember { mutableStateOf(false) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    // Error state
    var playbackError by remember(title) { mutableStateOf<String?>(null) }

    val isVod = contentType == ContentType.VOD

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
        if (showControls && !showTrackSelector && !showAspectRatioMenu) {
            delay(4000)
            showControls = false
        }
    }

    // Update playback position for VOD content
    LaunchedEffect(isVod, isPlaying) {
        if (isVod && isPlaying && !isSeeking) {
            while (true) {
                exoPlayer?.let { player ->
                    currentPosition = player.currentPosition
                    if (player.duration > 0) {
                        duration = player.duration
                    }
                }
                delay(1000)
            }
        }
    }

    // Listen for playback state and tracks - keyed on title to refresh when stream changes
    DisposableEffect(exoPlayer, title) {
        // Check current state immediately
        exoPlayer?.let { player ->
            val state = player.playbackState
            isBuffering = state == Player.STATE_BUFFERING || state == Player.STATE_IDLE

            // Get initial position and duration for VOD
            if (isVod) {
                currentPosition = player.currentPosition
                if (player.duration > 0) {
                    duration = player.duration
                }
            }

            // Also check current tracks immediately
            updateTracks(player.currentTracks, audioTracks, subtitleTracks) { audioIdx, subIdx ->
                selectedAudioIndex = audioIdx
                selectedSubtitleIndex = subIdx
            }
        }

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                // Clear error when playback starts successfully
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracks(tracks, audioTracks, subtitleTracks) { audioIdx, subIdx ->
                    selectedAudioIndex = audioIdx
                    selectedSubtitleIndex = subIdx
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val errorMessage = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                        "Network connection failed"
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Connection timed out"
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        "Server error (HTTP ${error.cause?.message ?: "error"})"
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                        "Stream not found"
                    PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
                        "Access denied"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                        "Invalid stream format"
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                        "Decoder initialization failed"
                    PlaybackException.ERROR_CODE_DECODING_FAILED ->
                        "Decoding error"
                    PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ->
                        "Audio initialization failed"
                    else ->
                        error.message ?: "Playback error"
                }
                playbackError = errorMessage
                isBuffering = false
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
                        resizeMode = aspectRatioMode.value
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        playerView = this
                    }
                },
                update = { view ->
                    view.resizeMode = aspectRatioMode.value
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Buffering indicator
        if (isBuffering && playbackError == null) {
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

        // Error display
        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Playback Error",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playbackError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            playbackError = null
                            isBuffering = true
                            onRetry()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        }

        // Controls overlay
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                // Top bar with back, title, aspect ratio, and settings
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

                    // Aspect ratio button
                    Box {
                        IconButton(onClick = { showAspectRatioMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Aspect ratio",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showAspectRatioMenu,
                            onDismissRequest = { showAspectRatioMenu = false }
                        ) {
                            AspectRatioMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(mode.label)
                                            if (mode == aspectRatioMode) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        aspectRatioMode = mode
                                        showAspectRatioMenu = false
                                    }
                                )
                            }
                        }
                    }

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

                // Center controls (skip back, play/pause, skip forward) - hide when buffering
                if (!isBuffering) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Skip backward (VOD only)
                        if (isVod) {
                            IconButton(
                                onClick = onSkipBackward,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Skip back 10 seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(24.dp))
                        }

                        // Play/Pause button
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

                        // Skip forward (VOD only)
                        if (isVod) {
                            Spacer(modifier = Modifier.width(24.dp))
                            IconButton(
                                onClick = onSkipForward,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Skip forward 10 seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom seek bar (VOD only)
                if (isVod && duration > 0) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
                        Slider(
                            value = if (isSeeking) seekPosition else (currentPosition.toFloat() / duration.toFloat()),
                            onValueChange = { value ->
                                isSeeking = true
                                seekPosition = value
                            },
                            onValueChangeFinished = {
                                val newPosition = (seekPosition * duration).toLong()
                                onSeekTo(newPosition)
                                currentPosition = newPosition
                                isSeeking = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(if (isSeeking) (seekPosition * duration).toLong() else currentPosition),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                            Text(
                                text = formatDuration(duration),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
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
