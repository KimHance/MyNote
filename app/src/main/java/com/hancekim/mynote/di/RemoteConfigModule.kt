package com.hancekim.mynote.di

import com.hancekim.mynote.BuildConfig
import com.hancekim.mynote.core.network.config.GithubConfig
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
    fun provideGithubConfig(): GithubConfig =
        GithubConfig(
            owner = BuildConfig.GITHUB_OWNER,
            repo = BuildConfig.GITHUB_REPO,
            branch = BuildConfig.GITHUB_BRANCH,
        )
}
