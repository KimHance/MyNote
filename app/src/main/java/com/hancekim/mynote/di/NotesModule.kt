package com.hancekim.mynote.di

import com.hancekim.mynote.feature.notes.AndroidFailureLog
import com.hancekim.mynote.feature.notes.AndroidNotesMessages
import com.hancekim.mynote.feature.notes.FailureLog
import com.hancekim.mynote.feature.notes.NotesMessages
import com.hancekim.mynote.feature.notes.quiz.AccessibilityAutoAdvancePolicy
import com.hancekim.mynote.feature.notes.quiz.QuizAutoAdvancePolicy
import com.hancekim.mynote.feature.notes.stats.LogStatsTrace
import com.hancekim.mynote.feature.notes.stats.StatsTrace
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
