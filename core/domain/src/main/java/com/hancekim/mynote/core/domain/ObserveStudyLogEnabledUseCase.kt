package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveStudyLogEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeStudyLogEnabled()
}
