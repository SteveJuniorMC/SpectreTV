package com.spectretv.app.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectretv.app.domain.model.Channel
import com.spectretv.app.domain.model.Source
import com.spectretv.app.domain.repository.ChannelRepository
import com.spectretv.app.domain.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val channels: List<Channel> = emptyList(),
    val groups: List<String> = emptyList(),
    val selectedGroup: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val sources: List<Source> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val sourceRepository: SourceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                channelRepository.getAllChannels(),
                channelRepository.getAllGroups(),
                sourceRepository.getAllSources()
            ) { channels, groups, sources ->
                Triple(channels, groups, sources)
            }.collect { (channels, groups, sources) ->
                val selectedGroup = _uiState.value.selectedGroup
                val filteredChannels = if (selectedGroup != null) {
                    channels.filter { it.group == selectedGroup }
                } else {
                    channels
                }

                _uiState.value = _uiState.value.copy(
                    channels = filteredChannels,
                    groups = listOf("All") + groups,
                    sources = sources,
                    isLoading = false
                )
            }
        }
    }

    fun selectGroup(group: String?) {
        val actualGroup = if (group == "All") null else group
        _uiState.value = _uiState.value.copy(selectedGroup = actualGroup)

        viewModelScope.launch {
            val channels = if (actualGroup != null) {
                channelRepository.getChannelsByGroup(actualGroup).first()
            } else {
                channelRepository.getAllChannels().first()
            }
            _uiState.value = _uiState.value.copy(channels = channels)
        }
    }

    fun refreshChannels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val sources = sourceRepository.getActiveSources().first()
                sources.forEach { source ->
                    channelRepository.refreshChannels(source)
                }
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to refresh channels"
                )
            }
        }
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channelId)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
