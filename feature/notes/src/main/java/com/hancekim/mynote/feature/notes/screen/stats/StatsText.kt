package com.hancekim.mynote.feature.notes.screen.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.hancekim.mynote.core.designsystem.chart.ChartBarKind
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.stats.BucketState
import com.hancekim.mynote.core.domain.stats.StatsPeriod
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.stats.BucketLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Immutable
internal class StatsFormats(
    val date: DateTimeFormatter,
    val week: DateTimeFormatter,
    val month: DateTimeFormatter,
) {
    fun date(value: LocalDate): String = date.format(value)

    fun bucket(label: BucketLabel): String =
        when (label) {
            is BucketLabel.Day -> label.day.short()
            is BucketLabel.Week -> week.format(label.start)
            is BucketLabel.Month -> month.format(label.month)
        }
}

@Composable
internal fun rememberStatsFormats(): StatsFormats {
    val date = stringResource(R.string.notes_stats_date)
    val week = stringResource(R.string.notes_stats_bucket_week)
    val month = stringResource(R.string.notes_stats_bucket_month)
    return remember(date, week, month) {
        StatsFormats(
            date = DateTimeFormatter.ofPattern(date, Locale.KOREAN),
            week = DateTimeFormatter.ofPattern(week, Locale.KOREAN),
            month = DateTimeFormatter.ofPattern(month, Locale.KOREAN),
        )
    }
}

internal fun DayOfWeek.short(): String = getDisplayName(TextStyle.SHORT, Locale.KOREAN)

internal fun BucketState.toChartKind(): ChartBarKind =
    when (this) {
        BucketState.Past -> ChartBarKind.Past
        BucketState.Current -> ChartBarKind.Current
        BucketState.Future, BucketState.BeforeRecord -> ChartBarKind.Pending
    }

@Composable
@ReadOnlyComposable
internal fun categoryColor(index: Int): Color {
    val colors = MyNoteTheme.colors
    return when (index) {
        0 -> colors.accent
        1 -> colors.chartAccent2
        2 -> colors.chartAccent3
        3 -> colors.text2
        else -> colors.text3
    }
}

internal fun outlineOnly(index: Int): Boolean = index >= PALETTE_SIZE

@Composable
@ReadOnlyComposable
internal fun timeTitle(period: StatsPeriod): String =
    stringResource(
        when (period) {
            StatsPeriod.Week -> R.string.notes_stats_time_by_day
            StatsPeriod.Month -> R.string.notes_stats_time_by_week
            StatsPeriod.All -> R.string.notes_stats_time_by_month
        },
    )

private const val PALETTE_SIZE = 5
