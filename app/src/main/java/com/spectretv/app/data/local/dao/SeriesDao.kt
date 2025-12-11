package com.spectretv.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spectretv.app.data.local.entity.SeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series ORDER BY name ASC")
    fun getAllSeries(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE sourceId = :sourceId ORDER BY name ASC")
    fun getSeriesBySource(sourceId: Long): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE genre = :genre ORDER BY name ASC")
    fun getSeriesByGenre(genre: String): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteSeries(): Flow<List<SeriesEntity>>

    @Query("SELECT DISTINCT genre FROM series ORDER BY genre ASC")
    fun getAllGenres(): Flow<List<String>>

    @Query("SELECT * FROM series WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchSeries(query: String): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun getSeriesById(id: String): SeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: List<SeriesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOneSeries(series: SeriesEntity)

    @Query("UPDATE series SET isFavorite = :isFavorite WHERE id = :seriesId")
    suspend fun setFavorite(seriesId: String, isFavorite: Boolean)

    @Query("DELETE FROM series WHERE sourceId = :sourceId")
    suspend fun deleteSeriesBySource(sourceId: Long)

    @Query("DELETE FROM series")
    suspend fun deleteAllSeries()
}
