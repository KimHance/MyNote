package com.hancekim.mynote.core.data.di

import com.hancekim.mynote.core.data.repository.DummyNotesRepository
import com.hancekim.mynote.core.data.repository.NotesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindNotesRepository(impl: DummyNotesRepository): NotesRepository
}
