package com.hancekim.mynote.di

import android.util.Log
import com.hancekim.mynote.core.common.throttle.Clock
import com.hancekim.mynote.core.common.throttle.SystemClockSource
import com.hancekim.mynote.core.common.throttle.SystemWallClockSource
import com.hancekim.mynote.core.common.throttle.WallClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + APP_SCOPE_HANDLER)

    @Provides
    fun provideClock(): Clock = SystemClockSource

    @Provides
    fun provideWallClock(): WallClock = SystemWallClockSource
}

private val APP_SCOPE_HANDLER =
    CoroutineExceptionHandler { _, cause -> Log.e("MyNote", "앱 스코프 코루틴이 실패했습니다", cause) }
