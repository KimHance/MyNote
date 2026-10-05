package com.hancekim.mynote.core.data.datasource

import com.hancekim.mynote.core.domain.model.BookmarkEvent
import com.hancekim.mynote.core.domain.model.QuizAttempt
import com.hancekim.mynote.core.domain.model.QuizAttemptSummary
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.StudyWindow
import com.hancekim.mynote.core.domain.model.WrongAnswer
import kotlinx.coroutines.flow.Flow

interface StudyLogStore {
    suspend fun insert(session: ReadSession)

    suspend fun insert(attempt: QuizAttempt)

    suspend fun insert(event: BookmarkEvent): Long

    suspend fun deleteBookmarkEvent(id: Long)

    fun observeWindow(
        from: Long,
        until: Long,
    ): Flow<StudyWindow>

    fun observeLatestSessions(): Flow<List<ReadSession>>

    fun observeAttempts(): Flow<List<QuizAttemptSummary>>

    fun observeFirstRecordAt(): Flow<Long?>

    suspend fun wrongAnswers(
        since: Long,
        limit: Int,
    ): List<WrongAnswer>

    suspend fun countWrongAnswers(since: Long): Int

    suspend fun clear()

    suspend fun prune(before: Long)

    suspend fun pruneAnswers(before: Long)
}
