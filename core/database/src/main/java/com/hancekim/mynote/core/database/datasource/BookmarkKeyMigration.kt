package com.hancekim.mynote.core.database.datasource

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.hancekim.mynote.core.domain.model.NoteKey
import com.hancekim.mynote.core.domain.model.SourceId

internal object BookmarkKeyMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean = currentData.legacyKeys().isNotEmpty()

    override suspend fun migrate(currentData: Preferences): Preferences {
        val next = currentData.toMutablePreferences()
        currentData.legacyKeys().forEach { (key, addedAt) ->
            next.remove(key)
            val target = longPreferencesKey(NoteKey(SourceId.Legacy, key.name).value)
            if (next[target] == null) next[target] = addedAt
        }
        return next.toPreferences()
    }

    override suspend fun cleanUp() = Unit
}

private fun Preferences.legacyKeys(): List<Pair<Preferences.Key<Long>, Long>> =
    asMap().mapNotNull { (key, value) ->
        if ('/' !in key.name && value is Long) Pair(longPreferencesKey(key.name), value) else null
    }
