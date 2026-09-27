package com.hancekim.mynote.core.data.datasource

import com.hancekim.mynote.core.domain.model.FontScale
import kotlinx.coroutines.flow.Flow

interface SettingsStore {
    fun observeFontScale(): Flow<FontScale>

    suspend fun setFontScale(scale: FontScale)
}
