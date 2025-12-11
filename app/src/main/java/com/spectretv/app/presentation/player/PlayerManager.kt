package com.spectretv.app.presentation.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class PlayingStream(
    val url: String,
    val title: String
)

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
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

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
        }
    }

    fun play(url: String, title: String) {
        // If same stream, just ensure it's playing
        if (currentStream?.url == url) {
            _exoPlayer?.play()
            isFullScreen = true
            return
        }

        // New stream - create or reuse player
        currentStream = PlayingStream(url, title)
        isFullScreen = true

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
    }

    fun togglePlayPause() {
        _exoPlayer?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun minimize() {
        isFullScreen = false
    }

    fun expand() {
        isFullScreen = true
    }

    fun stop() {
        _exoPlayer?.stop()
        currentStream = null
        isFullScreen = false
    }

    fun release() {
        _exoPlayer?.apply {
            removeListener(playerListener)
            release()
        }
        _exoPlayer = null
        currentStream = null
        isFullScreen = false
    }
}
