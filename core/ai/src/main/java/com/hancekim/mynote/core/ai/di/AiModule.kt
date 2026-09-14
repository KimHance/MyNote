package com.hancekim.mynote.core.ai.di

import com.hancekim.mynote.core.ai.LiteRtLmQuizGenerator
import com.hancekim.mynote.core.domain.repository.QuizGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    @Binds
    @Singleton
    abstract fun bindQuizGenerator(impl: LiteRtLmQuizGenerator): QuizGenerator
}
