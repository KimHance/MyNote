package com.hancekim.mynote.di

import com.hancekim.mynote.BuildConfig
import com.hancekim.mynote.core.network.config.GithubConfig
import com.hancekim.mynote.core.network.config.GithubTokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 노트 레포 좌표/토큰을 BuildConfig(local.properties)에서 읽어 :core:network 에 제공한다.
 * 토큰 소스는 임시(APK 내장) — 다음 작업에서 런타임 입력 + 암호화 저장 [GithubTokenProvider] 구현으로 교체.
 */
@Module
@InstallIn(SingletonComponent::class)
object RemoteConfigModule {

    @Provides
    @Singleton
    fun provideGithubConfig(): GithubConfig = GithubConfig(
        owner = BuildConfig.GITHUB_OWNER,
        repo = BuildConfig.GITHUB_REPO,
        branch = BuildConfig.GITHUB_BRANCH,
    )

    @Provides
    @Singleton
    fun provideGithubTokenProvider(): GithubTokenProvider =
        GithubTokenProvider { BuildConfig.GITHUB_TOKEN.ifBlank { null } }
}
