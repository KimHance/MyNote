package com.hancekim.mynote.feature.notes

import android.content.Context
import com.hancekim.mynote.core.domain.model.SyncOutcome
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotesMessages
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        fun syncFailed(): String = context.getString(R.string.notes_sync_failed)

        fun quizAbandoned(): String = context.getString(R.string.notes_quiz_abandoned)

        fun tokenSaved(): String = context.getString(R.string.notes_settings_token_saved)

        fun disconnectFailed(): String = context.getString(R.string.notes_repo_disconnect_failed)

        fun tokenCleared(): String = context.getString(R.string.notes_settings_token_cleared)

        fun downloadPaused(): String = context.getString(R.string.notes_model_paused_snack)

        fun downloadCancelled(): String = context.getString(R.string.notes_model_cancelled_snack)

        fun partialDiscarded(): String = context.getString(R.string.notes_model_discarded_snack)

        fun modelDeleted(): String = context.getString(R.string.notes_model_deleted)

        fun bookmarkAdded(): String = context.getString(R.string.notes_bookmark_added)

        fun bookmarkRemoved(): String = context.getString(R.string.notes_bookmark_removed)

        fun studyLogCleared(): String = context.getString(R.string.notes_settings_studylog_cleared)

        fun syncApplied(outcome: SyncOutcome): String {
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
