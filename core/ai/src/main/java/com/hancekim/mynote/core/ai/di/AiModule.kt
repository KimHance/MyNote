package com.hancekim.mynote.core.ai.di

import com.hancekim.mynote.core.ai.LiteRtLmQuizGenerator
import com.hancekim.mynote.core.ai.download.ModelDownloads
import com.hancekim.mynote.core.ai.download.work.ModelDownloadIntents
import com.hancekim.mynote.core.domain.repository.QuizGenerator
import com.hancekim.mynote.core.domain.repository.QuizModelSource
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.time.Duration
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ModelDownload

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    @Binds
    @Singleton
    abstract fun bindQuizGenerator(impl: LiteRtLmQuizGenerator): QuizGenerator

    @Binds
    @Singleton
    abstract fun bindQuizModelSource(impl: ModelDownloads): QuizModelSource
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ModelDownloadIntentsModule {
    @BindsOptionalOf
    abstract fun modelDownloadIntents(): ModelDownloadIntents
}

@Module
@InstallIn(SingletonComponent::class)
object ModelDownloadModule {
    @Provides
    @Singleton
    @ModelDownload
    fun provideDownloadClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .callTimeout(Duration.ZERO)
            .connectTimeout(Duration.ofSeconds(20))
            .readTimeout(Duration.ofSeconds(60))
            .retryOnConnectionFailure(true)
            .build()
}
