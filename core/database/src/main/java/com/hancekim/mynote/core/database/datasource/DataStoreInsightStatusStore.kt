package com.hancekim.mynote.core.database.datasource

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hancekim.mynote.core.data.datasource.InsightStatusStore
import com.hancekim.mynote.core.domain.model.InsightFailureKind
import com.hancekim.mynote.core.domain.model.InsightResult
import com.hancekim.mynote.core.domain.model.InsightStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.insightDataStore by
    preferencesDataStore(
        name = "study_insights",
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    )
private val LAST_RESULT = stringPreferencesKey("last_result")
private val LAST_SUCCESS_AT = longPreferencesKey("last_success_at")
private val LAST_FAILURE_AT = longPreferencesKey("last_failure_at")
private val LAST_FAILURE_KIND = stringPreferencesKey("last_failure_kind")
private val LAST_SKIPPED_AT = longPreferencesKey("last_skipped_at")
private val LOG_EPOCH = longPreferencesKey("log_epoch")

class DataStoreInsightStatusStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : InsightStatusStore {
    override fun observe(): Flow<InsightStatus> =
        context.insightDataStore.data
            .map { it.toStatus() }
            .distinctUntilChanged()

    override suspend fun markSuccess(at: Long) {
        context.insightDataStore.edit {
            it[LAST_RESULT] = InsightResult.Success.toColumn()
            it[LAST_SUCCESS_AT] = at
        }
    }

    override suspend fun markFailure(
        at: Long,
        kind: InsightFailureKind,
        summaryFailed: Boolean,
    ) {
        context.insightDataStore.edit {
            it[LAST_RESULT] = InsightResult.Failure.toColumn()
            it[LAST_FAILURE_KIND] = kind.toColumn()
            if (summaryFailed) it[LAST_FAILURE_AT] = at
        }
    }

    override suspend fun markSkipped(at: Long) {
        context.insightDataStore.edit {
            it[LAST_RESULT] = InsightResult.Skipped.toColumn()
            it[LAST_SKIPPED_AT] = at
        }
    }

    override suspend fun bumpEpoch(): Long {
        var next = 0L
        context.insightDataStore.edit {
            next = (it[LOG_EPOCH] ?: 0L) + 1
            it[LOG_EPOCH] = next
        }
        return next
    }

    override suspend fun clear() {
        context.insightDataStore.edit {
            it.remove(LAST_RESULT)
            it.remove(LAST_SUCCESS_AT)
            it.remove(LAST_FAILURE_AT)
            it.remove(LAST_FAILURE_KIND)
            it.remove(LAST_SKIPPED_AT)
        }
    }
}

private fun Preferences.toStatus() =
    InsightStatus(
        lastResult = this[LAST_RESULT]?.toInsightResult(),
        lastSuccessAt = this[LAST_SUCCESS_AT],
        lastFailureAt = this[LAST_FAILURE_AT],
        lastFailureKind = this[LAST_FAILURE_KIND]?.toFailureKind(),
        lastSkippedAt = this[LAST_SKIPPED_AT],
        logEpoch = this[LOG_EPOCH] ?: 0L,
    )
