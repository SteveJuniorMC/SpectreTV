package com.spectretv.app.presentation.screens.live

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

data class LiveUiState(
    val channels: List<Channel> = emptyList(),
    val filteredChannels: List<Channel> = emptyList(),
    val groups: List<String> = emptyList(),
    val selectedGroup: String? = null,
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val sources: List<Source> = emptyList()
)

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val sourceRepository: SourceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

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
                _uiState.value = _uiState.value.copy(
                    channels = channels,
                    groups = listOf("All") + groups,
                    sources = sources,
                    isLoading = false
                )
                applyFilters()
            }
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = state.channels

        // Apply group filter
        if (state.selectedGroup != null) {
            filtered = filtered.filter { it.group == state.selectedGroup }
        }

        // Apply favorites filter
        if (state.showFavoritesOnly) {
            filtered = filtered.filter { it.isFavorite }
        }

        // Apply search filter
        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(state.searchQuery, ignoreCase = true)
            }
        }

        _uiState.value = _uiState.value.copy(filteredChannels = filtered)
    }

    fun selectGroup(group: String?) {
        val actualGroup = if (group == "All") null else group
        _uiState.value = _uiState.value.copy(selectedGroup = actualGroup)
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun toggleFavoritesFilter() {
        _uiState.value = _uiState.value.copy(
            showFavoritesOnly = !_uiState.value.showFavoritesOnly
        )
        applyFilters()
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
