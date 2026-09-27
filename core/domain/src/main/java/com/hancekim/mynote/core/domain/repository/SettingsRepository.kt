package com.hancekim.mynote.core.domain.repository

import com.hancekim.mynote.core.domain.model.FontScale
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeFontScale(): Flow<FontScale>

    suspend fun setFontScale(scale: FontScale)
}
