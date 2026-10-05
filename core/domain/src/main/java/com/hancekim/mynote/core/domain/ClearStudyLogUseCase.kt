package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.repository.InsightRepository
import com.hancekim.mynote.core.domain.repository.InsightScheduler
import com.hancekim.mynote.core.domain.repository.QuizModelSource
import com.hancekim.mynote.core.domain.repository.SettingsRepository
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ClearStudyLogUseCase @Inject constructor(
    private val studyLog: StudyLogRepository,
    private val insights: InsightRepository,
    private val scheduler: InsightScheduler,
    private val modelSource: QuizModelSource,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke() {
        scheduler.cancel()
        insights.bumpEpoch()
        studyLog.clear()
        insights.clearStatus()
        if (insightsWanted(modelSource.status().first(), settings.observeStudyLogEnabled().first())) {
            scheduler.schedule()
        }
    }
}

fun insightsWanted(
    status: ModelStatus,
    studyLogEnabled: Boolean,
): Boolean = status == ModelStatus.Ready && studyLogEnabled
