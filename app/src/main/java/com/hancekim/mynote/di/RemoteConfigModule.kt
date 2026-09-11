package com.hancekim.mynote.di

import com.hancekim.mynote.BuildConfig
import com.hancekim.mynote.core.network.config.GithubConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 노트 레포 좌표를 BuildConfig(local.properties)에서 읽어 :core:network 에 제공한다.
 * 토큰은 이제 GithubTokenHolder(런타임 DataStore 소스)가 담당 — TokenSync 가 갱신.
 */
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
