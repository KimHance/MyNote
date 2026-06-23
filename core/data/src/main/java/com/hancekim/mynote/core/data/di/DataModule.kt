package com.hancekim.mynote.core.data.di

import com.hancekim.mynote.core.data.datasource.LocalAssetNotesDataSource
import com.hancekim.mynote.core.data.datasource.NotesDataSource
import com.hancekim.mynote.core.data.repository.NotesRepositoryImpl
import com.hancekim.mynote.core.domain.repository.NotesRepository
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
    abstract fun bindNotesRepository(impl: NotesRepositoryImpl): NotesRepository

    // 현재는 로컬 더미. 원격(GitHub raw) 전환 시 이 바인딩을 :core:network 의 RemoteNotesDataSource 로 교체.
    @Binds
    @Singleton
    abstract fun bindNotesDataSource(impl: LocalAssetNotesDataSource): NotesDataSource
}
