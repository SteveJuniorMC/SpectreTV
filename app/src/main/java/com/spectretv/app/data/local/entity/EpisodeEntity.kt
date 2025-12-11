package com.spectretv.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spectretv.app.domain.model.Episode

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = SeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("seriesId"), Index("sourceId"), Index("seasonNumber")]
)
data class EpisodeEntity(
    @PrimaryKey
    val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String,
    val streamUrl: String,
    val posterUrl: String? = null,
    val plot: String? = null,
    val duration: String? = null,
    val sourceId: Long,
    val containerExtension: String? = null
) {
    fun toDomain(): Episode = Episode(
        id = id,
        seriesId = seriesId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        name = name,
        streamUrl = streamUrl,
        posterUrl = posterUrl,
        plot = plot,
        duration = duration,
        sourceId = sourceId,
        containerExtension = containerExtension
    )

    companion object {
        fun fromDomain(episode: Episode): EpisodeEntity = EpisodeEntity(
            id = episode.id,
            seriesId = episode.seriesId,
            seasonNumber = episode.seasonNumber,
            episodeNumber = episode.episodeNumber,
            name = episode.name,
            streamUrl = episode.streamUrl,
            posterUrl = episode.posterUrl,
            plot = episode.plot,
            duration = episode.duration,
            sourceId = episode.sourceId,
            containerExtension = episode.containerExtension
        )
    }
}
