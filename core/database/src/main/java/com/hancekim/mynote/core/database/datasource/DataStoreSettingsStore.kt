package com.hancekim.mynote.core.database.datasource

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hancekim.mynote.core.data.datasource.SettingsStore
import com.hancekim.mynote.core.domain.model.FontScale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.settingsDataStore by preferencesDataStore(name = "settings")
private val FONT_SCALE_KEY = stringPreferencesKey("font_scale")

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
}
