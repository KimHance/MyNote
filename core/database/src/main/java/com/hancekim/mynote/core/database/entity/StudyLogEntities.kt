package com.hancekim.mynote.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.hancekim.mynote.core.database.datasource.QuizFormatColumn

@Entity(
    tableName = "read_sessions",
    indices = [Index("started_at"), Index("note_id")],
)
internal data class ReadSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "note_id") val noteId: String,
    val category: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long,
    @ColumnInfo(name = "active_ms") val activeMs: Long,
    @ColumnInfo(name = "max_percent") val maxPercent: Int,
)

@Entity(
    tableName = "quiz_attempts",
    indices = [Index("finished_at"), Index("note_id")],
)
internal data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "note_id") val noteId: String,
    val category: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long,
    val asked: Int,
    val correct: Int,
    @ColumnInfo(defaultValue = QuizFormatColumn.LLM) val format: String = QuizFormatColumn.LLM,
)

@Entity(
    tableName = "quiz_answers",
    primaryKeys = ["attempt_id", "position"],
    foreignKeys = [
        ForeignKey(
            entity = QuizAttemptEntity::class,
            parentColumns = ["id"],
            childColumns = ["attempt_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class QuizAnswerEntity(
    @ColumnInfo(name = "attempt_id") val attemptId: Long,
    val position: Int,
    val question: String,
    @ColumnInfo(name = "choice_0") val choice0: String,
    @ColumnInfo(name = "choice_1") val choice1: String,
    @ColumnInfo(name = "choice_2") val choice2: String,
    @ColumnInfo(name = "choice_3") val choice3: String?,
    @ColumnInfo(name = "answer_index") val answerIndex: Int,
    @ColumnInfo(name = "picked_index") val pickedIndex: Int,
)

@Entity(
    tableName = "bookmark_events",
    indices = [Index("at")],
)
internal data class BookmarkEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "note_id") val noteId: String,
    val category: String,
    val kind: String,
    val at: Long,
)

internal data class WrongAnswerRow(
    val noteId: String,
    val category: String,
    val question: String,
    val choice0: String,
    val choice1: String,
    val choice2: String,
    val choice3: String?,
    val answerIndex: Int,
    val pickedIndex: Int,
    val at: Long,
) {
    fun choice(index: Int): String =
        when (index) {
            0 -> choice0
            1 -> choice1
            2 -> choice2
            3 -> choice3.orEmpty()
            else -> ""
        }
}
