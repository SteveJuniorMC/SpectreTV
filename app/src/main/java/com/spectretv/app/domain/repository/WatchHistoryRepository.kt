package com.spectretv.app.domain.repository

import com.spectretv.app.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

interface WatchHistoryRepository {
    fun getAllHistory(): Flow<List<WatchHistoryEntity>>
    fun getRecentChannels(limit: Int = 10): Flow<List<WatchHistoryEntity>>
    fun getRecentMovies(limit: Int = 10): Flow<List<WatchHistoryEntity>>
    fun getRecentEpisodes(limit: Int = 10): Flow<List<WatchHistoryEntity>>
    fun getContinueWatching(limit: Int = 10): Flow<List<WatchHistoryEntity>>
    suspend fun getByContentId(contentId: String): WatchHistoryEntity?
    suspend fun addToHistory(entry: WatchHistoryEntity)
    suspend fun updateProgress(contentId: String, positionMs: Long, durationMs: Long)
    suspend fun removeFromHistory(contentId: String)
    suspend fun clearHistory()
    suspend fun clearHistoryByType(contentType: String)
}
