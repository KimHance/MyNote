package com.hancekim.mynote.di

import com.hancekim.mynote.core.ai.download.work.ModelDownloadIntents
import com.hancekim.mynote.download.AppModelDownloadIntents
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DownloadIntentsModule {
    @Binds
    @Singleton
    abstract fun bindModelDownloadIntents(impl: AppModelDownloadIntents): ModelDownloadIntents
}
