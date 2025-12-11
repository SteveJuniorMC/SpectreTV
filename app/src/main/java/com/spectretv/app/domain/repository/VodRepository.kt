package com.spectretv.app.domain.repository

import com.spectretv.app.domain.model.Episode
import com.spectretv.app.domain.model.Movie
import com.spectretv.app.domain.model.Series
import com.spectretv.app.domain.model.Source
import kotlinx.coroutines.flow.Flow

interface VodRepository {
    // Movies
    fun getAllMovies(): Flow<List<Movie>>
    fun getMoviesByGenre(genre: String): Flow<List<Movie>>
    fun getFavoriteMovies(): Flow<List<Movie>>
    fun getMovieGenres(): Flow<List<String>>
    fun searchMovies(query: String): Flow<List<Movie>>
    suspend fun getMovieById(id: String): Movie?
    suspend fun refreshMovies(source: Source)
    suspend fun toggleMovieFavorite(movieId: String)

    // Series
    fun getAllSeries(): Flow<List<Series>>
    fun getSeriesByGenre(genre: String): Flow<List<Series>>
    fun getFavoriteSeries(): Flow<List<Series>>
    fun getSeriesGenres(): Flow<List<String>>
    fun searchSeries(query: String): Flow<List<Series>>
    suspend fun getSeriesById(id: String): Series?
    suspend fun getEpisodes(seriesId: String): List<Episode>
    suspend fun refreshSeries(source: Source)
    suspend fun toggleSeriesFavorite(seriesId: String)

    // Cleanup
    suspend fun deleteVodBySource(sourceId: Long)
}
