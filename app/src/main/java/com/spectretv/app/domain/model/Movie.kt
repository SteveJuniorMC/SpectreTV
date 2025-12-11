package com.spectretv.app.domain.model

data class Movie(
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
)
