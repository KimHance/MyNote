package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.SettingsRepository
import javax.inject.Inject

class SetStudyLogEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setStudyLogEnabled(enabled)
}
