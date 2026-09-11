package com.hancekim.mynote.core.database.di

import android.content.Context
import androidx.room3.Room
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.data.datasource.TokenStore
import com.hancekim.mynote.core.database.NotesDatabase
import com.hancekim.mynote.core.database.dao.NoteDao
import com.hancekim.mynote.core.database.datasource.DataStoreTokenStore
import com.hancekim.mynote.core.database.datasource.LocalNotesDataSourceImpl
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
        Room.databaseBuilder(context, NotesDatabase::class.java, "notes.db").build()

    @Provides
    @Singleton
    internal fun provideNoteDao(database: NotesDatabase): NoteDao = database.noteDao()
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
}
