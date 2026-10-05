package com.hancekim.mynote.feature.notes.screen.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designsystem.chart.CategoryDonut
import com.hancekim.mynote.core.designsystem.chart.CategoryStackBar
import com.hancekim.mynote.core.designsystem.chart.ChartBar
import com.hancekim.mynote.core.designsystem.chart.ChartBarKind
import com.hancekim.mynote.core.designsystem.chart.ChartSlice
import com.hancekim.mynote.core.designsystem.chart.DivergingBar
import com.hancekim.mynote.core.designsystem.chart.DivergingBars
import com.hancekim.mynote.core.designsystem.chart.HBarRow
import com.hancekim.mynote.core.designsystem.chart.TimeBars
import com.hancekim.mynote.core.designsystem.chart.rememberChartProgress
import com.hancekim.mynote.core.designsystem.component.NumberedRow
import com.hancekim.mynote.core.designsystem.component.SectionLabel
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.stats.BucketState
import com.hancekim.mynote.core.domain.stats.StatsPeriod
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.screen.bookmarks.rememberBookmarkTimeLabel
import com.hancekim.mynote.feature.notes.stats.BookmarkStatsUi
import com.hancekim.mynote.feature.notes.stats.CategoryMinutesUi
import com.hancekim.mynote.feature.notes.stats.PatternUi
import com.hancekim.mynote.feature.notes.stats.QuizStatsUi
import com.hancekim.mynote.feature.notes.stats.ReadingUi
import com.hancekim.mynote.feature.notes.stats.StaleReasonUi
import com.hancekim.mynote.feature.notes.stats.StatsDeltaFormat
import kotlinx.collections.immutable.toImmutableList

internal enum class ChartSize(
    val area: Dp,
    val maxBar: Dp,
) {
    Regular(104.dp, 70.dp),
    Wide(120.dp, 86.dp),
    Short(74.dp, 42.dp),
}

@Composable
internal fun ReadingCharts(
    period: StatsPeriod,
    reading: ReadingUi,
    size: ChartSize,
    formats: StatsFormats,
    focus: String?,
) {
    val progress = rememberChartProgress(period)
    val title = timeTitle(period)
    val minuteUnit = stringResource(R.string.notes_stats_unit_minutes)
    val minutesFormat = stringResource(R.string.notes_stats_minutes_value)
    val bars =
        reading.bars
            .map { ChartBar(formats.bucket(it.label), it.minutes, it.state.toChartKind()) }
            .toImmutableList()
    val currentIndex = reading.bars.indexOfFirst { it.state == BucketState.Current }
    val counted = bars.filter { it.kind != ChartBarKind.Pending }
    val pending = bars.withIndex().filter { it.value.kind == ChartBarKind.Pending }
    val beforeRecord = pending.filter { it.index < currentIndex }.joinToString(SEPARATOR) { it.value.label }
    val notYet = pending.filter { it.index > currentIndex }.joinToString(SEPARATOR) { it.value.label }
    val listed = counted.joinToString(SEPARATOR) { it.label + " " + minutesFormat.format(it.value) }
    val barsBase = stringResource(R.string.notes_stats_bars_desc, title, listed)
    val beforeText = stringResource(R.string.notes_stats_bars_before, beforeRecord)
    val futureText = stringResource(R.string.notes_stats_bars_future, notYet)
    val barsFuture =
        (if (beforeRecord.isEmpty()) "" else SENTENCE + beforeText) +
            (if (notYet.isEmpty()) "" else SENTENCE + futureText)
    Column(Modifier.fillMaxWidth()) {
        ChartCard(title = title, trailing = { UnitLabel(minuteUnit) }) {
            TimeBars(
                bars = bars,
                maxBarHeight = size.maxBar,
                progress = progress,
                contentDescription = barsBase + barsFuture,
                modifier = Modifier.height(size.area),
            )
        }
        Spacer(Modifier.height(CARD_GAP))
        CategoryCard(
            reading = reading,
            short = size == ChartSize.Short,
            progress = progress,
            focus = focus?.takeIf { name -> reading.legend.any { it.name == name } },
        )
    }
}

