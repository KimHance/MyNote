package com.hancekim.mynote.core.database.di

import android.content.Context
import androidx.room3.Room
import com.hancekim.mynote.core.data.datasource.BookmarkStore
import com.hancekim.mynote.core.data.datasource.InsightStatusStore
import com.hancekim.mynote.core.data.datasource.InsightStore
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.data.datasource.SettingsStore
import com.hancekim.mynote.core.data.datasource.StudyLogStore
import com.hancekim.mynote.core.data.datasource.TokenStore
import com.hancekim.mynote.core.database.NotesDatabase
import com.hancekim.mynote.core.database.dao.InsightDao
import com.hancekim.mynote.core.database.dao.NoteDao
import com.hancekim.mynote.core.database.dao.StudyLogDao
import com.hancekim.mynote.core.database.datasource.DataStoreBookmarkStore
import com.hancekim.mynote.core.database.datasource.DataStoreInsightStatusStore
import com.hancekim.mynote.core.database.datasource.DataStoreSettingsStore
import com.hancekim.mynote.core.database.datasource.DataStoreTokenStore
import com.hancekim.mynote.core.database.datasource.InsightStoreImpl
import com.hancekim.mynote.core.database.datasource.LocalNotesDataSourceImpl
import com.hancekim.mynote.core.database.datasource.StudyLogStoreImpl
import com.hancekim.mynote.core.database.migration.MIGRATION_4_5
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    internal fun provideNotesDatabase(
        @ApplicationContext context: Context,
    ): NotesDatabase =
        Room
            .databaseBuilder(context, NotesDatabase::class.java, "notes.db")
            .addMigrations(MIGRATION_4_5)
            .build()

    @Provides
    @Singleton
    internal fun provideNoteDao(database: NotesDatabase): NoteDao = database.noteDao()

    @Provides
    @Singleton
    internal fun provideStudyLogDao(database: NotesDatabase): StudyLogDao = database.studyLogDao()

    @Provides
    @Singleton
    internal fun provideInsightDao(database: NotesDatabase): InsightDao = database.insightDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseBindsModule {
    @Binds
    @Singleton
    abstract fun bindLocalNotesDataSource(impl: LocalNotesDataSourceImpl): LocalNotesDataSource

    @Binds
    @Singleton
    abstract fun bindTokenStore(impl: DataStoreTokenStore): TokenStore

    @Binds
    @Singleton
    abstract fun bindSettingsStore(impl: DataStoreSettingsStore): SettingsStore

    @Binds
    @Singleton
    abstract fun bindBookmarkStore(impl: DataStoreBookmarkStore): BookmarkStore

    @Binds
    @Singleton
    abstract fun bindStudyLogStore(impl: StudyLogStoreImpl): StudyLogStore

    @Binds
    @Singleton
    abstract fun bindInsightStore(impl: InsightStoreImpl): InsightStore

    @Binds
    @Singleton
    abstract fun bindInsightStatusStore(impl: DataStoreInsightStatusStore): InsightStatusStore
}
