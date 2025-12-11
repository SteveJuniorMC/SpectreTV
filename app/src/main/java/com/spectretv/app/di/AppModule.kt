package com.spectretv.app.di

import com.spectretv.app.data.remote.parser.M3UParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideM3UParser(): M3UParser {
        return M3UParser()
    }
}
