package com.hancekim.mynote.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import com.hancekim.mynote.core.database.entity.BookmarkEventEntity
import com.hancekim.mynote.core.database.entity.QuizAnswerEntity
import com.hancekim.mynote.core.database.entity.QuizAttemptEntity
import com.hancekim.mynote.core.database.entity.ReadSessionEntity
import com.hancekim.mynote.core.database.entity.WrongAnswerRow
import kotlinx.coroutines.flow.Flow

@Dao
internal interface StudyLogDao {
    @Insert
    suspend fun insertSession(session: ReadSessionEntity): Long

    @Insert
    suspend fun insertAttemptRow(attempt: QuizAttemptEntity): Long

    @Insert
    suspend fun insertAnswers(answers: List<QuizAnswerEntity>)

    @Transaction
    suspend fun insertAttempt(
        attempt: QuizAttemptEntity,
        answers: List<QuizAnswerEntity>,
    ) {
        val id = insertAttemptRow(attempt)
        insertAnswers(answers.map { it.copy(attemptId = id) })
    }

    @Insert
    suspend fun insertBookmarkEvent(event: BookmarkEventEntity): Long

    @Query("DELETE FROM bookmark_events WHERE id = :id")
    suspend fun deleteBookmarkEvent(id: Long)

    @Query("SELECT * FROM read_sessions WHERE started_at >= :from AND started_at < :until")
    fun observeSessions(
        from: Long,
        until: Long,
    ): Flow<List<ReadSessionEntity>>

    @Query("SELECT * FROM quiz_attempts WHERE finished_at >= :from AND finished_at < :until")
    fun observeAttempts(
        from: Long,
        until: Long,
    ): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM bookmark_events WHERE at >= :from AND at < :until")
    fun observeBookmarkEvents(
        from: Long,
        until: Long,
    ): Flow<List<BookmarkEventEntity>>

    @Query(
        "SELECT * FROM read_sessions AS s WHERE s.ended_at = " +
            "(SELECT MAX(ended_at) FROM read_sessions WHERE note_id = s.note_id)",
    )
    fun observeLatestSessions(): Flow<List<ReadSessionEntity>>

    @Query("SELECT * FROM quiz_attempts")
    fun observeAllAttempts(): Flow<List<QuizAttemptEntity>>

    @Query(
        "SELECT MIN(t) FROM (" +
            "SELECT MIN(started_at) AS t FROM read_sessions " +
            "UNION ALL SELECT MIN(started_at) AS t FROM quiz_attempts " +
            "UNION ALL SELECT MIN(at) AS t FROM bookmark_events)",
    )
    fun observeFirstRecordAt(): Flow<Long?>

    @Query("DELETE FROM read_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM quiz_answers")
    suspend fun deleteAllAnswers()

    @Query("DELETE FROM quiz_attempts")
    suspend fun deleteAllAttempts()

    @Query("DELETE FROM bookmark_events")
    suspend fun deleteAllBookmarkEvents()

    @Query("DELETE FROM weekly_summaries")
    suspend fun deleteAllSummaries()

    @Query("DELETE FROM quiz_patterns")
    suspend fun deleteAllPatterns()

    @Transaction
    suspend fun clear() {
        deleteAllSessions()
        deleteAllAnswers()
        deleteAllAttempts()
        deleteAllBookmarkEvents()
        deleteAllSummaries()
        deleteAllPatterns()
    }

    @Query(
        "SELECT a.note_id AS noteId, a.category AS category, q.question AS question, " +
            "q.choice_0 AS choice0, q.choice_1 AS choice1, q.choice_2 AS choice2, q.choice_3 AS choice3, " +
            "q.answer_index AS answerIndex, q.picked_index AS pickedIndex, a.finished_at AS at " +
            "FROM quiz_answers AS q JOIN quiz_attempts AS a ON a.id = q.attempt_id " +
            "WHERE a.finished_at >= :since AND q.picked_index != q.answer_index " +
            "ORDER BY a.finished_at DESC, q.position ASC LIMIT :limit",
    )
    suspend fun wrongAnswers(
        since: Long,
        limit: Int,
    ): List<WrongAnswerRow>

    @Query(
        "SELECT COUNT(*) FROM quiz_answers AS q JOIN quiz_attempts AS a ON a.id = q.attempt_id " +
            "WHERE a.finished_at >= :since AND q.picked_index != q.answer_index",
    )
    suspend fun countWrongAnswers(since: Long): Int

    @Query("DELETE FROM read_sessions WHERE started_at < :before")
    suspend fun deleteSessionsBefore(before: Long)

    @Query("DELETE FROM quiz_answers WHERE attempt_id IN (SELECT id FROM quiz_attempts WHERE finished_at < :before)")
    suspend fun deleteAnswersBefore(before: Long)

    @Query("DELETE FROM quiz_attempts WHERE finished_at < :before")
    suspend fun deleteAttemptsBefore(before: Long)

    @Query("DELETE FROM bookmark_events WHERE at < :before")
    suspend fun deleteBookmarkEventsBefore(before: Long)

    @Transaction
    suspend fun prune(before: Long) {
        deleteSessionsBefore(before)
        deleteAnswersBefore(before)
        deleteAttemptsBefore(before)
        deleteBookmarkEventsBefore(before)
    }
}
