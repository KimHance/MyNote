package com.hancekim.mynote.feature.notes

import android.content.Context
import com.hancekim.mynote.core.domain.model.SyncOutcome
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface NotesMessages {
    fun syncFailed(): String

    fun quizAbandoned(): String

    fun tokenSaved(): String

    fun disconnectFailed(): String

    fun tokenCleared(): String

    fun downloadPaused(): String

    fun downloadCancelled(): String

    fun partialDiscarded(): String

    fun modelDeleted(): String

    fun bookmarkAdded(): String

    fun bookmarkRemoved(): String

    fun studyLogCleared(): String

    fun syncApplied(outcome: SyncOutcome): String
}

class AndroidNotesMessages
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : NotesMessages {
        override fun syncFailed(): String = context.getString(R.string.notes_sync_failed)

        override fun quizAbandoned(): String = context.getString(R.string.notes_quiz_abandoned)

        override fun tokenSaved(): String = context.getString(R.string.notes_settings_token_saved)

        override fun disconnectFailed(): String = context.getString(R.string.notes_repo_disconnect_failed)

        override fun tokenCleared(): String = context.getString(R.string.notes_settings_token_cleared)

        override fun downloadPaused(): String = context.getString(R.string.notes_model_paused_snack)

        override fun downloadCancelled(): String = context.getString(R.string.notes_model_cancelled_snack)

        override fun partialDiscarded(): String = context.getString(R.string.notes_model_discarded_snack)

        override fun modelDeleted(): String = context.getString(R.string.notes_model_deleted)

        override fun bookmarkAdded(): String = context.getString(R.string.notes_bookmark_added)

        override fun bookmarkRemoved(): String = context.getString(R.string.notes_bookmark_removed)

        override fun studyLogCleared(): String = context.getString(R.string.notes_settings_studylog_cleared)

        override fun syncApplied(outcome: SyncOutcome): String {
            val parts =
                buildList {
                    if (outcome.changed > 0) add(context.getString(R.string.notes_sync_changed, outcome.changed))
                    if (outcome.deleted.isNotEmpty()) {
                        add(context.getString(R.string.notes_sync_deleted, outcome.deleted.size))
                    }
                }
            return context.getString(R.string.notes_sync_applied, parts.joinToString(" · "))
        }
    }
