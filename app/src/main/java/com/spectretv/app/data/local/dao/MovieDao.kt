package com.spectretv.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spectretv.app.data.local.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies ORDER BY name ASC")
    fun getAllMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE sourceId = :sourceId ORDER BY name ASC")
    fun getMoviesBySource(sourceId: Long): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE genre = :genre ORDER BY name ASC")
    fun getMoviesByGenre(genre: String): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteMovies(): Flow<List<MovieEntity>>

    @Query("SELECT DISTINCT genre FROM movies ORDER BY genre ASC")
    fun getAllGenres(): Flow<List<String>>

    @Query("SELECT * FROM movies WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchMovies(query: String): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getMovieById(id: String): MovieEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity)

    @Query("UPDATE movies SET isFavorite = :isFavorite WHERE id = :movieId")
    suspend fun setFavorite(movieId: String, isFavorite: Boolean)

    @Query("DELETE FROM movies WHERE sourceId = :sourceId")
    suspend fun deleteMoviesBySource(sourceId: Long)

    @Query("DELETE FROM movies")
    suspend fun deleteAllMovies()
}
