package com.hancekim.mynote.feature.stats

import androidx.compose.runtime.Immutable
import com.hancekim.mynote.core.common.mvi.MviEvent
import com.hancekim.mynote.core.common.mvi.MviState
import com.hancekim.mynote.core.domain.stats.BucketState
import com.hancekim.mynote.core.domain.stats.StatsPeriod
import kotlinx.collections.immutable.ImmutableList
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class StatsSection {
    Reading,
    Quiz,
    Bookmarks,
}

sealed interface StatsIntent : MviEvent {
    data class SelectPeriod(
        val period: StatsPeriod,
    ) : StatsIntent

    data class SelectSection(
        val section: StatsSection,
    ) : StatsIntent

    data object Entered : StatsIntent

    data object OpenHistory : StatsIntent

    data class OpenNote(
        val noteId: String,
        val resumeAt: Float? = null,
    ) : StatsIntent

    data class RetryQuiz(
        val noteId: String,
    ) : StatsIntent
}

@Immutable
sealed interface SummaryLabel {
    data object ThisWeek : SummaryLabel

    data object LastWeek : SummaryLabel

    data class Range(
        val start: LocalDate,
        val end: LocalDate,
    ) : SummaryLabel
}

@Immutable
data class SummaryCardUi(
    val label: SummaryLabel,
    val generated: GeneratedAgo,
    val headline: String,
    val detail: String,
    val focus: String?,
    val stale: Boolean,
    val running: Boolean,
    val failed: Boolean,
    val focusInLegend: Boolean,
)

@Immutable
data class PatternUi(
    val tags: ImmutableList<String>,
    val sentence: String,
    val generated: GeneratedAgo,
)

@Immutable
data class HistoryWeekUi(
    val weekStart: LocalDate,
    val current: Boolean,
    val headline: String?,
    val detail: String?,
    val focus: String?,
    val generatedOn: LocalDate?,
    val notes: Int,
    val minutes: Int,
    val quizzes: Int,
    val accuracy: Int?,
)

@Immutable
data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.Week,
    val section: StatsSection = StatsSection.Reading,
    val logEnabled: Boolean = true,
    val summary: SummaryCardUi? = null,
    val content: StatsContentUi? = null,
    val pattern: PatternUi? = null,
    val history: ImmutableList<HistoryWeekUi>? = null,
) : MviState

@Immutable
data class StatsContentUi(
    val period: StatsPeriod,
    val intro: Boolean,
    val periodNote: PeriodNoteUi?,
    val numbers: ImmutableList<StatNumberUi>,
    val comparison: ComparisonUi,
    val reading: ReadingUi,
    val quiz: QuizStatsUi,
    val bookmarks: BookmarkStatsUi,
)

@Immutable
data class PeriodNoteUi(
    val since: LocalDate,
    val days: Int,
) {
    val short: Boolean get() = days <= SHORT_NOTE_DAYS
}

enum class StatKind {
    Notes,
    Minutes,
    Quizzes,
    Accuracy,
}

@Immutable
data class StatNumberUi(
    val kind: StatKind,
    val value: Int?,
    val delta: DeltaUi,
)

@Immutable
sealed interface DeltaUi {
    data class Value(
        val diff: Int,
    ) : DeltaUi

    data object FirstWeek : DeltaUi

    data class Questions(
        val count: Int,
    ) : DeltaUi

    data object None : DeltaUi

    data object Blank : DeltaUi
}

@Immutable
sealed interface ComparisonUi {
    data class Week(
        val until: DayOfWeek,
    ) : ComparisonUi

    data class Month(
        val untilDay: Int,
    ) : ComparisonUi

    data class Since(
        val date: LocalDate,
    ) : ComparisonUi

    data object Pending : ComparisonUi
}

@Immutable
sealed interface BucketLabel {
    data class Day(
        val day: DayOfWeek,
    ) : BucketLabel

    data class Week(
        val start: LocalDate,
    ) : BucketLabel

    data class Month(
        val month: YearMonth,
    ) : BucketLabel
}

@Immutable
data class TimeBarUi(
    val label: BucketLabel,
    val minutes: Int,
    val state: BucketState,
)

@Immutable
data class CategoryMinutesUi(
    val name: String,
    val colorIndex: Int,
    val minutes: Int,
)

@Immutable
data class StalledNoteUi(
    val noteId: String,
    val title: String,
    val category: String,
    val percent: Int,
    val endedAt: Long,
)

@Immutable
data class ReadingUi(
    val bars: ImmutableList<TimeBarUi>,
    val legend: ImmutableList<CategoryMinutesUi>,
    val categoryTotal: Int,
    val stalled: ImmutableList<StalledNoteUi>,
)

@Immutable
data class AccuracyRowUi(
    val name: String,
    val asked: Int,
    val accuracy: Int?,
    val lowest: Boolean,
)

@Immutable
data class RetryUi(
    val noteId: String,
    val title: String,
    val category: String,
    val first: Int,
    val last: Int,
    val ordinal: Int,
) {
    val improved: Boolean get() = last > first
}

@Immutable
data class QuizStatsUi(
    val asked: Int,
    val rows: ImmutableList<AccuracyRowUi>,
    val retries: ImmutableList<RetryUi>,
)

@Immutable
data class BookmarkBucketUi(
    val label: BucketLabel,
    val added: Int,
    val removed: Int,
    val state: BucketState,
)

@Immutable
sealed interface StaleReasonUi {
    data class LastOpened(
        val days: Int,
    ) : StaleReasonUi

    data object Unread : StaleReasonUi

    data object UnreadSinceLog : StaleReasonUi
}

@Immutable
data class StaleBookmarkUi(
    val noteId: String,
    val title: String,
    val category: String,
    val reason: StaleReasonUi,
    val ageDays: Int,
)

@Immutable
data class BookmarkStatsUi(
    val buckets: ImmutableList<BookmarkBucketUi>,
    val added: Int,
    val removed: Int,
    val stale: ImmutableList<StaleBookmarkUi>,
)

private const val SHORT_NOTE_DAYS = 7
