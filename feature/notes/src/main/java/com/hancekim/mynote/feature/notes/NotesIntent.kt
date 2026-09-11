package com.hancekim.mynote.feature.notes

import com.hancekim.mynote.core.common.mvi.MviEvent

/** 노트 화면에서 발생하는 사용자 인텐트(단방향: UI → ViewModel). */
sealed interface NotesIntent : MviEvent {
    /** 홈 카드/패널 노트 탭 → 해당 노트 열기 */
    data class SelectNote(
        val id: String,
    ) : NotesIntent

    /** 검색어 변경 */
    data class QueryChanged(
        val query: String,
    ) : NotesIntent

    /** 카테고리 펼침/접힘 토글 */
    data class ToggleCategory(
        val name: String,
    ) : NotesIntent

    /** 본문 글자 크기 토글(보통 ↔ 크게) */
    data object ToggleFontScale : NotesIntent

    /** 현재 노트를 닫고 홈으로(Compact 뒤로가기) */
    data object CloseNote : NotesIntent

    /** 런타임 토큰 입력 저장 */
    data class SaveToken(
        val token: String,
    ) : NotesIntent

    /** 상단 새로고침 — index 재조회 후 변경(갱신/삭제) 있으면 스낵바 안내 */
    data object Refresh : NotesIntent
}
