package com.hancekim.mynote.feature.notes.element

import com.hancekim.mynote.core.common.mvi.MviEvent
import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale

sealed interface NotesIntent : MviEvent {
    data class SelectNote(
        val id: String,
        val resumeAt: Float? = null,
    ) : NotesIntent

    data class OpenQuizFor(
        val noteId: String,
    ) : NotesIntent

    data class ForegroundChanged(
        val visible: Boolean,
    ) : NotesIntent

    data class ReaderProgress(
        val noteId: String,
        val percent: Int,
    ) : NotesIntent

    data class SetStudyLogEnabled(
        val enabled: Boolean,
    ) : NotesIntent

    data object ClearStudyLog : NotesIntent

    data class QueryChanged(
        val query: String,
    ) : NotesIntent

    data class ToggleCategory(
        val name: String,
    ) : NotesIntent

    data object OpenSettings : NotesIntent

    data class SetFontScale(
        val scale: MyNoteFontScale,
    ) : NotesIntent

    data object Back : NotesIntent

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

    data object StartTokenEdit : NotesIntent

    data object CancelTokenEdit : NotesIntent

    data class SaveToken(
        val token: SecretText,
    ) : NotesIntent

    data object ClearToken : NotesIntent

    data object UndoClearToken : NotesIntent

    data object StartModelDownload : NotesIntent

    data object CancelModelDownload : NotesIntent

    data object DiscardPartialModel : NotesIntent

    data object DeleteModel : NotesIntent

    data object Refresh : NotesIntent

    data object OpenConnect : NotesIntent

    data object RequestDisconnect : NotesIntent

    data object ConfirmDisconnect : NotesIntent

    data object DismissDisconnect : NotesIntent
}
