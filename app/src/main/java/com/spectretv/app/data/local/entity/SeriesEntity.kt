package com.spectretv.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spectretv.app.domain.model.Series

@Entity(
    tableName = "series",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sourceId"), Index("genre")]
)
data class SeriesEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val plot: String? = null,
    val genre: String = "Uncategorized",
    val year: String? = null,
    val rating: String? = null,
    val sourceId: Long,
    val isFavorite: Boolean = false
) {
    fun toDomain(): Series = Series(
        id = id,
        name = name,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        plot = plot,
        genre = genre,
        year = year,
        rating = rating,
        sourceId = sourceId,
        isFavorite = isFavorite
    )

    companion object {
        fun fromDomain(series: Series): SeriesEntity = SeriesEntity(
            id = series.id,
            name = series.name,
            posterUrl = series.posterUrl,
            backdropUrl = series.backdropUrl,
            plot = series.plot,
            genre = series.genre,
            year = series.year,
            rating = series.rating,
            sourceId = series.sourceId,
            isFavorite = series.isFavorite
        )
    }
}
