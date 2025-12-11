package com.spectretv.app.domain.model

data class Episode(
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
)
