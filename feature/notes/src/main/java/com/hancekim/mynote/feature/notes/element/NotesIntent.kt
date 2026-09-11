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

    data object CloseNote : NotesIntent

    data class SaveToken(
        val token: String,
    ) : NotesIntent

    data object Refresh : NotesIntent
}
