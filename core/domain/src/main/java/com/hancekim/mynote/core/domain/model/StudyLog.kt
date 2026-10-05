package com.hancekim.mynote.core.domain.model

data class ReadSession(
    val noteId: String,
    val category: String,
    val startedAt: Long,
    val endedAt: Long,
    val activeMillis: Long,
    val maxPercent: Int,
)

data class QuizAnswer(
    val question: String,
    val choices: List<String>,
    val answerIndex: Int,
    val pickedIndex: Int,
) {
    val correct: Boolean get() = pickedIndex == answerIndex
}

data class QuizAttempt(
    val noteId: String,
    val category: String,
    val startedAt: Long,
    val finishedAt: Long,
    val answers: List<QuizAnswer>,
) {
    val asked: Int get() = answers.size

    val correct: Int get() = answers.count { it.correct }
}

data class QuizAttemptSummary(
    val noteId: String,
    val category: String,
    val startedAt: Long,
    val finishedAt: Long,
    val asked: Int,
    val correct: Int,
)

enum class BookmarkEventKind {
    Added,
    Removed,
}

data class BookmarkEvent(
    val noteId: String,
    val category: String,
    val kind: BookmarkEventKind,
    val at: Long,
)

data class StudyWindow(
    val sessions: List<ReadSession>,
    val attempts: List<QuizAttemptSummary>,
    val bookmarkEvents: List<BookmarkEvent>,
)

data class NoteFacts(
    val latestSessions: List<ReadSession>,
    val attempts: List<QuizAttemptSummary>,
)
