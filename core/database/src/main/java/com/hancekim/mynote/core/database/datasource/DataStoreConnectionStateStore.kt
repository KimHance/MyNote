package com.hancekim.mynote.core.database.datasource

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hancekim.mynote.core.data.datasource.AttemptRecord
import com.hancekim.mynote.core.data.datasource.ConnectionStateStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private val Context.connectionStateDataStore by
    preferencesDataStore(
        name = "connection_state",
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    )
private val Context.attemptTokenDataStore by
    preferencesDataStore(
        name = "connection_attempt_token",
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    )
private val USED_IDS = stringSetPreferencesKey("used_source_ids")
private val ATTEMPT_SOURCE = stringPreferencesKey("attempt_source")
private val ATTEMPT_ID = stringPreferencesKey("attempt_id")
private val ATTEMPT_TOKEN = stringPreferencesKey("attempt_token")
private val ATTEMPT_CANDIDATE = booleanPreferencesKey("attempt_candidate")
private val DISCONNECT_MARKER = stringPreferencesKey("disconnect_marker")
private val SEED_BLOCKED = booleanPreferencesKey("seed_blocked")
private val BOOKMARK_SWEEP = stringSetPreferencesKey("bookmark_sweep")

class DataStoreConnectionStateStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ConnectionStateStore {
    private suspend fun read() = context.connectionStateDataStore.data.first()

    override suspend fun usedSourceIds(): Set<String> = read()[USED_IDS].orEmpty()

    override suspend fun addUsedSourceId(id: String) {
        context.connectionStateDataStore.edit { it[USED_IDS] = it[USED_IDS].orEmpty() + id }
    }

    override suspend fun attempt(): AttemptRecord? {
        val prefs = read()
        val source = prefs[ATTEMPT_SOURCE] ?: return null
        val attemptId = prefs[ATTEMPT_ID] ?: return null
        val token = context.attemptTokenDataStore.data.first()[ATTEMPT_TOKEN] ?: prefs[ATTEMPT_TOKEN]
        return AttemptRecord(source, attemptId, token, prefs[ATTEMPT_CANDIDATE] == true)
    }

    override suspend fun saveAttempt(record: AttemptRecord) {
        context.attemptTokenDataStore.edit { prefs ->
            val before = record.tokenBefore
            if (before == null) prefs.remove(ATTEMPT_TOKEN) else prefs[ATTEMPT_TOKEN] = before
        }
        context.connectionStateDataStore.edit { prefs ->
            prefs[ATTEMPT_SOURCE] = record.sourceId
            prefs[ATTEMPT_ID] = record.attemptId
            prefs[ATTEMPT_CANDIDATE] = record.candidateToken
            prefs.remove(ATTEMPT_TOKEN)
        }
    }

    override suspend fun clearAttempt() {
        context.connectionStateDataStore.edit { prefs ->
            prefs.remove(ATTEMPT_SOURCE)
            prefs.remove(ATTEMPT_ID)
            prefs.remove(ATTEMPT_CANDIDATE)
            prefs.remove(ATTEMPT_TOKEN)
        }
        context.attemptTokenDataStore.edit { it.remove(ATTEMPT_TOKEN) }
    }

    override suspend fun disconnectMarker(): String? = read()[DISCONNECT_MARKER]

    override suspend fun markDisconnect(sourceId: String) {
        context.connectionStateDataStore.edit { prefs ->
            prefs[DISCONNECT_MARKER] = sourceId
            prefs[SEED_BLOCKED] = true
        }
    }

    override suspend fun clearDisconnectMarker() {
        context.connectionStateDataStore.edit { it.remove(DISCONNECT_MARKER) }
    }

    override suspend fun clearDisconnectAndSeedBlock() {
        context.connectionStateDataStore.edit { prefs ->
            prefs.remove(DISCONNECT_MARKER)
            prefs.remove(SEED_BLOCKED)
        }
    }

    override suspend fun isSeedBlocked(): Boolean = read()[SEED_BLOCKED] == true

    override suspend fun clearSeedBlock() {
        context.connectionStateDataStore.edit { it.remove(SEED_BLOCKED) }
    }

    override suspend fun bookmarkSweep(): Set<String> = read()[BOOKMARK_SWEEP].orEmpty()

    override suspend fun addBookmarkSweep(ids: Collection<String>) {
        context.connectionStateDataStore.edit { it[BOOKMARK_SWEEP] = it[BOOKMARK_SWEEP].orEmpty() + ids }
    }

    override suspend fun clearBookmarkSweep() {
        context.connectionStateDataStore.edit { it.remove(BOOKMARK_SWEEP) }
    }
}
