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