@Composable
private fun CategoryCard(
    reading: ReadingUi,
    short: Boolean,
    progress: () -> Float,
    focus: String?,
) {
    val slices =
        reading.legend
            .sortedBy { it.colorIndex }
            .map { ChartSlice(it.minutes.toFloat(), categoryColor(it.colorIndex)) }
            .toImmutableList()
    val minutesFormat = stringResource(R.string.notes_stats_minutes_value)
    val parts = reading.legend.joinToString(SEPARATOR) { it.name + " " + minutesFormat.format(it.minutes) }
    val description =
        stringResource(R.string.notes_stats_donut_desc, reading.categoryTotal, parts) +
            (focus?.let { SEPARATOR + stringResource(R.string.notes_stats_focus_desc, it) } ?: "")
    val minuteUnit = stringResource(R.string.notes_stats_unit_minutes)
    ChartCard(
        title = stringResource(R.string.notes_stats_time_by_category),
        trailing = {
            UnitLabel(
                if (short) stringResource(R.string.notes_stats_minutes_value, reading.categoryTotal) else minuteUnit,
            )
        },
    ) {
        if (short) {
            CategoryStackBar(slices = slices, progress = progress, contentDescription = description)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.clearAndSetSemantics {},
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                reading.legend.forEach { LegendItem(it, compact = true, focused = it.name == focus) }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryDonut(
                    slices = slices,
                    total = reading.categoryTotal.toString(),
                    unit = minuteUnit,
                    progress = progress,
                    contentDescription = description,
                )
                Spacer(Modifier.width(18.dp))
                Column(
                    modifier = Modifier.weight(1f).clearAndSetSemantics {},
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    reading.legend.forEach { LegendItem(it, compact = false, focused = it.name == focus) }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    item: CategoryMinutesUi,
    compact: Boolean,
    focused: Boolean,
) {
    val colors = MyNoteTheme.colors
    val color = categoryColor(item.colorIndex)
    val hollow = item.minutes == 0 || outlineOnly(item.colorIndex)
    Row(
        modifier = if (compact) Modifier else Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(SWATCH)
                .clip(RoundedCornerShape(3.dp))
                .then(
                    if (hollow) {
                        Modifier.border(
                            1.5.dp,
                            color,
                            RoundedCornerShape(3.dp),
                        )
                    } else {
                        Modifier.background(color)
                    },
                ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = item.name,
            style = MyNoteTheme.textStyles.caption,
            color = colors.bodyText,
            maxLines = 1,
            modifier = if (compact || focused) Modifier else Modifier.weight(1f),
        )
        if (focused) {
            Text(
                text = stringResource(R.string.notes_stats_focus),
                style = MyNoteTheme.textStyles.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.W600),
                color = colors.accent,
                maxLines = 1,
                modifier = Modifier.padding(start = 6.dp).then(if (compact) Modifier else Modifier.weight(1f)),
            )
        }
        Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
        Text(
            text = item.minutes.toString(),
            style = MyNoteTheme.textStyles.monoSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.W400),
            color = if (item.minutes == 0) colors.text3 else colors.text,
        )
    }
}

@Composable
internal fun ReadingList(
    reading: ReadingUi,
    compactHeight: Boolean,
    first: Boolean,
    onNotesIntent: (NotesIntent) -> Unit,
) {
    ListLabel(
        text = stringResource(R.string.notes_stats_stalled),
        count = reading.stalled.size.takeIf { it > 0 },
        first = first,
    )
    if (reading.stalled.isEmpty()) {
        EmptyLine(stringResource(R.string.notes_stats_stalled_empty))
        return
    }
    reading.stalled.forEachIndexed { index, note ->
        val time = rememberBookmarkTimeLabel(note.endedAt, long = false)
        val subtitle =
            stringResource(
                R.string.notes_stats_row_sub3,
                note.category,
                stringResource(R.string.notes_stats_stalled_at, note.percent),
                time,
            )
        NumberedRow(
            number = index + 1,
            title = note.title,
            subtitle = subtitle,
            contentDescription = stringResource(R.string.notes_stats_row_desc, index + 1, note.title, subtitle),
            onClick = { onNotesIntent(NotesIntent.SelectNote(note.noteId, resumeAt = note.percent / FULL)) },
            compactHeight = compactHeight,
            trailing = { ProgressTrack(note.percent / FULL) },
        )
    }
}

@Composable
internal fun QuizCharts(
    period: StatsPeriod,
    quiz: QuizStatsUi,
) {
    val progress = rememberChartProgress(period)
    val questions = stringResource(R.string.notes_stats_questions, quiz.asked)
    val notTaken = stringResource(R.string.notes_stats_not_taken)
    val lowest = stringResource(R.string.notes_stats_lowest)
    ChartCard(
        title = stringResource(R.string.notes_stats_accuracy_by_category),
        trailing = { UnitLabel(questions) },
    ) {
        quiz.rows.forEachIndexed { index, row ->
            val accuracy = row.accuracy
            val description =
                if (accuracy == null) {
                    row.name + SEPARATOR + notTaken
                } else {
                    stringResource(R.string.notes_stats_accuracy_row_desc, row.name, row.asked, accuracy) +
                        if (row.lowest) SEPARATOR + lowest else ""
                }
            HBarRow(
                title = row.name,
                subtitle = if (row.asked > 0) stringResource(R.string.notes_stats_questions, row.asked) else notTaken,
                value = accuracy?.let { StatsDeltaFormat.percent(it) } ?: StatsDeltaFormat.DASH,
                fraction = accuracy?.let { it / FULL },
                highlighted = row.lowest,
                progress = progress,
                contentDescription = description,
                index = index,
            )
        }
    }
}

@Composable
internal fun QuizList(
    quiz: QuizStatsUi,
    pattern: PatternUi?,
    formats: StatsFormats,
    compactHeight: Boolean,
    first: Boolean,
    onNotesIntent: (NotesIntent) -> Unit,
) {
    val colors = MyNoteTheme.colors
    if (pattern != null) {
        PatternCard(
            pattern = pattern,
            formats = formats,
            modifier = Modifier.padding(top = if (first) 0.dp else CARD_GAP),
        )
    }
    ListLabel(
        text = stringResource(R.string.notes_stats_retry),
        hint = stringResource(R.string.notes_stats_retry_hint),
        first = first && pattern == null,
    )
    if (quiz.retries.isEmpty()) {
        EmptyLine(stringResource(R.string.notes_stats_retry_empty))
        return
    }
    quiz.retries.forEachIndexed { index, retry ->
        val subtitle =
            stringResource(
                R.string.notes_stats_row_sub,
                retry.category,
                stringResource(R.string.notes_stats_retry_count, retry.ordinal),
            )
        val value = stringResource(R.string.notes_stats_retry_value, retry.first, retry.last)
        val tail = value.lastIndexOf(retry.last.toString()).coerceAtLeast(0)
        NumberedRow(
            number = index + 1,
            title = retry.title,
            subtitle = subtitle,
            contentDescription =
                stringResource(R.string.notes_stats_row_desc, index + 1, retry.title, subtitle) + SEPARATOR + value,
            onClick = { onNotesIntent(NotesIntent.OpenQuizFor(retry.noteId)) },
            compactHeight = compactHeight,
            trailing = {
                Text(
                    text =
                        buildAnnotatedString {
                            append(value.substring(0, tail))
                            withStyle(
                                if (retry.improved) {
                                    SpanStyle(color = colors.accent, fontWeight = FontWeight.W600)
                                } else {
                                    SpanStyle(color = colors.text2)
                                },
                            ) { append(value.substring(tail)) }
                        },
                    style = MyNoteTheme.textStyles.monoSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.W400),
                    color = colors.text2,
                    maxLines = 1,
                )
            },
        )
    }
}

