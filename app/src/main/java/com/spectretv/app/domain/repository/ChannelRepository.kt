package com.spectretv.app.domain.repository

import com.spectretv.app.domain.model.Channel
import com.spectretv.app.domain.model.Source
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun getAllChannels(): Flow<List<Channel>>
    fun getChannelsBySource(sourceId: Long): Flow<List<Channel>>
    fun getChannelsByGroup(group: String): Flow<List<Channel>>
    fun getFavoriteChannels(): Flow<List<Channel>>
    fun getAllGroups(): Flow<List<String>>
    fun searchChannels(query: String): Flow<List<Channel>>
    suspend fun getChannelById(id: String): Channel?
    suspend fun refreshChannels(source: Source)
    suspend fun toggleFavorite(channelId: String)
    suspend fun deleteChannelsBySource(sourceId: Long)
}
