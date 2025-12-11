package com.spectretv.app.presentation.screens.player

import androidx.lifecycle.ViewModel
import com.spectretv.app.data.local.preferences.AppPreferences
import com.spectretv.app.data.local.preferences.VideoQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class TrackInfo(
    val index: Int,
    val groupIndex: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean = false
)

data class PlayerUiState(
    val title: String = "",
    val streamUrl: String = "",
    val isPlaying: Boolean = true,
    val audioTracks: List<TrackInfo> = emptyList(),
    val subtitleTracks: List<TrackInfo> = emptyList(),
    val selectedAudioIndex: Int = 0,
    val selectedSubtitleIndex: Int = -1,
    val showTrackSelector: Boolean = false
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    val videoQuality: Flow<VideoQuality> = appPreferences.videoQuality

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun initialize(streamUrl: String, title: String) {
        _uiState.value = _uiState.value.copy(
            streamUrl = streamUrl,
            title = title
        )
    }

    fun togglePlayPause() {
        _uiState.value = _uiState.value.copy(isPlaying = !_uiState.value.isPlaying)
    }

    fun setPlaying(playing: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = playing)
    }

    fun updateAudioTracks(tracks: List<TrackInfo>) {
        _uiState.value = _uiState.value.copy(audioTracks = tracks)
    }

    fun updateSubtitleTracks(tracks: List<TrackInfo>) {
        _uiState.value = _uiState.value.copy(subtitleTracks = tracks)
    }

    fun selectAudioTrack(index: Int) {
        _uiState.value = _uiState.value.copy(selectedAudioIndex = index)
    }

    fun selectSubtitleTrack(index: Int) {
        _uiState.value = _uiState.value.copy(selectedSubtitleIndex = index)
    }

    fun toggleTrackSelector() {
        _uiState.value = _uiState.value.copy(
            showTrackSelector = !_uiState.value.showTrackSelector
        )
    }

    fun hideTrackSelector() {
        _uiState.value = _uiState.value.copy(showTrackSelector = false)
    }
}
