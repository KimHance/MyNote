package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.datasource.SettingsStore
import com.hancekim.mynote.core.domain.model.FontScale
import com.hancekim.mynote.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val settingsStore: SettingsStore,
) : SettingsRepository {
    override fun observeFontScale(): Flow<FontScale> = settingsStore.observeFontScale()

    override suspend fun setFontScale(scale: FontScale) = settingsStore.setFontScale(scale)

    override fun observeStudyLogEnabled(): Flow<Boolean> = settingsStore.observeStudyLogEnabled()

    override suspend fun setStudyLogEnabled(enabled: Boolean) = settingsStore.setStudyLogEnabled(enabled)
}
