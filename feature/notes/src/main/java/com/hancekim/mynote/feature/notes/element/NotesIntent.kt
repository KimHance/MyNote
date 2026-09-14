package com.hancekim.mynote.feature.notes.element

import com.hancekim.mynote.core.common.mvi.MviEvent

sealed interface NotesIntent : MviEvent {
    data class SelectNote(
        val id: String,
    ) : NotesIntent

    data class QueryChanged(
        val query: String,
    ) : NotesIntent

    data class ToggleCategory(
        val name: String,
    ) : NotesIntent

    data object ToggleFontScale : NotesIntent

    data object Back : NotesIntent

    data object CloseNote : NotesIntent

    /** 본문 끝 CTA. 바로 만들지 않고 문항 수를 고르는 다이얼로그를 연다. */
    data class OpenQuizSetup(
        val noteId: String,
    ) : NotesIntent

    data class SetQuizCount(
        val count: Int,
    ) : NotesIntent

    /** 실제 생성 시작. 첫 호출은 엔진 로딩까지 포함해 10초 이상 걸린다. */
    data class GenerateQuiz(
        val noteId: String,
        val maxCount: Int,
    ) : NotesIntent

    data class PickChoice(
        val index: Int,
    ) : NotesIntent

    /**
     * 다음 문항으로.
     *
     * [fromIndex] 는 자동 진행 타이머가 늦게 도착해 **엉뚱한 문항을 건너뛰는 것**을 막는다.
     * 사용자가 버튼으로 넘긴 경우는 null 이다.
     */
    data class NextQuestion(
        val fromIndex: Int? = null,
    ) : NotesIntent

    data object RetryQuiz : NotesIntent

    data class RetryQuizWithFewer(
        val count: Int,
    ) : NotesIntent

    /** 취소·닫기·백프레스 공용. 어느 화면에서 왔는지는 상태가 안다. */
    data object DismissQuiz : NotesIntent

    /** 풀던 퀴즈를 스낵바로 되살린다. */
    data object UndoQuizExit : NotesIntent

    data class SaveToken(
        val token: String,
    ) : NotesIntent

    data object Refresh : NotesIntent
}
