package com.spectretv.app.presentation.screens.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectretv.app.data.local.entity.WatchHistoryEntity
import com.spectretv.app.domain.model.Series
import com.spectretv.app.domain.model.Source
import com.spectretv.app.domain.repository.SourceRepository
import com.spectretv.app.domain.repository.VodRepository
import com.spectretv.app.domain.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SeriesSortOption(val label: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    YEAR_DESC("Year (Newest)"),
    YEAR_ASC("Year (Oldest)"),
    RATING_DESC("Rating (Highest)"),
    RATING_ASC("Rating (Lowest)")
}

data class SeriesUiState(
    val series: List<Series> = emptyList(),
    val filteredSeries: List<Series> = emptyList(),
    val genres: List<String> = emptyList(),
    val selectedGenre: String? = null,
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val sortOption: SeriesSortOption = SeriesSortOption.NAME_ASC,
    val isLoading: Boolean = false,
    val error: String? = null,
    val sources: List<Source> = emptyList(),
    val debugInfo: String? = null,
    val recentlyWatched: List<WatchHistoryEntity> = emptyList()
)

@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val vodRepository: VodRepository,
    private val sourceRepository: SourceRepository,
    private val watchHistoryRepository: WatchHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                vodRepository.getAllSeries(),
                vodRepository.getSeriesGenres(),
                sourceRepository.getAllSources(),
                watchHistoryRepository.getRecentEpisodes(10)
            ) { series, genres, sources, recentlyWatched ->
                CombinedData(series, genres, sources, recentlyWatched)
            }.collect { data ->
                val genreList = mutableListOf("All")
                if (data.recentlyWatched.isNotEmpty()) {
                    genreList.add("History")
                }
                genreList.addAll(data.genres)

                _uiState.value = _uiState.value.copy(
                    series = data.series,
                    genres = genreList,
                    sources = data.sources,
                    recentlyWatched = data.recentlyWatched,
                    isLoading = false
                )
                applyFilters()
            }
        }
    }

    private data class CombinedData(
        val series: List<Series>,
        val genres: List<String>,
        val sources: List<Source>,
        val recentlyWatched: List<WatchHistoryEntity>
    )

    private fun applyFilters() {
        val state = _uiState.value
        var filtered: List<Series>

        // Handle History filter specially - show series with recently watched episodes
        if (state.selectedGenre == "History") {
            val historySeriesIds = state.recentlyWatched.mapNotNull { it.seriesId }.distinct()
            filtered = historySeriesIds.mapNotNull { id -> state.series.find { it.id == id } }
        } else {
            filtered = state.series

            if (state.selectedGenre != null) {
                filtered = filtered.filter { it.genre == state.selectedGenre }
            }
        }

        if (state.showFavoritesOnly) {
            filtered = filtered.filter { it.isFavorite }
        }

        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(state.searchQuery, ignoreCase = true)
            }
        }

        // Apply sorting (skip for History to preserve watch order)
        if (state.selectedGenre != "History") {
            filtered = when (state.sortOption) {
                SeriesSortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
                SeriesSortOption.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
                SeriesSortOption.YEAR_DESC -> filtered.sortedByDescending { it.year ?: "0" }
                SeriesSortOption.YEAR_ASC -> filtered.sortedBy { it.year ?: "9999" }
                SeriesSortOption.RATING_DESC -> filtered.sortedByDescending { it.rating?.toDoubleOrNull() ?: 0.0 }
                SeriesSortOption.RATING_ASC -> filtered.sortedBy { it.rating?.toDoubleOrNull() ?: 0.0 }
            }
        }

        _uiState.value = _uiState.value.copy(filteredSeries = filtered)
    }

    fun selectGenre(genre: String?) {
        val actualGenre = if (genre == "All") null else genre
        _uiState.value = _uiState.value.copy(selectedGenre = actualGenre)
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

    fun setSortOption(option: SeriesSortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
        applyFilters()
    }

    fun refreshSeries() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, debugInfo = null)
            try {
                val sources = sourceRepository.getActiveSources().first()
                val debugLines = mutableListOf<String>()
                debugLines.add("Active sources: ${sources.size}")

                sources.forEach { source ->
                    debugLines.add("--- ${source.name} ---")
                    val debug = vodRepository.refreshSeriesWithDebug(source)
                    debugLines.add(debug)
                }

                // Get the updated series count
                val seriesCount = vodRepository.getAllSeries().first().size
                debugLines.add("--- Total in DB: $seriesCount ---")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    debugInfo = debugLines.joinToString("\n")
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to refresh series",
                    debugInfo = "Error: ${e.message}"
                )
            }
        }
    }

    fun toggleFavorite(seriesId: String) {
        viewModelScope.launch {
            vodRepository.toggleSeriesFavorite(seriesId)
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
