package com.spectretv.app.presentation.screens.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectretv.app.domain.model.Movie
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

data class MoviesUiState(
    val movies: List<Movie> = emptyList(),
    val filteredMovies: List<Movie> = emptyList(),
    val genres: List<String> = emptyList(),
    val selectedGenre: String? = null,
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val sources: List<Source> = emptyList()
)

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val vodRepository: VodRepository,
    private val sourceRepository: SourceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                vodRepository.getAllMovies(),
                vodRepository.getMovieGenres(),
                sourceRepository.getAllSources()
            ) { movies, genres, sources ->
                Triple(movies, genres, sources)
            }.collect { (movies, genres, sources) ->
                _uiState.value = _uiState.value.copy(
                    movies = movies,
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
        var filtered = state.movies

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

        _uiState.value = _uiState.value.copy(filteredMovies = filtered)
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

    fun refreshMovies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val sources = sourceRepository.getActiveSources().first()
                sources.forEach { source ->
                    vodRepository.refreshMovies(source)
                }
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to refresh movies"
                )
            }
        }
    }

    fun toggleFavorite(movieId: String) {
        viewModelScope.launch {
            vodRepository.toggleMovieFavorite(movieId)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
