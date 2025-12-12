package com.spectretv.app.presentation.screens.movies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectretv.app.domain.model.Movie
import com.spectretv.app.domain.repository.VodRepository
import com.spectretv.app.domain.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieDetailUiState(
    val movie: Movie? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val resumePosition: Long? = null,
    val progressPercent: Float = 0f
)

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val vodRepository: VodRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: String = savedStateHandle.get<String>("movieId") ?: ""

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    init {
        loadMovie()
        loadWatchHistory()
    }

    private fun loadMovie() {
        viewModelScope.launch {
            try {
                val movie = vodRepository.getMovieById(movieId)
                _uiState.value = _uiState.value.copy(
                    movie = movie,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load movie"
                )
            }
        }
    }

    private fun loadWatchHistory() {
        viewModelScope.launch {
            val history = watchHistoryRepository.getByContentId(movieId)
            if (history != null && history.positionMs > 0 && history.durationMs > 0) {
                val progress = history.positionMs.toFloat() / history.durationMs.toFloat()
                // Only show resume if not near the end (< 95%)
                if (progress < 0.95f) {
                    _uiState.value = _uiState.value.copy(
                        resumePosition = history.positionMs,
                        progressPercent = progress
                    )
                } else {
                    // Clear resume state if completed
                    _uiState.value = _uiState.value.copy(
                        resumePosition = null,
                        progressPercent = 0f
                    )
                }
            } else {
                // Clear resume state if no history
                _uiState.value = _uiState.value.copy(
                    resumePosition = null,
                    progressPercent = 0f
                )
            }
        }
    }

    fun refreshWatchHistory() {
        loadWatchHistory()
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            _uiState.value.movie?.let { movie ->
                vodRepository.toggleMovieFavorite(movie.id)
                _uiState.value = _uiState.value.copy(
                    movie = movie.copy(isFavorite = !movie.isFavorite)
                )
            }
        }
    }
}
