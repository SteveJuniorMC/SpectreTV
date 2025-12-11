package com.spectretv.app.domain.repository

import com.spectretv.app.domain.model.Source
import kotlinx.coroutines.flow.Flow

interface SourceRepository {
    fun getAllSources(): Flow<List<Source>>
    fun getActiveSources(): Flow<List<Source>>
    suspend fun getSourceById(id: Long): Source?
    suspend fun addSource(source: Source): Long
    suspend fun updateSource(source: Source)
    suspend fun deleteSource(source: Source)
    suspend fun deleteSourceById(id: Long)
}
