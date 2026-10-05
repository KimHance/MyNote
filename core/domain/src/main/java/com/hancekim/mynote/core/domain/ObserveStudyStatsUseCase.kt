package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.repository.BookmarkRepository
import com.hancekim.mynote.core.domain.repository.NotesRepository
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import com.hancekim.mynote.core.domain.stats.NoteRef
import com.hancekim.mynote.core.domain.stats.StatsPeriod
import com.hancekim.mynote.core.domain.stats.StatsPeriodRange
import com.hancekim.mynote.core.domain.stats.StudyStats
import com.hancekim.mynote.core.domain.stats.StudyStatsCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject

class ObserveStudyStatsUseCase @Inject constructor(
    private val studyLog: StudyLogRepository,
    private val notes: NotesRepository,
    private val bookmarks: BookmarkRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        period: StatsPeriod,
        now: ZonedDateTime,
    ): Flow<StudyStats> =
        studyLog
            .observeFirstRecordAt()
            .distinctUntilChanged()
            .flatMapLatest { firstRecordAt ->
                val firstRecord = firstRecordAt?.let { Instant.ofEpochMilli(it).atZone(now.zone).toLocalDate() }
                val range = StatsPeriodRange.of(period, now, firstRecord)
                val from = (range.compareStart ?: range.start).toInstant().toEpochMilli()
                combine(
                    studyLog.observeWindow(from, Long.MAX_VALUE),
                    studyLog.observeNoteFacts(),
                    notes.observeCategories().map { it.toNoteRefs() },
                    bookmarks.observeBookmarks(),
                ) { window, facts, refs, marks ->
                    StudyStatsCalculator.compute(range, now, firstRecordAt, window, facts, refs, marks)
                }
            }
}

private fun List<Category>.toNoteRefs(): List<NoteRef> =
    flatMap { category -> category.notes.map { NoteRef(it.id, it.title, category.name) } }
