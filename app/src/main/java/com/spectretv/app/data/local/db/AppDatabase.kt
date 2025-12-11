package com.spectretv.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.spectretv.app.data.local.dao.ChannelDao
import com.spectretv.app.data.local.dao.EpisodeDao
import com.spectretv.app.data.local.dao.MovieDao
import com.spectretv.app.data.local.dao.SeriesDao
import com.spectretv.app.data.local.dao.SourceDao
import com.spectretv.app.data.local.entity.ChannelEntity
import com.spectretv.app.data.local.entity.EpisodeEntity
import com.spectretv.app.data.local.entity.MovieEntity
import com.spectretv.app.data.local.entity.SeriesEntity
import com.spectretv.app.data.local.entity.SourceEntity

@Database(
    entities = [
        SourceEntity::class,
        ChannelEntity::class,
        MovieEntity::class,
        SeriesEntity::class,
        EpisodeEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun channelDao(): ChannelDao
    abstract fun movieDao(): MovieDao
    abstract fun seriesDao(): SeriesDao
    abstract fun episodeDao(): EpisodeDao

    companion object {
        const val DATABASE_NAME = "spectretv_database"
    }
}
