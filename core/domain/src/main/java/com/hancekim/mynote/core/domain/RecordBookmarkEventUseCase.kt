package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.BookmarkEvent
import com.hancekim.mynote.core.domain.repository.SettingsRepository
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RecordBookmarkEventUseCase @Inject constructor(
    private val studyLog: StudyLogRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(event: BookmarkEvent): Long? {
        if (!settings.observeStudyLogEnabled().first()) return null
        return studyLog.record(event)
    }

    suspend fun cancel(eventId: Long) = studyLog.deleteBookmarkEvent(eventId)
}
