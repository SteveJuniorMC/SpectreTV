package com.spectretv.app.presentation.screens.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectretv.app.data.local.entity.WatchHistoryEntity
import com.spectretv.app.domain.model.Channel
import com.spectretv.app.domain.model.Source
import com.spectretv.app.domain.repository.ChannelRepository
import com.spectretv.app.domain.repository.LoadingProgress
import com.spectretv.app.domain.repository.SourceRepository
import com.spectretv.app.domain.repository.WatchHistoryRepository
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
    val loadingProgress: LoadingProgress? = null,
    val error: String? = null,
    val sources: List<Source> = emptyList(),
    val recentlyWatched: List<WatchHistoryEntity> = emptyList()
)

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val sourceRepository: SourceRepository,
    private val watchHistoryRepository: WatchHistoryRepository
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
                sourceRepository.getAllSources(),
                watchHistoryRepository.getRecentChannels(10)
            ) { channels, groups, sources, recentlyWatched ->
                CombinedData(channels, groups, sources, recentlyWatched)
            }.collect { data ->
                val groupList = mutableListOf("All")
                if (data.recentlyWatched.isNotEmpty()) {
                    groupList.add("Recent")
                }
                groupList.addAll(data.groups)

                _uiState.value = _uiState.value.copy(
                    channels = data.channels,
                    groups = groupList,
                    sources = data.sources,
                    recentlyWatched = data.recentlyWatched,
                    isLoading = false
                )
                applyFilters()
            }
        }
    }

    private data class CombinedData(
        val channels: List<Channel>,
        val groups: List<String>,
        val sources: List<Source>,
        val recentlyWatched: List<WatchHistoryEntity>
    )

    private fun applyFilters() {
        val state = _uiState.value
        var filtered: List<Channel>

        // Handle Recent filter specially
        if (state.selectedGroup == "Recent") {
            val historyIds = state.recentlyWatched.map { it.contentId }
            filtered = historyIds.mapNotNull { id -> state.channels.find { it.id == id } }
        } else {
            filtered = state.channels

            // Apply group filter
            if (state.selectedGroup != null) {
                filtered = filtered.filter { it.group == state.selectedGroup }
            }
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

    fun loadChannels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingProgress = LoadingProgress(),
                error = null
            )
            try {
                val sources = sourceRepository.getActiveSources().first()
                sources.forEach { source ->
                    channelRepository.refreshChannelsWithProgress(source) { progress ->
                        _uiState.value = _uiState.value.copy(loadingProgress = progress)
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingProgress = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingProgress = null,
                    error = e.message ?: "Failed to load channels"
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

    fun resetSearch() {
        if (_uiState.value.searchQuery.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(searchQuery = "")
            applyFilters()
        }
    }
}
