package com.hancekim.mynote.core.ui

import android.app.Application
import com.hancekim.mynote.core.domain.model.SyncOutcome
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
        private val context: Application,
    ) : NotesMessages {
        override fun syncFailed(): String = context.getString(R.string.ui_sync_failed)

        override fun quizAbandoned(): String = context.getString(R.string.ui_quiz_abandoned)

        override fun tokenSaved(): String = context.getString(R.string.ui_settings_token_saved)

        override fun disconnectFailed(): String = context.getString(R.string.ui_repo_disconnect_failed)

        override fun tokenCleared(): String = context.getString(R.string.ui_settings_token_cleared)

        override fun downloadPaused(): String = context.getString(R.string.ui_model_paused_snack)

        override fun downloadCancelled(): String = context.getString(R.string.ui_model_cancelled_snack)

        override fun partialDiscarded(): String = context.getString(R.string.ui_model_discarded_snack)

        override fun modelDeleted(): String = context.getString(R.string.ui_model_deleted)

        override fun bookmarkAdded(): String = context.getString(R.string.ui_bookmark_added)

        override fun bookmarkRemoved(): String = context.getString(R.string.ui_bookmark_removed)

        override fun studyLogCleared(): String = context.getString(R.string.ui_settings_studylog_cleared)

        override fun syncApplied(outcome: SyncOutcome): String {
            val parts =
                buildList {
                    if (outcome.changed > 0) add(context.getString(R.string.ui_sync_changed, outcome.changed))
                    if (outcome.deleted.isNotEmpty()) {
                        add(context.getString(R.string.ui_sync_deleted, outcome.deleted.size))
                    }
                }
            return context.getString(R.string.ui_sync_applied, parts.joinToString(" · "))
        }
    }
