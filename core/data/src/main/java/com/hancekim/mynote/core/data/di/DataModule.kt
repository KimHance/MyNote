package com.hancekim.mynote.core.data.di

import com.hancekim.mynote.core.data.datasource.ConnectionIdGenerator
import com.hancekim.mynote.core.data.datasource.ContentNoteFileReader
import com.hancekim.mynote.core.data.dto.NotesJson
import com.hancekim.mynote.core.data.repository.BookmarkRepositoryImpl
import com.hancekim.mynote.core.data.repository.ConnectionRepositoryImpl
import com.hancekim.mynote.core.data.repository.DeviceNotesRepositoryImpl
import com.hancekim.mynote.core.data.repository.InsightRepositoryImpl
import com.hancekim.mynote.core.data.repository.NotesRepositoryImpl
import com.hancekim.mynote.core.data.repository.SettingsRepositoryImpl
import com.hancekim.mynote.core.data.repository.StudyLogRepositoryImpl
import com.hancekim.mynote.core.data.repository.TokenRepositoryImpl
import com.hancekim.mynote.core.domain.repository.BookmarkRepository
import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import com.hancekim.mynote.core.domain.repository.DeviceNoteText
import com.hancekim.mynote.core.domain.repository.DeviceNotesRepository
import com.hancekim.mynote.core.domain.repository.InsightRepository
import com.hancekim.mynote.core.domain.repository.NoteFileReader
import com.hancekim.mynote.core.domain.repository.NotesRepository
import com.hancekim.mynote.core.domain.repository.SettingsRepository
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import com.hancekim.mynote.core.domain.repository.TokenRepository
import com.hancekim.mynote.core.format.DeviceNoteMarkdown
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindNotesRepository(impl: NotesRepositoryImpl): NotesRepository

    @Binds
    @Singleton
    abstract fun bindConnectionRepository(impl: ConnectionRepositoryImpl): ConnectionRepository

    @Binds
    @Singleton
    abstract fun bindDeviceNotesRepository(impl: DeviceNotesRepositoryImpl): DeviceNotesRepository

    @Binds
    abstract fun bindNoteFileReader(impl: ContentNoteFileReader): NoteFileReader

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

        @Provides
        fun provideDeviceNoteText(): DeviceNoteText = DeviceNoteMarkdown()

        @Provides
        @Singleton
        fun provideConnectionIdGenerator(): ConnectionIdGenerator =
            ConnectionIdGenerator {
                UUID.randomUUID().toString()
            }
    }
}
