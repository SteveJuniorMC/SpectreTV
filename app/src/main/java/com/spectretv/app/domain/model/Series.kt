package com.spectretv.app.domain.model

data class Series(
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
)
