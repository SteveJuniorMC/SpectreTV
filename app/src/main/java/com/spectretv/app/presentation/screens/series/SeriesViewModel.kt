package com.spectretv.app.presentation.screens.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectretv.app.domain.model.Series
import com.spectretv.app.domain.model.Source
import com.spectretv.app.domain.repository.SourceRepository
import com.spectretv.app.domain.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeriesUiState(
    val series: List<Series> = emptyList(),
    val filteredSeries: List<Series> = emptyList(),
    val genres: List<String> = emptyList(),
    val selectedGenre: String? = null,
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val sources: List<Source> = emptyList(),
    val debugInfo: String? = null
)

@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val vodRepository: VodRepository,
    private val sourceRepository: SourceRepository
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
                sourceRepository.getAllSources()
            ) { series, genres, sources ->
                Triple(series, genres, sources)
            }.collect { (series, genres, sources) ->
                _uiState.value = _uiState.value.copy(
                    series = series,
                    genres = listOf("All") + genres,
                    sources = sources,
                    isLoading = false
                )
                applyFilters()
            }
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = state.series

        if (state.selectedGenre != null) {
            filtered = filtered.filter { it.genre == state.selectedGenre }
        }

        if (state.showFavoritesOnly) {
            filtered = filtered.filter { it.isFavorite }
        }

        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(state.searchQuery, ignoreCase = true)
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
}
