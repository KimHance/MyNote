package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.FontScale
import com.hancekim.mynote.core.domain.repository.SettingsRepository
import javax.inject.Inject

class SetFontScaleUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(scale: FontScale) = repository.setFontScale(scale)
}
