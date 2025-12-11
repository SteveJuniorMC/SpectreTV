package com.spectretv.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.spectretv.app.data.local.dao.ChannelDao
import com.spectretv.app.data.local.dao.SourceDao
import com.spectretv.app.data.local.entity.ChannelEntity
import com.spectretv.app.data.local.entity.SourceEntity

@Database(
    entities = [SourceEntity::class, ChannelEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun channelDao(): ChannelDao

    companion object {
        const val DATABASE_NAME = "spectretv_database"
    }
}
