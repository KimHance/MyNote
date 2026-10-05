package com.hancekim.mynote.di

import com.hancekim.mynote.BuildConfig
import com.hancekim.mynote.core.domain.model.ConnectionSeed
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemoteConfigModule {
    @Provides
    @Singleton
    fun provideConnectionSeed(): ConnectionSeed? =
        if (BuildConfig.GITHUB_OWNER.isBlank() || BuildConfig.GITHUB_REPO.isBlank()) {
            null
        } else {
            ConnectionSeed(
                owner = BuildConfig.GITHUB_OWNER,
                repo = BuildConfig.GITHUB_REPO,
                branch = BuildConfig.GITHUB_BRANCH,
                token = BuildConfig.GITHUB_TOKEN.ifBlank { null },
            )
        }
}
