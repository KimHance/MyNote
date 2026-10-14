package com.hancekim.mynote.feature.notes.element

import com.hancekim.mynote.core.common.mvi.MviEvent
import com.hancekim.mynote.core.navigation.NotesTab
import com.hancekim.mynote.core.ui.element.NotesSignal

sealed interface NotesIntent : MviEvent {
    data class SelectNote(
        val id: String,
    ) : NotesIntent

    data class ForegroundChanged(
        val visible: Boolean,
    ) : NotesIntent

    data class ReaderProgress(
        val noteId: String,
        val percent: Int,
    ) : NotesIntent

    data class QueryChanged(
        val query: String,
    ) : NotesIntent

    data class ToggleCategory(
        val name: String,
    ) : NotesIntent

    data object OpenSettings : NotesIntent

    data object OpenConnect : NotesIntent

    /** 빈 편집기를 지금 스택 위에 연다(LN-1 · LN-2). */
    data object NewNote : NotesIntent

    /** 가져오기 그릇을 연다(LN-1 · LN-2). 그릇은 편집기 기능 화면이라 신호로 알린다. */
    data object Import : NotesIntent

    data class SignalShown(
        val signal: NotesSignal,
        val id: Int,
    ) : NotesIntent

    data class SwitchTab(
        val tab: NotesTab,
    ) : NotesIntent

    data object ToggleBookmark : NotesIntent

    data class RemoveBookmark(
        val noteId: String,
    ) : NotesIntent

    data object UndoRemoveBookmark : NotesIntent

    data object Refresh : NotesIntent
}
