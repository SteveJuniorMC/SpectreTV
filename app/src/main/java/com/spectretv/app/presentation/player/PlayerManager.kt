package com.spectretv.app.presentation.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.spectretv.app.data.local.entity.WatchHistoryEntity
import com.spectretv.app.domain.repository.WatchHistoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class ContentType {
    LIVE,
    VOD
}

data class PlayingStream(
    val url: String,
    val title: String,
    val contentType: ContentType = ContentType.LIVE,
    val contentId: String? = null,  // For tracking watch history
    val posterUrl: String? = null,
    val seriesId: String? = null,
    val seriesName: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null
)

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val watchHistoryRepository: WatchHistoryRepository
) {
    var currentStream by mutableStateOf<PlayingStream?>(null)
        private set

    var isFullScreen by mutableStateOf(false)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer?
        get() = _exoPlayer

    private var trackSelector: DefaultTrackSelector? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
            // Save progress when pausing
            if (!playing) {
                saveCurrentProgress()
            }
        }
    }

    private fun saveCurrentProgress() {
        val stream = currentStream ?: return
        val contentId = stream.contentId ?: return
        val player = _exoPlayer ?: return

        // Get values on main thread before launching coroutine
        val position = player.currentPosition
        val duration = player.duration.takeIf { it > 0 } ?: 0L

        scope.launch {
            watchHistoryRepository.updateProgress(contentId, position, duration)
        }
    }

    private fun addToWatchHistory(stream: PlayingStream) {
        val contentId = stream.contentId ?: return

        scope.launch {
            val contentTypeStr = when (stream.contentType) {
                ContentType.LIVE -> "channel"
                ContentType.VOD -> if (stream.seriesId != null) "episode" else "movie"
            }

            val entry = WatchHistoryEntity(
                contentId = contentId,
                contentType = contentTypeStr,
                name = stream.title,
                posterUrl = stream.posterUrl,
                streamUrl = stream.url,
                seriesId = stream.seriesId,
                seriesName = stream.seriesName,
                seasonNumber = stream.seasonNumber,
                episodeNumber = stream.episodeNumber
            )
            watchHistoryRepository.addToHistory(entry)
        }
    }

    fun play(
        url: String,
        title: String,
        contentType: ContentType = ContentType.LIVE,
        contentId: String? = null,
        posterUrl: String? = null,
        seriesId: String? = null,
        seriesName: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        startPosition: Long? = null // null = auto-resume, 0 = start from beginning, positive = specific position
    ) {
        // Save progress of current stream before switching
        if (currentStream?.url != url) {
            saveCurrentProgress()
        }

        // If same stream, just ensure it's playing
        if (currentStream?.url == url) {
            _exoPlayer?.play()
            isFullScreen = true
            return
        }

        // New stream - create or reuse player
        val stream = PlayingStream(
            url = url,
            title = title,
            contentType = contentType,
            contentId = contentId,
            posterUrl = posterUrl,
            seriesId = seriesId,
            seriesName = seriesName,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )
        currentStream = stream
        isFullScreen = true

        // Add to watch history
        addToWatchHistory(stream)

        if (_exoPlayer == null) {
            trackSelector = DefaultTrackSelector(context).apply {
                setParameters(
                    buildUponParameters()
                        .setPreferredAudioLanguage(Locale.getDefault().language)
                        .setPreferredTextLanguage(Locale.getDefault().language)
                )
            }
            _exoPlayer = ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector!!)
                .build().apply {
                    addListener(playerListener)
                    playWhenReady = true
                }
        }

        _exoPlayer?.apply {
            stop()
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            play()
        }

        // Handle seeking for VOD content
        if (contentType == ContentType.VOD && contentId != null) {
            when {
                startPosition != null -> {
                    // Explicit position provided (0 = start from beginning, positive = specific position)
                    if (startPosition > 0) {
                        _exoPlayer?.seekTo(startPosition)
                    }
                    // startPosition == 0 means play from start, no seek needed
                }
                else -> {
                    // Auto-resume from saved position
                    scope.launch {
                        val savedPosition = watchHistoryRepository.getByContentId(contentId)?.positionMs ?: 0L
                        if (savedPosition > 0) {
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                _exoPlayer?.seekTo(savedPosition)
                            }
                        }
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        _exoPlayer?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(positionMs: Long) {
        _exoPlayer?.seekTo(positionMs)
    }

    fun skipForward(ms: Long = 10000) {
        _exoPlayer?.let {
            val newPosition = (it.currentPosition + ms).coerceAtMost(it.duration)
            it.seekTo(newPosition)
        }
    }

    fun skipBackward(ms: Long = 10000) {
        _exoPlayer?.let {
            val newPosition = (it.currentPosition - ms).coerceAtLeast(0)
            it.seekTo(newPosition)
        }
    }

    fun minimize() {
        isFullScreen = false
    }

    fun expand() {
        isFullScreen = true
    }

    fun stop() {
        saveCurrentProgress()
        _exoPlayer?.stop()
        currentStream = null
        isFullScreen = false
    }

    fun release() {
        saveCurrentProgress()
        _exoPlayer?.apply {
            removeListener(playerListener)
            release()
        }
        _exoPlayer = null
        currentStream = null
        isFullScreen = false
    }
}
