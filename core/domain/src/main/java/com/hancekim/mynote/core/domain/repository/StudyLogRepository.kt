package com.hancekim.mynote.core.domain.repository

import com.hancekim.mynote.core.domain.model.BookmarkEvent
import com.hancekim.mynote.core.domain.model.NoteFacts
import com.hancekim.mynote.core.domain.model.QuizAttempt
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.StudyWindow
import com.hancekim.mynote.core.domain.model.WrongAnswer
import kotlinx.coroutines.flow.Flow

interface StudyLogRepository {
    suspend fun record(session: ReadSession)

    suspend fun record(attempt: QuizAttempt)

    suspend fun record(event: BookmarkEvent): Long

    suspend fun deleteBookmarkEvent(id: Long)

    fun observeWindow(
        from: Long,
        until: Long,
    ): Flow<StudyWindow>

    fun observeNoteFacts(): Flow<NoteFacts>

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
