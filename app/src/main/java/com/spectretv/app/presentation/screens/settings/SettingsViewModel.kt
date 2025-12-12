package com.spectretv.app.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectretv.app.data.local.preferences.AppPreferences
import com.spectretv.app.data.local.preferences.VideoQuality
import com.spectretv.app.domain.model.Source
import com.spectretv.app.domain.model.SourceType
import com.spectretv.app.data.remote.XtreamClient
import com.spectretv.app.domain.repository.ChannelRepository
import com.spectretv.app.domain.repository.SourceRepository
import com.spectretv.app.domain.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val sources: List<Source> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showAddDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val channelRepository: ChannelRepository,
    private val vodRepository: VodRepository,
    private val xtreamClient: XtreamClient,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val videoQuality: StateFlow<VideoQuality> = appPreferences.videoQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VideoQuality.AUTO)

    init {
        loadSources()
    }

    private fun loadSources() {
        viewModelScope.launch {
            sourceRepository.getAllSources().collect { sources ->
                _uiState.value = _uiState.value.copy(sources = sources)
            }
        }
    }

    fun setVideoQuality(quality: VideoQuality) {
        viewModelScope.launch {
            appPreferences.setVideoQuality(quality)
        }
    }

    fun showAddSourceDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun hideAddSourceDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun addM3USource(name: String, url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, showAddDialog = false)
            try {
                val source = Source(
                    name = name,
                    type = SourceType.M3U,
                    url = url
                )

                // Validate URL is accessible before saving
                val isValid = channelRepository.validateM3USource(source)
                if (!isValid) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Could not connect to M3U URL. Please check the URL and try again."
                    )
                    return@launch
                }

                // Save source without loading content
                sourceRepository.addSource(source)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Source added successfully. Go to Live TV to load channels."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to add source"
                )
            }
        }
    }

    fun addXtreamSource(name: String, serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, showAddDialog = false)
            try {
                val source = Source(
                    name = name,
                    type = SourceType.XTREAM,
                    serverUrl = serverUrl,
                    username = username,
                    password = password
                )

                // Validate credentials before saving
                val isAuthenticated = xtreamClient.authenticate(source)
                if (!isAuthenticated) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Authentication failed. Please check server URL, username, and password."
                    )
                    return@launch
                }

                // Save source without loading content
                sourceRepository.addSource(source)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Source added successfully. Go to content tabs to load channels, movies, and series."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to add source"
                )
            }
        }
    }

    fun deleteSource(source: Source) {
        viewModelScope.launch {
            try {
                channelRepository.deleteChannelsBySource(source.id)
                vodRepository.deleteVodBySource(source.id)
                sourceRepository.deleteSource(source)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete source"
                )
            }
        }
    }

    fun refreshSource(source: Source) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                channelRepository.refreshChannels(source)
                if (source.type == SourceType.XTREAM) {
                    vodRepository.refreshMovies(source)
                    vodRepository.refreshSeries(source)
                }
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to refresh source"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}
