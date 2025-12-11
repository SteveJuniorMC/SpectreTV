package com.spectretv.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey
    val contentId: String,
    val contentType: String, // "channel", "movie", "episode"
    val name: String,
    val posterUrl: String? = null,
    val streamUrl: String,
    val lastWatchedAt: Long = System.currentTimeMillis(),
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val seriesId: String? = null,  // For episodes - to link back to series
    val seriesName: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null
) {
    val progressPercent: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()) else 0f

    val isCompleted: Boolean
        get() = durationMs > 0 && progressPercent >= 0.9f
}
