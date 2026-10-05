package com.hancekim.mynote.core.database.datasource

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hancekim.mynote.core.data.datasource.BookmarkStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.bookmarkDataStore by
    preferencesDataStore(
        name = "bookmarks",
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceMigrations = { listOf(BookmarkKeyMigration) },
    )

class DataStoreBookmarkStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : BookmarkStore {
    override fun observe(): Flow<Map<String, Long>> =
        context.bookmarkDataStore.data
            .map { prefs ->
                buildMap {
                    prefs.asMap().forEach { (key, value) ->
                        if (value is Long) put(key.name, value)
                    }
                }
            }.distinctUntilChanged()

    override suspend fun put(
        noteId: String,
        addedAt: Long,
    ) {
        context.bookmarkDataStore.edit { prefs -> prefs[longPreferencesKey(noteId)] = addedAt }
    }

    override suspend fun remove(noteIds: Collection<String>) {
        if (noteIds.isEmpty()) return
        context.bookmarkDataStore.edit { prefs -> noteIds.forEach { prefs.remove(longPreferencesKey(it)) } }
    }

    override suspend fun countByPrefix(prefix: String): Int =
        context.bookmarkDataStore.data
            .first()
            .asMap()
            .count { (key, value) -> value is Long && key.name.startsWith(prefix) }

    override suspend fun removeByPrefix(prefix: String) {
        context.bookmarkDataStore.edit { prefs ->
            prefs
                .asMap()
                .keys
                .filter { it.name.startsWith(prefix) }
                .forEach { prefs.remove(it) }
        }
    }
}
