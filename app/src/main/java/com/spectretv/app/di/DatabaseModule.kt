package com.spectretv.app.di

import android.content.Context
import androidx.room.Room
import com.spectretv.app.data.local.dao.ChannelDao
import com.spectretv.app.data.local.dao.EpisodeDao
import com.spectretv.app.data.local.dao.MovieDao
import com.spectretv.app.data.local.dao.SeriesDao
import com.spectretv.app.data.local.dao.SourceDao
import com.spectretv.app.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSourceDao(database: AppDatabase): SourceDao {
        return database.sourceDao()
    }

    @Provides
    @Singleton
    fun provideChannelDao(database: AppDatabase): ChannelDao {
        return database.channelDao()
    }

    @Provides
    @Singleton
    fun provideMovieDao(database: AppDatabase): MovieDao {
        return database.movieDao()
    }

    @Provides
    @Singleton
    fun provideSeriesDao(database: AppDatabase): SeriesDao {
        return database.seriesDao()
    }

    @Provides
    @Singleton
    fun provideEpisodeDao(database: AppDatabase): EpisodeDao {
        return database.episodeDao()
    }
}
