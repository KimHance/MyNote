package com.hancekim.mynote.core.database.datasource

import com.hancekim.mynote.core.data.datasource.StudyLogStore
import com.hancekim.mynote.core.database.dao.StudyLogDao
import com.hancekim.mynote.core.database.entity.BookmarkEventEntity
import com.hancekim.mynote.core.database.entity.QuizAnswerEntity
import com.hancekim.mynote.core.database.entity.QuizAttemptEntity
import com.hancekim.mynote.core.database.entity.ReadSessionEntity
import com.hancekim.mynote.core.domain.model.BookmarkEvent
import com.hancekim.mynote.core.domain.model.QuizAttempt
import com.hancekim.mynote.core.domain.model.QuizAttemptSummary
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.StudyWindow
import com.hancekim.mynote.core.domain.model.WrongAnswer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StudyLogStoreImpl @Inject internal constructor(
    private val dao: StudyLogDao,
) : StudyLogStore {
    override suspend fun insert(session: ReadSession) {
        dao.insertSession(session.toEntity())
    }

    override suspend fun insert(attempt: QuizAttempt) {
        dao.insertAttempt(
            attempt =
                QuizAttemptEntity(
                    noteId = attempt.noteId,
                    category = attempt.category,
                    startedAt = attempt.startedAt,
                    finishedAt = attempt.finishedAt,
                    asked = attempt.asked,
                    correct = attempt.correct,
                ),
            answers =
                attempt.answers.mapIndexed { position, answer ->
                    QuizAnswerEntity(
                        attemptId = 0,
                        position = position,
                        question = answer.question,
                        choice0 = answer.choices.getOrElse(0) { "" },
                        choice1 = answer.choices.getOrElse(1) { "" },
                        choice2 = answer.choices.getOrElse(2) { "" },
                        choice3 = answer.choices.getOrNull(3),
                        answerIndex = answer.answerIndex,
                        pickedIndex = answer.pickedIndex,
                    )
                },
        )
    }

    override suspend fun insert(event: BookmarkEvent): Long =
        dao.insertBookmarkEvent(
            BookmarkEventEntity(
                noteId = event.noteId,
                category = event.category,
                kind = event.kind.toColumn(),
                at = event.at,
            ),
        )

    override suspend fun deleteBookmarkEvent(id: Long) = dao.deleteBookmarkEvent(id)

    override fun observeWindow(
        from: Long,
        until: Long,
    ): Flow<StudyWindow> =
        combine(
            dao.observeSessions(from, until),
            dao.observeAttempts(from, until),
            dao.observeBookmarkEvents(from, until),
        ) { sessions, attempts, events ->
            StudyWindow(
                sessions = sessions.map { it.toDomain() },
                attempts = attempts.map { it.toSummary() },
                bookmarkEvents = events.mapNotNull { it.toDomain() },
            )
        }

    override fun observeLatestSessions(): Flow<List<ReadSession>> =
        dao.observeLatestSessions().map { rows -> rows.map { it.toDomain() } }

    override fun observeAttempts(): Flow<List<QuizAttemptSummary>> =
        dao.observeAllAttempts().map { rows -> rows.map { it.toSummary() } }

    override fun observeFirstRecordAt(): Flow<Long?> = dao.observeFirstRecordAt()

    override suspend fun wrongAnswers(
        since: Long,
        limit: Int,
    ): List<WrongAnswer> =
        dao.wrongAnswers(since, limit).map { row ->
            WrongAnswer(
                noteId = row.noteId,
                category = row.category,
                question = row.question,
                picked = row.choice(row.pickedIndex),
                answer = row.choice(row.answerIndex),
                at = row.at,
            )
        }

    override suspend fun countWrongAnswers(since: Long): Int = dao.countWrongAnswers(since)

    override suspend fun clear() = dao.clear()

    override suspend fun prune(before: Long) = dao.prune(before)

    override suspend fun pruneAnswers(before: Long) = dao.deleteAnswersBefore(before)
}

private fun ReadSession.toEntity() =
    ReadSessionEntity(
        noteId = noteId,
        category = category,
        startedAt = startedAt,
        endedAt = endedAt,
        activeMs = activeMillis,
        maxPercent = maxPercent,
    )

private fun ReadSessionEntity.toDomain() =
    ReadSession(
        noteId = noteId,
        category = category,
        startedAt = startedAt,
        endedAt = endedAt,
        activeMillis = activeMs,
        maxPercent = maxPercent,
    )

private fun QuizAttemptEntity.toSummary() =
    QuizAttemptSummary(
        noteId = noteId,
        category = category,
        startedAt = startedAt,
        finishedAt = finishedAt,
        asked = asked,
        correct = correct,
    )

private fun BookmarkEventEntity.toDomain(): BookmarkEvent? =
    kind.toBookmarkEventKind()?.let { BookmarkEvent(noteId = noteId, category = category, kind = it, at = at) }
