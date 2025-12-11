package com.spectretv.app.data.repository

import com.spectretv.app.data.local.dao.ChannelDao
import com.spectretv.app.data.local.entity.ChannelEntity
import com.spectretv.app.data.remote.XtreamClient
import com.spectretv.app.data.remote.parser.M3UParser
import com.spectretv.app.domain.model.Channel
import com.spectretv.app.domain.model.Source
import com.spectretv.app.domain.model.SourceType
import com.spectretv.app.domain.repository.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val channelDao: ChannelDao,
    private val xtreamClient: XtreamClient,
    private val m3uParser: M3UParser,
    private val okHttpClient: OkHttpClient
) : ChannelRepository {

    override fun getAllChannels(): Flow<List<Channel>> {
        return channelDao.getAllChannels().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getChannelsBySource(sourceId: Long): Flow<List<Channel>> {
        return channelDao.getChannelsBySource(sourceId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getChannelsByGroup(group: String): Flow<List<Channel>> {
        return channelDao.getChannelsByGroup(group).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteChannels(): Flow<List<Channel>> {
        return channelDao.getFavoriteChannels().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllGroups(): Flow<List<String>> {
        return channelDao.getAllGroups()
    }

    override fun searchChannels(query: String): Flow<List<Channel>> {
        return channelDao.searchChannels(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getChannelById(id: String): Channel? {
        return channelDao.getChannelById(id)?.toDomain()
    }

    override suspend fun refreshChannels(source: Source) {
        withContext(Dispatchers.IO) {
            val channels = when (source.type) {
                SourceType.M3U -> fetchM3UChannels(source)
                SourceType.XTREAM -> xtreamClient.getChannels(source)
            }

            // Preserve favorites
            val existingChannels = channelDao.getChannelsBySource(source.id).first()
            val existingFavorites = existingChannels
                .filter { it.isFavorite }
                .map { it.id }
                .toSet()

            // Delete old channels from this source
            channelDao.deleteChannelsBySource(source.id)

            // Insert new channels with preserved favorites
            val entities = channels.map { channel ->
                ChannelEntity.fromDomain(
                    channel.copy(isFavorite = existingFavorites.contains(channel.id))
                )
            }
            channelDao.insertChannels(entities)
        }
    }

    private suspend fun fetchM3UChannels(source: Source): List<Channel> {
        val request = Request.Builder()
            .url(source.url!!)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val content = response.body?.string() ?: return emptyList()

        return m3uParser.parse(content, source.id)
    }

    override suspend fun toggleFavorite(channelId: String) {
        val channel = channelDao.getChannelById(channelId)
        channel?.let {
            channelDao.setFavorite(channelId, !it.isFavorite)
        }
    }

    override suspend fun deleteChannelsBySource(sourceId: Long) {
        channelDao.deleteChannelsBySource(sourceId)
    }
}
