package com.spectretv.app.presentation.screens.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.spectretv.app.data.local.preferences.VideoQuality
import kotlinx.coroutines.delay
import java.util.Locale

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    streamUrl: String,
    title: String,
    isInPipMode: Boolean,
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val videoQuality by viewModel.videoQuality.collectAsState(initial = VideoQuality.AUTO)
    val context = LocalContext.current

    var showControls by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    // Initialize ViewModel
    LaunchedEffect(streamUrl, title) {
        viewModel.initialize(streamUrl, title)
    }

    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredAudioLanguage(Locale.getDefault().language)
                    .setPreferredTextLanguage(Locale.getDefault().language)
            )
        }
    }

    // Apply video quality constraints
    LaunchedEffect(videoQuality) {
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setMaxVideoSize(Int.MAX_VALUE, videoQuality.maxHeight)
                .setMaxVideoBitrate(videoQuality.maxBitrate)
        )
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build().apply {
                playWhenReady = true
            }
    }

    // Handle back button - call onBackClick which will enter PiP
    BackHandler(enabled = !isInPipMode) {
        onBackClick()
    }

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    // Set up player when stream URL is available
    LaunchedEffect(streamUrl) {
        if (streamUrl.isNotEmpty()) {
            val mediaItem = MediaItem.fromUri(streamUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    // Sync play state
    LaunchedEffect(uiState.isPlaying) {
        exoPlayer.playWhenReady = uiState.isPlaying
    }

    // Player listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.message ?: "Playback error"
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.setPlaying(isPlaying)
            }

            override fun onTracksChanged(tracks: Tracks) {
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
                                    ?: "Subtitle ${subtitleTracks.size + 1}"
                                subtitleTracks.add(
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

                viewModel.updateAudioTracks(audioTracks)
                viewModel.updateSubtitleTracks(subtitleTracks)
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Handle track selection
    LaunchedEffect(uiState.selectedAudioIndex) {
        val audioTracks = uiState.audioTracks
        if (audioTracks.isNotEmpty() && uiState.selectedAudioIndex < audioTracks.size) {
            val track = audioTracks[uiState.selectedAudioIndex]
            val trackGroup = exoPlayer.currentTracks.groups.getOrNull(track.groupIndex)
            trackGroup?.let { group ->
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .setOverrideForType(
                            TrackSelectionOverride(group.mediaTrackGroup, track.index)
                        )
                )
            }
        }
    }

    LaunchedEffect(uiState.selectedSubtitleIndex) {
        if (uiState.selectedSubtitleIndex == -1) {
            // Disable subtitles
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .setPreferredTextLanguage(null)
            )
        } else {
            val subtitleTracks = uiState.subtitleTracks
            if (subtitleTracks.isNotEmpty() && uiState.selectedSubtitleIndex < subtitleTracks.size) {
                val track = subtitleTracks[uiState.selectedSubtitleIndex]
                val trackGroup = exoPlayer.currentTracks.groups.getOrNull(track.groupIndex)
                trackGroup?.let { group ->
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setOverrideForType(
                                TrackSelectionOverride(group.mediaTrackGroup, track.index)
                            )
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                showControls = !showControls
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

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

        playbackError?.let { error ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Hide controls in PiP mode
        if (showControls && !isInPipMode) {
            PlayerControls(
                title = title,
                isPlaying = uiState.isPlaying,
                hasAudioTracks = uiState.audioTracks.size > 1,
                hasSubtitleTracks = uiState.subtitleTracks.isNotEmpty(),
                onBackClick = onBackClick,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onTrackSettingsClick = { viewModel.toggleTrackSelector() }
            )
        }

        // Track selection bottom sheet (not in PiP)
        if (uiState.showTrackSelector && !isInPipMode) {
            TrackSelectionSheet(
                audioTracks = uiState.audioTracks,
                subtitleTracks = uiState.subtitleTracks,
                selectedAudioIndex = uiState.selectedAudioIndex,
                selectedSubtitleIndex = uiState.selectedSubtitleIndex,
                onAudioTrackSelect = { viewModel.selectAudioTrack(it) },
                onSubtitleTrackSelect = { viewModel.selectSubtitleTrack(it) },
                onDismiss = { viewModel.hideTrackSelector() }
            )
        }
    }
}

@Composable
private fun PlayerControls(
    title: String,
    isPlaying: Boolean,
    hasAudioTracks: Boolean,
    hasSubtitleTracks: Boolean,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onTrackSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            if (hasAudioTracks || hasSubtitleTracks) {
                IconButton(onClick = onTrackSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Track settings",
                        tint = Color.White
                    )
                }
            }
        }

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
                    .clickable(onClick = onPlayPauseClick),
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
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
