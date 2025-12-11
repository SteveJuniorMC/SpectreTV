package com.spectretv.app.data.repository

import com.spectretv.app.data.local.dao.EpisodeDao
import com.spectretv.app.data.local.dao.MovieDao
import com.spectretv.app.data.local.dao.SeriesDao
import com.spectretv.app.data.local.dao.SourceDao
import com.spectretv.app.data.local.entity.EpisodeEntity
import com.spectretv.app.data.local.entity.MovieEntity
import com.spectretv.app.data.local.entity.SeriesEntity
import com.spectretv.app.data.remote.XtreamClient
import com.spectretv.app.domain.model.Episode
import com.spectretv.app.domain.model.Movie
import com.spectretv.app.domain.model.Series
import com.spectretv.app.domain.model.Source
import com.spectretv.app.domain.model.SourceType
import com.spectretv.app.domain.repository.VodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VodRepositoryImpl @Inject constructor(
    private val movieDao: MovieDao,
    private val seriesDao: SeriesDao,
    private val episodeDao: EpisodeDao,
    private val sourceDao: SourceDao,
    private val xtreamClient: XtreamClient
) : VodRepository {

    // Movies
    override fun getAllMovies(): Flow<List<Movie>> {
        return movieDao.getAllMovies().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMoviesByGenre(genre: String): Flow<List<Movie>> {
        return movieDao.getMoviesByGenre(genre).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteMovies(): Flow<List<Movie>> {
        return movieDao.getFavoriteMovies().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMovieGenres(): Flow<List<String>> {
        return movieDao.getAllGenres()
    }

    override fun searchMovies(query: String): Flow<List<Movie>> {
        return movieDao.searchMovies(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMovieById(id: String): Movie? {
        return movieDao.getMovieById(id)?.toDomain()
    }

    override suspend fun refreshMovies(source: Source) {
        if (source.type != SourceType.XTREAM) return

        withContext(Dispatchers.IO) {
            val movies = xtreamClient.getMovies(source)

            // Preserve favorites
            val existingMovies = movieDao.getMoviesBySource(source.id).first()
            val existingFavorites = existingMovies
                .filter { it.isFavorite }
                .map { it.id }
                .toSet()

            // Delete old movies from this source
            movieDao.deleteMoviesBySource(source.id)

            // Insert new movies with preserved favorites
            val entities = movies.map { movie ->
                MovieEntity.fromDomain(
                    movie.copy(isFavorite = existingFavorites.contains(movie.id))
                )
            }
            movieDao.insertMovies(entities)
        }
    }

    override suspend fun toggleMovieFavorite(movieId: String) {
        val movie = movieDao.getMovieById(movieId)
        movie?.let {
            movieDao.setFavorite(movieId, !it.isFavorite)
        }
    }

    // Series
    override fun getAllSeries(): Flow<List<Series>> {
        return seriesDao.getAllSeries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSeriesByGenre(genre: String): Flow<List<Series>> {
        return seriesDao.getSeriesByGenre(genre).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteSeries(): Flow<List<Series>> {
        return seriesDao.getFavoriteSeries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSeriesGenres(): Flow<List<String>> {
        return seriesDao.getAllGenres()
    }

    override fun searchSeries(query: String): Flow<List<Series>> {
        return seriesDao.searchSeries(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSeriesById(id: String): Series? {
        return seriesDao.getSeriesById(id)?.toDomain()
    }

    override suspend fun getEpisodes(seriesId: String): List<Episode> {
        // First check if we have episodes cached
        val cachedEpisodes = episodeDao.getEpisodesBySeries(seriesId).first()
        if (cachedEpisodes.isNotEmpty()) {
            return cachedEpisodes.map { it.toDomain() }
        }

        // Fetch from API
        val series = seriesDao.getSeriesById(seriesId) ?: return emptyList()
        val source = sourceDao.getSourceById(series.sourceId)?.toDomain() ?: return emptyList()

        if (source.type != SourceType.XTREAM) return emptyList()

        return withContext(Dispatchers.IO) {
            val episodes = xtreamClient.getEpisodes(source, seriesId)

            // Cache episodes
            val entities = episodes.map { EpisodeEntity.fromDomain(it) }
            episodeDao.insertEpisodes(entities)

            episodes
        }
    }

    override suspend fun refreshSeries(source: Source) {
        if (source.type != SourceType.XTREAM) return

        withContext(Dispatchers.IO) {
            val seriesList = xtreamClient.getSeries(source)

            // Preserve favorites
            val existingSeries = seriesDao.getSeriesBySource(source.id).first()
            val existingFavorites = existingSeries
                .filter { it.isFavorite }
                .map { it.id }
                .toSet()

            // Delete old series and episodes from this source
            episodeDao.deleteEpisodesBySource(source.id)
            seriesDao.deleteSeriesBySource(source.id)

            // Insert new series with preserved favorites
            val entities = seriesList.map { series ->
                SeriesEntity.fromDomain(
                    series.copy(isFavorite = existingFavorites.contains(series.id))
                )
            }
            seriesDao.insertSeries(entities)
        }
    }

    override suspend fun toggleSeriesFavorite(seriesId: String) {
        val series = seriesDao.getSeriesById(seriesId)
        series?.let {
            seriesDao.setFavorite(seriesId, !it.isFavorite)
        }
    }

    // Cleanup
    override suspend fun deleteVodBySource(sourceId: Long) {
        withContext(Dispatchers.IO) {
            movieDao.deleteMoviesBySource(sourceId)
            episodeDao.deleteEpisodesBySource(sourceId)
            seriesDao.deleteSeriesBySource(sourceId)
        }
    }
}
