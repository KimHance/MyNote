package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.datasource.StudyLogStore
import com.hancekim.mynote.core.domain.model.BookmarkEvent
import com.hancekim.mynote.core.domain.model.NoteFacts
import com.hancekim.mynote.core.domain.model.QuizAttempt
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.StudyWindow
import com.hancekim.mynote.core.domain.model.WrongAnswer
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class StudyLogRepositoryImpl @Inject constructor(
    private val store: StudyLogStore,
) : StudyLogRepository {
    override suspend fun record(session: ReadSession) = store.insert(session)

    override suspend fun record(attempt: QuizAttempt) = store.insert(attempt)

    override suspend fun record(event: BookmarkEvent): Long = store.insert(event)

    override suspend fun deleteBookmarkEvent(id: Long) = store.deleteBookmarkEvent(id)

    override fun observeWindow(
        from: Long,
        until: Long,
    ): Flow<StudyWindow> = store.observeWindow(from, until)

    override fun observeNoteFacts(): Flow<NoteFacts> =
        combine(store.observeLatestSessions(), store.observeAttempts()) { latest, attempts ->
            NoteFacts(latest, attempts)
        }

    override fun observeFirstRecordAt(): Flow<Long?> = store.observeFirstRecordAt()

    override suspend fun wrongAnswers(
        since: Long,
        limit: Int,
    ): List<WrongAnswer> = store.wrongAnswers(since, limit)

    override suspend fun countWrongAnswers(since: Long): Int = store.countWrongAnswers(since)

    override suspend fun clear() = store.clear()

    override suspend fun prune(before: Long) = store.prune(before)

    override suspend fun pruneAnswers(before: Long) = store.pruneAnswers(before)
}
