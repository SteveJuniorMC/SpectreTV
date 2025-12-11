package com.spectretv.app.di

import com.spectretv.app.data.repository.ChannelRepositoryImpl
import com.spectretv.app.data.repository.SourceRepositoryImpl
import com.spectretv.app.data.repository.VodRepositoryImpl
import com.spectretv.app.data.repository.WatchHistoryRepositoryImpl
import com.spectretv.app.domain.repository.ChannelRepository
import com.spectretv.app.domain.repository.SourceRepository
import com.spectretv.app.domain.repository.VodRepository
import com.spectretv.app.domain.repository.WatchHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSourceRepository(
        sourceRepositoryImpl: SourceRepositoryImpl
    ): SourceRepository

    @Binds
    @Singleton
    abstract fun bindChannelRepository(
        channelRepositoryImpl: ChannelRepositoryImpl
    ): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindVodRepository(
        vodRepositoryImpl: VodRepositoryImpl
    ): VodRepository

    @Binds
    @Singleton
    abstract fun bindWatchHistoryRepository(
        watchHistoryRepositoryImpl: WatchHistoryRepositoryImpl
    ): WatchHistoryRepository
}
