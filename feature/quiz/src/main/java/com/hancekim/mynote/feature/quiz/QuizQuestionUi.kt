package com.hancekim.mynote.feature.quiz

import androidx.compose.runtime.Immutable
import com.hancekim.mynote.core.domain.model.QuizQuestion
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class QuizQuestionUi(
    val question: String,
    val choices: ImmutableList<String>,
    val answerIndex: Int,
    val explanation: String,
)

internal fun QuizQuestion.toUi(): QuizQuestionUi =
    QuizQuestionUi(
        question = question,
        choices = choices.toImmutableList(),
        answerIndex = answerIndex,
        explanation = explanation,
    )
