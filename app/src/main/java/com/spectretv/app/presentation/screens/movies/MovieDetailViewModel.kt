package com.spectretv.app.presentation.screens.movies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectretv.app.domain.model.Movie
import com.spectretv.app.domain.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieDetailUiState(
    val movie: Movie? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val vodRepository: VodRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: String = savedStateHandle.get<String>("movieId") ?: ""

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    init {
        loadMovie()
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
