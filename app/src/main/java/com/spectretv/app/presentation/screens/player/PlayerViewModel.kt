package com.spectretv.app.presentation.screens.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectretv.app.domain.model.Channel
import com.spectretv.app.domain.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val channel: Channel? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isPlaying: Boolean = true
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val channelRepository: ChannelRepository
) : ViewModel() {

    private val channelId: String = savedStateHandle.get<String>("channelId") ?: ""

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        loadChannel()
    }

    private fun loadChannel() {
        viewModelScope.launch {
            try {
                val channel = channelRepository.getChannelById(channelId)
                _uiState.value = _uiState.value.copy(
                    channel = channel,
                    isLoading = false,
                    error = if (channel == null) "Channel not found" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load channel"
                )
            }
        }
    }

    fun togglePlayPause() {
        _uiState.value = _uiState.value.copy(
            isPlaying = !_uiState.value.isPlaying
        )
    }

    fun setPlaying(playing: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = playing)
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            _uiState.value.channel?.let { channel ->
                channelRepository.toggleFavorite(channel.id)
                _uiState.value = _uiState.value.copy(
                    channel = channel.copy(isFavorite = !channel.isFavorite)
                )
            }
        }
    }
}
