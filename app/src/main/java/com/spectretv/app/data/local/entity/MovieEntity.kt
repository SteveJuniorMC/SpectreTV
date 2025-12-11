package com.spectretv.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spectretv.app.domain.model.Movie

@Entity(
    tableName = "movies",
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
data class MovieEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val streamUrl: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val plot: String? = null,
    val genre: String = "Uncategorized",
    val year: String? = null,
    val duration: String? = null,
    val rating: String? = null,
    val sourceId: Long,
    val isFavorite: Boolean = false,
    val containerExtension: String? = null
) {
    fun toDomain(): Movie = Movie(
        id = id,
        name = name,
        streamUrl = streamUrl,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        plot = plot,
        genre = genre,
        year = year,
        duration = duration,
        rating = rating,
        sourceId = sourceId,
        isFavorite = isFavorite,
        containerExtension = containerExtension
    )

    companion object {
        fun fromDomain(movie: Movie): MovieEntity = MovieEntity(
            id = movie.id,
            name = movie.name,
            streamUrl = movie.streamUrl,
            posterUrl = movie.posterUrl,
            backdropUrl = movie.backdropUrl,
            plot = movie.plot,
            genre = movie.genre,
            year = movie.year,
            duration = movie.duration,
            rating = movie.rating,
            sourceId = movie.sourceId,
            isFavorite = movie.isFavorite,
            containerExtension = movie.containerExtension
        )
    }
}
