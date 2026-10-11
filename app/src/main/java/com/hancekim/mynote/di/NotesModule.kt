package com.hancekim.mynote.di

import com.hancekim.mynote.core.ui.AndroidFailureLog
import com.hancekim.mynote.core.ui.AndroidNotesMessages
import com.hancekim.mynote.core.ui.FailureLog
import com.hancekim.mynote.core.ui.NotesMessages
import com.hancekim.mynote.feature.quiz.AccessibilityAutoAdvancePolicy
import com.hancekim.mynote.feature.quiz.QuizAutoAdvancePolicy
import com.hancekim.mynote.feature.stats.LogStatsTrace
import com.hancekim.mynote.feature.stats.StatsTrace
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotesModule {
    @Binds
    abstract fun bindNotesMessages(impl: AndroidNotesMessages): NotesMessages

    @Binds
    abstract fun bindFailureLog(impl: AndroidFailureLog): FailureLog

    @Binds
    abstract fun bindAutoAdvancePolicy(impl: AccessibilityAutoAdvancePolicy): QuizAutoAdvancePolicy

    @Binds
    abstract fun bindStatsTrace(impl: LogStatsTrace): StatsTrace
}