@Composable
internal fun BookmarkCharts(
    period: StatsPeriod,
    bookmarks: BookmarkStatsUi,
    formats: StatsFormats,
) {
    val progress = rememberChartProgress(period)
    val colors = MyNoteTheme.colors
    val bars =
        bookmarks.buckets
            .map { DivergingBar(formats.bucket(it.label), it.added, it.removed, it.state.toChartKind()) }
            .toImmutableList()
    val addedFormat = stringResource(R.string.notes_stats_bookmark_added)
    val removedFormat = stringResource(R.string.notes_stats_bookmark_removed)
    val perBucket =
        bars
            .filter { it.up > 0 || it.down > 0 }
            .joinToString(SEPARATOR) { bar ->
                listOfNotNull(
                    bar.label,
                    bar.up.takeIf { it > 0 }?.let { addedFormat.format(it) },
                    bar.down.takeIf { it > 0 }?.let { removedFormat.format(it) },
                ).joinToString(" ")
            }
    val description =
        stringResource(R.string.notes_stats_bookmark_trend_desc, bookmarks.added, bookmarks.removed, perBucket) +
            SENTENCE.trim()
    ChartCard(
        title = stringResource(R.string.notes_stats_bookmark_trend),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendSwatch(colors.accent)
                Text(
                    text = stringResource(R.string.notes_stats_bookmark_added, bookmarks.added),
                    style = MyNoteTheme.textStyles.caption.copy(fontSize = 12.sp),
                    color = colors.text2,
                )
                Spacer(Modifier.width(12.dp))
                LegendSwatch(colors.text3)
                Text(
                    text = stringResource(R.string.notes_stats_bookmark_removed, bookmarks.removed),
                    style = MyNoteTheme.textStyles.caption.copy(fontSize = 12.sp),
                    color = colors.text2,
                )
            }
        },
    ) {
        DivergingBars(bars = bars, progress = progress, contentDescription = description)
    }
}

