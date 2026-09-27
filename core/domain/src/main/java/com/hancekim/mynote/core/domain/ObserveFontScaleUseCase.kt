package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.FontScale
import com.hancekim.mynote.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFontScaleUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<FontScale> = repository.observeFontScale()
}
