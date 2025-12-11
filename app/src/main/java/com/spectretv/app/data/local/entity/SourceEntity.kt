package com.spectretv.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spectretv.app.domain.model.Source
import com.spectretv.app.domain.model.SourceType

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // "M3U" or "XTREAM"
    val url: String? = null,
    val serverUrl: String? = null,
    val username: String? = null,
    val password: String? = null,
    val isActive: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toDomain(): Source = Source(
        id = id,
        name = name,
        type = SourceType.valueOf(type),
        url = url,
        serverUrl = serverUrl,
        username = username,
        password = password,
        isActive = isActive,
        lastUpdated = lastUpdated
    )

    companion object {
        fun fromDomain(source: Source): SourceEntity = SourceEntity(
            id = source.id,
            name = source.name,
            type = source.type.name,
            url = source.url,
            serverUrl = source.serverUrl,
            username = source.username,
            password = source.password,
            isActive = source.isActive,
            lastUpdated = source.lastUpdated
        )
    }
}
