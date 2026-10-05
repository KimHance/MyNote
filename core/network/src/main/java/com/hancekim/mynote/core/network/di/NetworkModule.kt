package com.hancekim.mynote.core.network.di

import com.hancekim.mynote.core.data.datasource.NoteSourceAdapter
import com.hancekim.mynote.core.domain.connect.ConnectProbe
import com.hancekim.mynote.core.domain.repository.AppliedTokenSource
import com.hancekim.mynote.core.domain.repository.NetworkStatusSource
import com.hancekim.mynote.core.network.api.ConnectProbeApi
import com.hancekim.mynote.core.network.api.GithubContentsApi
import com.hancekim.mynote.core.network.config.GithubTokenHolder
import com.hancekim.mynote.core.network.config.GithubTokenProvider
import com.hancekim.mynote.core.network.datasource.GithubConnectProbe
import com.hancekim.mynote.core.network.datasource.GithubManifestAdapter
import com.hancekim.mynote.core.network.interceptor.AuthInterceptor
import com.hancekim.mynote.core.network.status.NetworkStatusSourceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(authInterceptor)
            .withTimeouts()
            .build()

    @Provides
    @Singleton
    @ProbeClient
    fun provideProbeOkHttpClient(): OkHttpClient = OkHttpClient.Builder().withTimeouts().build()

    @Provides
    @Singleton
    @ProbeClient
    internal fun provideConnectProbeApi(
        @ProbeClient client: OkHttpClient,
        json: Json,
    ): ConnectProbeApi = buildConnectProbeApi(client, json, GithubContentsApi.BASE_URL.toHttpUrl())

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(GithubContentsApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideGithubContentsApi(retrofit: Retrofit): GithubContentsApi =
        retrofit.create(GithubContentsApi::class.java)
}

private fun OkHttpClient.Builder.withTimeouts(): OkHttpClient.Builder =
    connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)

internal fun buildConnectProbeApi(
    client: OkHttpClient,
    json: Json,
    baseUrl: HttpUrl,
): ConnectProbeApi =
    Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ConnectProbeApi::class.java)

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteDataSourceModule {
    @Binds
    @Singleton
    internal abstract fun bindConnectProbe(impl: GithubConnectProbe): ConnectProbe

    @Binds
    @IntoSet
    abstract fun bindManifestAdapter(impl: GithubManifestAdapter): NoteSourceAdapter

    @Binds
    @Singleton
    abstract fun bindGithubTokenProvider(impl: GithubTokenHolder): GithubTokenProvider

    @Binds
    @Singleton
    abstract fun bindNetworkStatusSource(impl: NetworkStatusSourceImpl): NetworkStatusSource

    @Binds
    @Singleton
    abstract fun bindAppliedTokenSource(impl: GithubTokenHolder): AppliedTokenSource
}
