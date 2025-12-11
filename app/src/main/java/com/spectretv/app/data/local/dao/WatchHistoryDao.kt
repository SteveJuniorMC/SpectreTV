package com.spectretv.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spectretv.app.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedAt DESC")
    fun getAllHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE contentType = :contentType ORDER BY lastWatchedAt DESC")
    fun getHistoryByType(contentType: String): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE contentType = 'channel' ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun getRecentChannels(limit: Int = 10): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE contentType = 'movie' ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun getRecentMovies(limit: Int = 10): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE contentType = 'episode' ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun getRecentEpisodes(limit: Int = 10): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE contentType IN ('movie', 'episode') AND positionMs > 0 AND (durationMs = 0 OR (positionMs * 1.0 / durationMs) < 0.9) ORDER BY lastWatchedAt DESC LIMIT :limit")
    fun getContinueWatching(limit: Int = 10): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE contentId = :contentId")
    suspend fun getByContentId(contentId: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: WatchHistoryEntity)

    @Query("UPDATE watch_history SET positionMs = :positionMs, durationMs = :durationMs, lastWatchedAt = :timestamp WHERE contentId = :contentId")
    suspend fun updateProgress(contentId: String, positionMs: Long, durationMs: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM watch_history WHERE contentId = :contentId")
    suspend fun delete(contentId: String)

    @Query("DELETE FROM watch_history WHERE contentType = :contentType")
    suspend fun deleteByType(contentType: String)

    @Query("DELETE FROM watch_history")
    suspend fun deleteAll()
}
