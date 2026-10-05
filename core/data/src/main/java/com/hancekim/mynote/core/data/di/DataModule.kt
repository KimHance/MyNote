package com.hancekim.mynote.core.data.di

import com.hancekim.mynote.core.data.dto.NotesJson
import com.hancekim.mynote.core.data.repository.BookmarkRepositoryImpl
import com.hancekim.mynote.core.data.repository.InsightRepositoryImpl
import com.hancekim.mynote.core.data.repository.NotesRepositoryImpl
import com.hancekim.mynote.core.data.repository.SettingsRepositoryImpl
import com.hancekim.mynote.core.data.repository.StudyLogRepositoryImpl
import com.hancekim.mynote.core.data.repository.TokenRepositoryImpl
import com.hancekim.mynote.core.domain.repository.BookmarkRepository
import com.hancekim.mynote.core.domain.repository.InsightRepository
import com.hancekim.mynote.core.domain.repository.NotesRepository
import com.hancekim.mynote.core.domain.repository.SettingsRepository
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import com.hancekim.mynote.core.domain.repository.TokenRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindNotesRepository(impl: NotesRepositoryImpl): NotesRepository

    @Binds
    @Singleton
    abstract fun bindTokenRepository(impl: TokenRepositoryImpl): TokenRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository

    @Binds
    @Singleton
    abstract fun bindStudyLogRepository(impl: StudyLogRepositoryImpl): StudyLogRepository

    @Binds
    @Singleton
    abstract fun bindInsightRepository(impl: InsightRepositoryImpl): InsightRepository

    companion object {
        @Provides
        @Singleton
        fun provideJson(): Json = NotesJson
    }
}
