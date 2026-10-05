package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.repository.SettingsRepository
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RecordReadSessionUseCase @Inject constructor(
    private val studyLog: StudyLogRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(session: ReadSession) {
        if (!settings.observeStudyLogEnabled().first()) return
        studyLog.record(session)
    }
}
