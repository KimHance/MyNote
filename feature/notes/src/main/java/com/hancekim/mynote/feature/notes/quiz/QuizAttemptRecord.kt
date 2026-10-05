package com.hancekim.mynote.feature.notes.quiz

import com.hancekim.mynote.core.domain.model.QuizAnswer
import com.hancekim.mynote.core.domain.model.QuizAttempt

internal data class QuizMeta(
    val noteId: String,
    val startedAt: Long,
)

internal fun completedAttempt(
    before: QuizUiState,
    after: QuizUiState,
    meta: QuizMeta?,
    category: String,
    finishedAt: Long,
): QuizAttempt? {
    if (before !is QuizUiState.Solving || after !is QuizUiState.Result || meta == null) return null
    if (before.picks.size != before.questions.size || before.marks.size != before.picks.size) return null
    return QuizAttempt(
        noteId = meta.noteId,
        category = category,
        startedAt = meta.startedAt,
        finishedAt = finishedAt,
        answers =
            before.questions.zip(before.picks) { question, pick ->
                QuizAnswer(question.question, question.choices, question.answerIndex, pick)
            },
    )
}
