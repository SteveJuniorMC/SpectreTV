package com.spectretv.app.domain.model

enum class SourceType {
    M3U,
    XTREAM
}

data class Source(
    val id: Long = 0,
    val name: String,
    val type: SourceType,
    // For M3U sources
    val url: String? = null,
    // For Xtream sources
    val serverUrl: String? = null,
    val username: String? = null,
    val password: String? = null,
    val isActive: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)
