package com.spectretv.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spectretv.app.data.local.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun getEpisodesBySeries(seriesId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId AND seasonNumber = :seasonNumber ORDER BY episodeNumber ASC")
    fun getEpisodesBySeason(seriesId: String, seasonNumber: Int): Flow<List<EpisodeEntity>>

    @Query("SELECT DISTINCT seasonNumber FROM episodes WHERE seriesId = :seriesId ORDER BY seasonNumber ASC")
    fun getSeasons(seriesId: String): Flow<List<Int>>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getEpisodeById(id: String): EpisodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisodes(episodes: List<EpisodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisode(episode: EpisodeEntity)

    @Query("DELETE FROM episodes WHERE seriesId = :seriesId")
    suspend fun deleteEpisodesBySeries(seriesId: String)

    @Query("DELETE FROM episodes WHERE sourceId = :sourceId")
    suspend fun deleteEpisodesBySource(sourceId: Long)

    @Query("DELETE FROM episodes")
    suspend fun deleteAllEpisodes()
}
