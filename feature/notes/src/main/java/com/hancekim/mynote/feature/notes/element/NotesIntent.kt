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

    data class OpenQuizSetup(
        val noteId: String,
    ) : NotesIntent

    data class SetQuizCount(
        val count: Int,
    ) : NotesIntent

    data class GenerateQuiz(
        val noteId: String,
        val maxCount: Int,
    ) : NotesIntent

    data class PickChoice(
        val index: Int,
    ) : NotesIntent

    data class NextQuestion(
        val fromIndex: Int? = null,
    ) : NotesIntent

    data object RetryQuiz : NotesIntent

    data class RetryQuizWithFewer(
        val count: Int,
    ) : NotesIntent

    data object DismissQuiz : NotesIntent

    data object UndoQuizExit : NotesIntent

    data class SaveToken(
        val token: String,
    ) : NotesIntent

    data object Refresh : NotesIntent
}
