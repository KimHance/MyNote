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
