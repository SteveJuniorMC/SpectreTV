package com.spectretv.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

enum class VideoQuality(val label: String, val maxHeight: Int, val maxBitrate: Int) {
    AUTO("Auto (Best available)", Int.MAX_VALUE, Int.MAX_VALUE),
    HIGH("High (1080p)", 1080, 8_000_000),
    MEDIUM("Medium (720p)", 720, 4_000_000),
    LOW("Low (480p)", 480, 2_000_000),
    VERY_LOW("Very Low (360p)", 360, 1_000_000)
}

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val VIDEO_QUALITY_KEY = intPreferencesKey("video_quality")
    }

    val videoQuality: Flow<VideoQuality> = context.dataStore.data.map { preferences ->
        val ordinal = preferences[VIDEO_QUALITY_KEY] ?: VideoQuality.AUTO.ordinal
        VideoQuality.entries.getOrElse(ordinal) { VideoQuality.AUTO }
    }

    suspend fun setVideoQuality(quality: VideoQuality) {
        context.dataStore.edit { preferences ->
            preferences[VIDEO_QUALITY_KEY] = quality.ordinal
        }
    }
}
