package com.spectretv.app.data.repository

import com.spectretv.app.data.local.dao.SourceDao
import com.spectretv.app.data.local.entity.SourceEntity
import com.spectretv.app.domain.model.Source
import com.spectretv.app.domain.repository.SourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceRepositoryImpl @Inject constructor(
    private val sourceDao: SourceDao
) : SourceRepository {

    override fun getAllSources(): Flow<List<Source>> {
        return sourceDao.getAllSources().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveSources(): Flow<List<Source>> {
        return sourceDao.getActiveSources().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSourceById(id: Long): Source? {
        return sourceDao.getSourceById(id)?.toDomain()
    }

    override suspend fun addSource(source: Source): Long {
        return sourceDao.insertSource(SourceEntity.fromDomain(source))
    }

    override suspend fun updateSource(source: Source) {
        sourceDao.updateSource(SourceEntity.fromDomain(source))
    }

    override suspend fun deleteSource(source: Source) {
        sourceDao.deleteSource(SourceEntity.fromDomain(source))
    }

    override suspend fun deleteSourceById(id: Long) {
        sourceDao.deleteSourceById(id)
    }
}
