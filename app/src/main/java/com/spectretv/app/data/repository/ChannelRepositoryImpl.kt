package com.spectretv.app.data.repository

import com.spectretv.app.data.local.dao.ChannelDao
import com.spectretv.app.data.local.entity.ChannelEntity
import com.spectretv.app.data.remote.XtreamClient
import com.spectretv.app.data.remote.parser.M3UParser
import com.spectretv.app.domain.model.Channel
import com.spectretv.app.domain.model.Source
import com.spectretv.app.domain.model.SourceType
import com.spectretv.app.domain.repository.ChannelRepository
import com.spectretv.app.domain.repository.LoadingProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    override suspend fun validateM3USource(source: Source): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(source.url!!)
                    .head()
                    .build()
                val response = okHttpClient.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun refreshChannelsWithProgress(
        source: Source,
        onProgress: (LoadingProgress) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            when (source.type) {
                SourceType.M3U -> {
                    onProgress(LoadingProgress(currentCategory = "Downloading playlist...", categoriesLoaded = 0, totalCategories = 1, itemsLoaded = 0))
                    val channels = fetchM3UChannels(source)
                    onProgress(LoadingProgress(currentCategory = "Saving channels...", categoriesLoaded = 1, totalCategories = 1, itemsLoaded = channels.size))
                    saveChannels(source, channels)
                }
                SourceType.XTREAM -> {
                    // Get categories first
                    val categories = xtreamClient.getCategories(source)
                    val totalCategories = categories.size
                    var itemsLoaded = 0

                    // Preserve favorites
                    val existingChannels = channelDao.getChannelsBySource(source.id).first()
                    val existingFavorites = existingChannels
                        .filter { it.isFavorite }
                        .map { it.id }
                        .toSet()

                    // Collect all channels in memory first (don't insert until done)
                    val allEntities = mutableListOf<ChannelEntity>()

                    // Divide into 4 batches for parallel fetching
                    val batchSize = maxOf(1, (categories.size + 3) / 4)
                    val batches = categories.chunked(batchSize)

                    batches.forEachIndexed { batchIndex, batch ->
                        onProgress(LoadingProgress(
                            currentCategory = "Batch ${batchIndex + 1} of ${batches.size}",
                            categoriesLoaded = batchIndex * batchSize,
                            totalCategories = totalCategories,
                            itemsLoaded = itemsLoaded
                        ))

                        // Fetch all categories in this batch in parallel
                        val batchResults = batch.map { category ->
                            async {
                                val categoryId = category.categoryId ?: return@async emptyList()
                                val categoryName = category.categoryName ?: "Uncategorized"
                                xtreamClient.getChannelsByCategory(source, categoryId, categoryName)
                            }
                        }.awaitAll()

                        // Process results
                        batchResults.flatten().forEach { channel ->
                            allEntities.add(
                                ChannelEntity.fromDomain(
                                    channel.copy(isFavorite = existingFavorites.contains(channel.id))
                                )
                            )
                            itemsLoaded++
                        }
                    }

                    // Now save all at once
                    onProgress(LoadingProgress(
                        currentCategory = "Saving...",
                        categoriesLoaded = totalCategories,
                        totalCategories = totalCategories,
                        itemsLoaded = itemsLoaded
                    ))
                    channelDao.deleteChannelsBySource(source.id)
                    channelDao.insertChannels(allEntities)

                    onProgress(LoadingProgress(
                        currentCategory = "Done",
                        categoriesLoaded = totalCategories,
                        totalCategories = totalCategories,
                        itemsLoaded = itemsLoaded
                    ))
                }
            }
        }
    }

    private suspend fun saveChannels(source: Source, channels: List<Channel>) {
        val existingChannels = channelDao.getChannelsBySource(source.id).first()
        val existingFavorites = existingChannels
            .filter { it.isFavorite }
            .map { it.id }
            .toSet()

        channelDao.deleteChannelsBySource(source.id)

        val entities = channels.map { channel ->
            ChannelEntity.fromDomain(
                channel.copy(isFavorite = existingFavorites.contains(channel.id))
            )
        }
        channelDao.insertChannels(entities)
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
