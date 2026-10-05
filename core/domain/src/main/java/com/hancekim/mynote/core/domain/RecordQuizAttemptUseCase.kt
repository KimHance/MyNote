package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.QuizAttempt
import com.hancekim.mynote.core.domain.repository.SettingsRepository
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RecordQuizAttemptUseCase @Inject constructor(
    private val studyLog: StudyLogRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(attempt: QuizAttempt) {
        if (!settings.observeStudyLogEnabled().first()) return
        if (attempt.answers.isEmpty()) return
        studyLog.record(attempt)
    }
}
