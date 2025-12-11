package com.spectretv.app.domain.model

data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val group: String = "Uncategorized",
    val epgId: String? = null,
    val sourceId: Long,
    val isFavorite: Boolean = false
)
