package com.hancekim.mynote.feature.notes.stats

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/** 통계 집계 · 지난 요약을 돌리는 디스패처. 시험은 가상 시간 디스패처를 넣는다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StatsWork

@Module
@InstallIn(SingletonComponent::class)
object StatsDispatcherModule {
    @Provides
    @StatsWork
    fun provideStatsWork(): CoroutineDispatcher = Dispatchers.Default
}
