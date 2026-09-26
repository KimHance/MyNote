package com.hancekim.mynote.feature.notes.element

import androidx.compose.runtime.Immutable
import com.hancekim.mynote.feature.notes.QuizQuestionUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Immutable
sealed interface QuizUiState {
    data object Idle : QuizUiState

    data class Setup(
        val noteId: String,
        val maxCount: Int = DEFAULT_COUNT,
    ) : QuizUiState

    data class Loading(
        val noteId: String,
        val maxCount: Int,
    ) : QuizUiState

    data class Solving(
        val questions: ImmutableList<QuizQuestionUi>,
        val index: Int = 0,
        val picked: Int? = null,
        val marks: ImmutableList<Boolean> = persistentListOf(),
        val partial: Boolean = false,
        val autoAdvancing: Boolean = false,
    ) : QuizUiState {
        val current: QuizQuestionUi get() = questions[index]

        val isLast: Boolean get() = index == questions.lastIndex

        val revealed: Boolean get() = picked != null

        val isCorrect: Boolean get() = picked == current.answerIndex

        fun pick(choice: Int): Solving =
            if (revealed || choice !in current.choices.indices) {
                this
            } else {
                copy(picked = choice, marks = (marks + (choice == current.answerIndex)).toImmutableList())
            }

        fun advance(): QuizUiState =
            when {
                !revealed -> this
                isLast -> Result(marks)
                else -> copy(index = index + 1, picked = null, autoAdvancing = false)
            }
    }

    data class Result(
        val marks: ImmutableList<Boolean>,
    ) : QuizUiState {
        val total: Int get() = marks.size

        val score: Int get() = marks.count { it }

        val rate: Float get() = if (total == 0) 0f else score.toFloat() / total
    }

    data class Failed(
        val kind: Kind,
        val maxCount: Int,
        val noteId: String,
    ) : QuizUiState {
        enum class Kind { Parse, Empty, Engine, Timeout }

        val canRetryWithFewer: Boolean
            get() = kind != Kind.Empty && maxCount > MIN_COUNT
    }

    val coversReader: Boolean
        get() = this is Loading || this is Solving || this is Result || this is Failed

    companion object {
        const val MIN_COUNT = 3
        const val DEFAULT_COUNT = 4
        const val MAX_COUNT = 5
        val COUNT_CHOICES: ImmutableList<Int> = persistentListOf(MIN_COUNT, DEFAULT_COUNT, MAX_COUNT)
        const val AUTO_ADVANCE_MILLIS = 3000
    }
}
