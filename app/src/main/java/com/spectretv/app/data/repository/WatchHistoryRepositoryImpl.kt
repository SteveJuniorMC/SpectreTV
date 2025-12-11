package com.spectretv.app.data.repository

import com.spectretv.app.data.local.dao.WatchHistoryDao
import com.spectretv.app.data.local.entity.WatchHistoryEntity
import com.spectretv.app.domain.repository.WatchHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchHistoryRepositoryImpl @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao
) : WatchHistoryRepository {

    override fun getAllHistory(): Flow<List<WatchHistoryEntity>> {
        return watchHistoryDao.getAllHistory()
    }

    override fun getRecentChannels(limit: Int): Flow<List<WatchHistoryEntity>> {
        return watchHistoryDao.getRecentChannels(limit)
    }

    override fun getRecentMovies(limit: Int): Flow<List<WatchHistoryEntity>> {
        return watchHistoryDao.getRecentMovies(limit)
    }

    override fun getRecentEpisodes(limit: Int): Flow<List<WatchHistoryEntity>> {
        return watchHistoryDao.getRecentEpisodes(limit)
    }

    override fun getContinueWatching(limit: Int): Flow<List<WatchHistoryEntity>> {
        return watchHistoryDao.getContinueWatching(limit)
    }

    override suspend fun getByContentId(contentId: String): WatchHistoryEntity? {
        return watchHistoryDao.getByContentId(contentId)
    }

    override suspend fun addToHistory(entry: WatchHistoryEntity) {
        watchHistoryDao.insertOrUpdate(entry)
    }

    override suspend fun updateProgress(contentId: String, positionMs: Long, durationMs: Long) {
        watchHistoryDao.updateProgress(contentId, positionMs, durationMs)
    }

    override suspend fun removeFromHistory(contentId: String) {
        watchHistoryDao.delete(contentId)
    }

    override suspend fun clearHistory() {
        watchHistoryDao.deleteAll()
    }

    override suspend fun clearHistoryByType(contentType: String) {
        watchHistoryDao.deleteByType(contentType)
    }
}
