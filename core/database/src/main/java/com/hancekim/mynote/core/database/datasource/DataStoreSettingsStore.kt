package com.hancekim.mynote.core.database.datasource

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hancekim.mynote.core.data.datasource.SettingsStore
import com.hancekim.mynote.core.domain.model.FontScale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.settingsDataStore by
    preferencesDataStore(
        name = "settings",
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    )
private val FONT_SCALE_KEY = stringPreferencesKey("font_scale")
private val STUDY_LOG_KEY = booleanPreferencesKey("study_log_enabled")

class DataStoreSettingsStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SettingsStore {
    override fun observeFontScale(): Flow<FontScale> =
        context.settingsDataStore.data
            .map { it[FONT_SCALE_KEY].toFontScale() }
            .distinctUntilChanged()

    override suspend fun setFontScale(scale: FontScale) {
        context.settingsDataStore.edit { prefs ->
            prefs[FONT_SCALE_KEY] = scale.toPreference()
        }
    }

    override fun observeStudyLogEnabled(): Flow<Boolean> =
        context.settingsDataStore.data
            .map { it[STUDY_LOG_KEY] ?: true }
            .distinctUntilChanged()

    override suspend fun setStudyLogEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[STUDY_LOG_KEY] = enabled
        }
    }
}
