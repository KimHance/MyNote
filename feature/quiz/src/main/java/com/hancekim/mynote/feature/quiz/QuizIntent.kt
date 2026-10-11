package com.hancekim.mynote.feature.quiz

import androidx.compose.runtime.Stable
import com.hancekim.mynote.core.common.mvi.MviEvent
import com.hancekim.mynote.core.common.mvi.MviState
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.ui.element.NotesSignal

/** 퀴즈 ViewModel 이 내는 상태 — 오버레이 · 그만두기 되돌리기 · CTA 의 모델 상태. */
@Stable
data class QuizOverlayState(
    val quiz: QuizUiState = QuizUiState.Idle,
    val quizUndoMessage: String? = null,
    val quizUndoId: Int = 0,
    val modelStatus: ModelStatus = ModelStatus.Missing,
) : MviState

sealed interface QuizIntent : MviEvent {
    /** [category] 는 결과 기록에 쓰는 노트 범주 — 노트 쪽이 알려 준다(FM-6 ⑧). */
    data class OpenSetup(
        val noteId: String,
        val category: String,
    ) : QuizIntent

    data class SetCount(
        val count: Int,
    ) : QuizIntent

    data class Generate(
        val noteId: String,
        val maxCount: Int,
    ) : QuizIntent

    data class PickChoice(
        val index: Int,
    ) : QuizIntent

    data class NextQuestion(
        val fromIndex: Int? = null,
    ) : QuizIntent

    data object Retry : QuizIntent

    data class RetryWithFewer(
        val count: Int,
    ) : QuizIntent

    data object Dismiss : QuizIntent

    data object UndoExit : QuizIntent

    data class SignalShown(
        val signal: NotesSignal,
        val id: Int,
    ) : QuizIntent
}
