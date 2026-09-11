package com.hancekim.mynote.core.network.di

import com.hancekim.mynote.core.data.datasource.RemoteNotesDataSource
import com.hancekim.mynote.core.network.api.GithubContentsApi
import com.hancekim.mynote.core.network.config.GithubTokenHolder
import com.hancekim.mynote.core.network.config.GithubTokenProvider
import com.hancekim.mynote.core.network.datasource.RemoteNotesDataSourceImpl
import com.hancekim.mynote.core.network.interceptor.AuthInterceptor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
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
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()

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

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteDataSourceModule {
    @Binds
    @Singleton
    abstract fun bindRemoteNotesDataSource(impl: RemoteNotesDataSourceImpl): RemoteNotesDataSource

    @Binds
    @Singleton
    abstract fun bindGithubTokenProvider(impl: GithubTokenHolder): GithubTokenProvider

    @Binds
    @Singleton
    abstract fun bindAppliedTokenSource(
        impl: GithubTokenHolder,
    ): com.hancekim.mynote.core.domain.repository.AppliedTokenSource
}
