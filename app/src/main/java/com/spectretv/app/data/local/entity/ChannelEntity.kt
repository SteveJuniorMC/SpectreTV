package com.spectretv.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spectretv.app.domain.model.Channel

@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sourceId"), Index("group")]
)
data class ChannelEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val group: String = "Uncategorized",
    val epgId: String? = null,
    val sourceId: Long,
    val isFavorite: Boolean = false
) {
    fun toDomain(): Channel = Channel(
        id = id,
        name = name,
        streamUrl = streamUrl,
        logoUrl = logoUrl,
        group = group,
        epgId = epgId,
        sourceId = sourceId,
        isFavorite = isFavorite
    )

    companion object {
        fun fromDomain(channel: Channel): ChannelEntity = ChannelEntity(
            id = channel.id,
            name = channel.name,
            streamUrl = channel.streamUrl,
            logoUrl = channel.logoUrl,
            group = channel.group,
            epgId = channel.epgId,
            sourceId = channel.sourceId,
            isFavorite = channel.isFavorite
        )
    }
}