@Composable
internal fun BookmarkList(
    bookmarks: BookmarkStatsUi,
    compactHeight: Boolean,
    first: Boolean,
    onNotesIntent: (NotesIntent) -> Unit,
) {
    ListLabel(
        text = stringResource(R.string.notes_stats_bookmark_old),
        hint = stringResource(R.string.notes_stats_bookmark_old_hint),
        first = first,
    )
    if (bookmarks.stale.isEmpty()) {
        EmptyLine(stringResource(R.string.notes_stats_bookmark_old_empty))
        return
    }
    bookmarks.stale.forEachIndexed { index, stale ->
        val reason =
            when (val why = stale.reason) {
                is StaleReasonUi.LastOpened -> stringResource(R.string.notes_stats_bookmark_last_opened, why.days)
                StaleReasonUi.Unread -> stringResource(R.string.notes_stats_bookmark_unread)
                StaleReasonUi.UnreadSinceLog -> stringResource(R.string.notes_stats_bookmark_unread_since_log)
            }
        val subtitle = stringResource(R.string.notes_stats_row_sub, stale.category, reason)
        val age = stringResource(R.string.notes_stats_bookmark_age, stale.ageDays)
        NumberedRow(
            number = index + 1,
            title = stale.title,
            subtitle = subtitle,
            contentDescription =
                stringResource(R.string.notes_stats_row_desc, index + 1, stale.title, subtitle) + SEPARATOR + age,
            onClick = { onNotesIntent(NotesIntent.SelectNote(stale.noteId)) },
            compactHeight = compactHeight,
            trailing = {
                Text(
                    text = age,
                    style = MyNoteTheme.textStyles.monoSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.W400),
                    color = MyNoteTheme.colors.text2,
                    maxLines = 1,
                )
            },
        )
    }
}

@Composable
internal fun PatternCard(
    pattern: PatternUi,
    formats: StatsFormats,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val chipShape = RoundedCornerShape(8.dp)
    val title = stringResource(R.string.notes_stats_pattern)
    val generated = generatedText(pattern.generated, formats)
    val description = (listOf(title, generated) + pattern.tags + pattern.sentence).joinToString(SEPARATOR)
    ChartCard(
        title = title,
        modifier =
            modifier.clearAndSetSemantics {
                contentDescription = description
            },
        trailing = { UnitLabel(generated) },
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            pattern.tags.forEach { tag ->
                Text(
                    text = tag,
                    style = MyNoteTheme.textStyles.caption.copy(fontSize = 12.sp),
                    color = colors.bodyText,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .clip(chipShape)
                            .background(colors.surface2)
                            .border(1.dp, colors.border, chipShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Text(
            text = pattern.sentence,
            style = MyNoteTheme.textStyles.body.copy(fontSize = 14.sp, lineHeight = 1.65.em),
            color = colors.bodyText,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
internal fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(CARD_RADIUS)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.border, shape)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MyNoteTheme.textStyles.caption.copy(fontWeight = FontWeight.W600),
                color = colors.text,
                maxLines = 1,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            trailing()
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun UnitLabel(text: String) {
    Text(
        text = text,
        style = MyNoteTheme.textStyles.monoSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.W400),
        color = MyNoteTheme.colors.text3,
        maxLines = 1,
    )
}

@Composable
private fun LegendSwatch(color: Color) {
    Box(
        Modifier
            .padding(end = 6.dp)
            .size(8.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}

@Composable
private fun ListLabel(
    text: String,
    first: Boolean,
    count: Int? = null,
    hint: String? = null,
) {
    SectionLabel(
        text = text,
        count = count,
        hint = hint,
        modifier = Modifier.padding(top = if (first) 0.dp else LIST_LABEL_TOP, bottom = 4.dp),
    )
}

@Composable
private fun EmptyLine(text: String) {
    Text(
        text = text,
        style = MyNoteTheme.textStyles.bodySmall,
        color = MyNoteTheme.colors.text2,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@Composable
private fun ProgressTrack(fraction: Float) {
    val colors = MyNoteTheme.colors
    Box(
        Modifier
            .size(width = TRACK_WIDTH, height = TRACK_HEIGHT)
            .clip(RoundedCornerShape(2.dp))
            .background(colors.border),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(TRACK_HEIGHT)
                .background(colors.text2),
        )
    }
}

private val CARD_RADIUS = 18.dp
private val CARD_GAP = 12.dp
private val SWATCH = 10.dp
private val LIST_LABEL_TOP = 24.dp
private val TRACK_WIDTH = 40.dp
private val TRACK_HEIGHT = 4.dp
private const val FULL = 100f
private const val SEPARATOR = ", "
private const val SENTENCE = ". "
