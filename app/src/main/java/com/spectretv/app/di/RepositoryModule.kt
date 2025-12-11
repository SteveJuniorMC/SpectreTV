package com.spectretv.app.di

import com.spectretv.app.data.repository.ChannelRepositoryImpl
import com.spectretv.app.data.repository.SourceRepositoryImpl
import com.spectretv.app.domain.repository.ChannelRepository
import com.spectretv.app.domain.repository.SourceRepository
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
}
