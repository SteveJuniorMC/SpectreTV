package com.spectretv.app.presentation.screens.series

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectretv.app.domain.model.Episode
import com.spectretv.app.domain.model.Series
import com.spectretv.app.domain.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeriesDetailUiState(
    val series: Series? = null,
    val episodes: List<Episode> = emptyList(),
    val seasons: List<Int> = emptyList(),
    val selectedSeason: Int? = null,
    val isLoading: Boolean = true,
    val isLoadingEpisodes: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val vodRepository: VodRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val seriesId: String = savedStateHandle.get<String>("seriesId") ?: ""

    private val _uiState = MutableStateFlow(SeriesDetailUiState())
    val uiState: StateFlow<SeriesDetailUiState> = _uiState.asStateFlow()

    init {
        loadSeries()
    }

    private fun loadSeries() {
        viewModelScope.launch {
            try {
                val series = vodRepository.getSeriesById(seriesId)
                _uiState.value = _uiState.value.copy(
                    series = series,
                    isLoading = false
                )
                loadEpisodes()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load series"
                )
            }
        }
    }

    private fun loadEpisodes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingEpisodes = true)
            try {
                val episodes = vodRepository.getEpisodes(seriesId)
                val seasons = episodes.map { it.seasonNumber }.distinct().sorted()
                val selectedSeason = seasons.firstOrNull()

                _uiState.value = _uiState.value.copy(
                    episodes = episodes,
                    seasons = seasons,
                    selectedSeason = selectedSeason,
                    isLoadingEpisodes = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingEpisodes = false,
                    error = e.message ?: "Failed to load episodes"
                )
            }
        }
    }

    fun selectSeason(season: Int) {
        _uiState.value = _uiState.value.copy(selectedSeason = season)
    }

    fun getFilteredEpisodes(): List<Episode> {
        val selectedSeason = _uiState.value.selectedSeason ?: return _uiState.value.episodes
        return _uiState.value.episodes.filter { it.seasonNumber == selectedSeason }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            _uiState.value.series?.let { series ->
                vodRepository.toggleSeriesFavorite(series.id)
                _uiState.value = _uiState.value.copy(
                    series = series.copy(isFavorite = !series.isFavorite)
                )
            }
        }
    }
}
