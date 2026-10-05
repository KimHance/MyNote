package com.hancekim.mynote.feature.notes.screen.stats

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designsystem.motion.MyNoteEasing
import com.hancekim.mynote.core.designsystem.motion.rememberReduceMotion
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.stats.StatsPeriod
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.stats.ComparisonUi
import com.hancekim.mynote.feature.notes.stats.DeltaUi
import com.hancekim.mynote.feature.notes.stats.PeriodNoteUi
import com.hancekim.mynote.feature.notes.stats.StatKind
import com.hancekim.mynote.feature.notes.stats.StatNumberUi
import com.hancekim.mynote.feature.notes.stats.StatsDeltaFormat
import com.hancekim.mynote.feature.notes.stats.StatsSection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDate

@Composable
internal fun StatsNotice(
    logEnabled: Boolean,
    intro: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    when {
        !logEnabled -> {
            Text(
                text = stringResource(R.string.notes_stats_log_off),
                style = MyNoteTheme.textStyles.caption.copy(lineHeight = 21.sp),
                color = colors.text2,
                modifier = modifier.padding(start = 2.dp, end = 2.dp, bottom = 12.dp),
            )
        }

        intro -> {
            Text(
                text = stringResource(R.string.notes_stats_intro_empty),
                style = MyNoteTheme.textStyles.bodySmall,
                color = colors.text2,
                modifier = modifier.padding(start = 2.dp, end = 2.dp, bottom = 18.dp),
            )
        }
    }
}

@Composable
internal fun PeriodSegment(
    selected: StatsPeriod,
    onSelect: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val reduceMotion = rememberReduceMotion()
    val periods = StatsPeriod.entries
    val offset by animateFloatAsState(
        targetValue = periods.indexOf(selected).toFloat(),
        animationSpec = if (reduceMotion) snap() else tween(SEGMENT_SLIDE_MILLIS, easing = MyNoteEasing),
        label = "periodSegment",
    )
    val shape = RoundedCornerShape(STRIP_RADIUS)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(SEGMENT_HEIGHT)
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.border, shape)
                .drawBehind {
                    val pad = SEGMENT_PADDING.toPx()
                    val gap = SEGMENT_PADDING.toPx()
                    val width = (size.width - pad * 2 - gap * (periods.size - 1)) / periods.size
                    drawRoundRect(
                        color = colors.border,
                        topLeft = Offset(pad + offset * (width + gap), pad),
                        size = Size(width, size.height - pad * 2),
                        cornerRadius = CornerRadius(SEGMENT_ITEM_RADIUS.toPx()),
                    )
                }.padding(SEGMENT_PADDING)
                .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(SEGMENT_PADDING),
    ) {
        periods.forEach { period ->
            val isSelected = period == selected
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(SEGMENT_ITEM_RADIUS))
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(period) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(period.labelRes()),
                    style =
                        MyNoteTheme.textStyles.cardTitle.copy(
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W500,
                        ),
                    color = if (isSelected) colors.text else colors.text2,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun PeriodNoteText(
    note: PeriodNoteUi,
    formats: StatsFormats,
    modifier: Modifier = Modifier,
) {
    val since = formats.date(note.since)
    Text(
        text =
            stringResource(
                if (note.short) R.string.notes_stats_period_short else R.string.notes_stats_period_partial,
                since,
                note.days,
            ),
        style = MyNoteTheme.textStyles.caption.copy(lineHeight = 21.sp),
        color = MyNoteTheme.colors.text2,
        modifier = modifier.padding(start = 2.dp, end = 2.dp, top = 10.dp),
    )
}

@Composable
internal fun NumberStrip(
    period: StatsPeriod,
    numbers: ImmutableList<StatNumberUi>,
    comparison: ComparisonUi,
    formats: StatsFormats,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(STRIP_RADIUS)
    Column(modifier = modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(shape)
                    .border(1.dp, colors.border, shape),
        ) {
            numbers.forEachIndexed { index, number ->
                if (index > 0) VerticalDivider(color = colors.border, thickness = 1.dp)
                NumberCell(period = period, number = number, modifier = Modifier.weight(1f))
            }
        }
        Text(
            text = comparisonText(comparison, formats),
            style = MyNoteTheme.textStyles.caption.copy(fontSize = 12.sp),
            color = colors.text3,
            modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 6.dp),
        )
    }
}

@Composable
private fun NumberCell(
    period: StatsPeriod,
    number: StatNumberUi,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val styles = MyNoteTheme.textStyles
    val label = stringResource(number.kind.labelRes())
    val unit = number.kind.unit()
    val value = number.value?.toString() ?: StatsDeltaFormat.DASH
    val deltaText = deltaText(number)
    val description = cellDescription(period, number, label, unit)
    Column(
        modifier =
            modifier
                .background(colors.bg)
                .semantics(mergeDescendants = true) { contentDescription = description }
                .padding(start = 10.dp, end = 6.dp, top = 10.dp, bottom = 11.dp),
    ) {
        Text(
            text = label,
            style = styles.caption.copy(fontSize = 12.sp),
            color = colors.text3,
            maxLines = 1,
            softWrap = false,
        )
        Row {
            Text(
                text = value,
                style = styles.title.copy(fontSize = 24.sp, lineHeight = 30.sp),
                color = colors.text,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.alignByBaseline(),
            )
            if (number.value != null) {
                Text(
                    text = unit,
                    style = styles.caption.copy(fontSize = 12.sp),
                    color = colors.text2,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline().padding(start = 2.dp),
                )
            }
        }
        Text(
            text = deltaText,
            style = styles.monoSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.W400),
            color = colors.text2,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
internal fun SectionChips(
    selected: StatsSection,
    onSelect: (StatsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatsSection.entries.forEach { section ->
            SectionChip(
                label = stringResource(section.labelRes()),
                selected = section == selected,
                onClick = { onSelect(section) },
            )
        }
    }
}

@Composable
private fun SectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val reduceMotion = rememberReduceMotion()
    val spec = if (reduceMotion) snap() else tween<Color>(CHIP_COLOR_MILLIS)
    val fill by animateColorAsState(
        if (selected) colors.accent.copy(alpha = SELECTED_FILL) else colors.bg.copy(alpha = 0f),
        spec,
        label = "chipFill",
    )
    val stroke by animateColorAsState(
        if (selected) colors.accent.copy(alpha = SELECTED_STROKE) else colors.border,
        spec,
        label = "chipStroke",
    )
    val text by animateColorAsState(if (selected) colors.accent else colors.text2, spec, label = "chipText")
    Box(
        modifier =
            Modifier
                .height(CHIP_TOUCH)
                .selectable(
                    selected = selected,
                    role = Role.Tab,
                    interactionSource = null,
                    indication = null,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .height(CHIP_HEIGHT)
                    .drawBehind {
                        val radius = CornerRadius(size.height / 2f)
                        drawRoundRect(fill, cornerRadius = radius)
                        val inset = 0.5.dp.toPx()
                        drawRoundRect(
                            color = stroke,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - inset * 2, size.height - inset * 2),
                            cornerRadius = radius,
                            style = Stroke(1.dp.toPx()),
                        )
                    }.padding(horizontal = CHIP_PADDING),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = label,
                style =
                    MyNoteTheme.textStyles.cardTitle.copy(
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.W600 else FontWeight.W500,
                    ),
                color = { text },
                maxLines = 1,
            )
        }
    }
}

@Composable
@ReadOnlyComposable
private fun comparisonText(
    comparison: ComparisonUi,
    formats: StatsFormats,
): String =
    when (comparison) {
        is ComparisonUi.Week -> stringResource(R.string.notes_stats_compare_week, comparison.until.short())
        is ComparisonUi.Month -> stringResource(R.string.notes_stats_compare_month, comparison.untilDay)
        is ComparisonUi.Since -> stringResource(R.string.notes_stats_compare_since, formats.date(comparison.date))
        ComparisonUi.Pending -> stringResource(R.string.notes_stats_compare_pending)
    }

@Composable
@ReadOnlyComposable
private fun deltaText(number: StatNumberUi): String =
    when (val delta = number.delta) {
        is DeltaUi.Value -> StatsDeltaFormat.signed(delta.diff, percentPoint = number.kind == StatKind.Accuracy)
        DeltaUi.FirstWeek -> stringResource(R.string.notes_stats_first_week)
        is DeltaUi.Questions -> stringResource(R.string.notes_stats_questions, delta.count)
        DeltaUi.None -> StatsDeltaFormat.DASH
        DeltaUi.Blank -> BLANK
    }

@Composable
@ReadOnlyComposable
private fun cellDescription(
    period: StatsPeriod,
    number: StatNumberUi,
    label: String,
    unit: String,
): String {
    val value = number.value?.let { it.toString() + unit } ?: StatsDeltaFormat.DASH
    val head = stringResource(R.string.notes_stats_cell_desc, label, value)
    val delta = number.delta as? DeltaUi.Value ?: return head
    val amount = StatsDeltaFormat.magnitude(delta.diff, if (number.kind == StatKind.Accuracy) PERCENT_POINT else unit)
    val week = period == StatsPeriod.Week
    val reading =
        when {
            delta.diff > 0 -> {
                stringResource(
                    if (week) R.string.notes_stats_delta_more_week else R.string.notes_stats_delta_more_month,
                    amount,
                )
            }

            delta.diff < 0 -> {
                stringResource(
                    if (week) R.string.notes_stats_delta_less_week else R.string.notes_stats_delta_less_month,
                    amount,
                )
            }

            else -> {
                stringResource(
                    if (week) R.string.notes_stats_delta_same_week else R.string.notes_stats_delta_same_month,
                )
            }
        }
    return head + SEPARATOR + reading
}

@Composable
@ReadOnlyComposable
private fun StatKind.unit(): String =
    when (this) {
        StatKind.Notes -> stringResource(R.string.notes_stats_unit_notes)
        StatKind.Minutes -> stringResource(R.string.notes_stats_unit_minutes)
        StatKind.Quizzes -> stringResource(R.string.notes_stats_unit_times)
        StatKind.Accuracy -> PERCENT
    }

private fun StatKind.labelRes(): Int =
    when (this) {
        StatKind.Notes -> R.string.notes_stats_num_notes
        StatKind.Minutes -> R.string.notes_stats_num_minutes
        StatKind.Quizzes -> R.string.notes_stats_num_quizzes
        StatKind.Accuracy -> R.string.notes_stats_num_accuracy
    }

private fun StatsPeriod.labelRes(): Int =
    when (this) {
        StatsPeriod.Week -> R.string.notes_stats_period_week
        StatsPeriod.Month -> R.string.notes_stats_period_month
        StatsPeriod.All -> R.string.notes_stats_period_all
    }

private fun StatsSection.labelRes(): Int =
    when (this) {
        StatsSection.Reading -> R.string.notes_stats_section_reading
        StatsSection.Quiz -> R.string.notes_stats_section_quiz
        StatsSection.Bookmarks -> R.string.notes_stats_section_bookmarks
    }

private val STRIP_RADIUS = 14.dp
private val SEGMENT_HEIGHT = 44.dp
private val SEGMENT_PADDING = 4.dp
private val SEGMENT_ITEM_RADIUS = 10.dp
private const val SEGMENT_SLIDE_MILLIS = 220
private val CHIP_HEIGHT = 36.dp
private val CHIP_TOUCH = 44.dp
private val CHIP_PADDING = 16.dp
private const val CHIP_COLOR_MILLIS = 160
private const val SELECTED_FILL = 0.16f
private const val SELECTED_STROKE = 0.32f
private const val PERCENT = "%"
private const val PERCENT_POINT = "%p"
private const val SEPARATOR = ", "
private const val BLANK = " "

@Preview(name = "PeriodSegment · 이번 달", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun PeriodSegmentPreview() {
    MyNoteTheme {
        PeriodSegment(selected = StatsPeriod.Month, onSelect = {}, modifier = Modifier.padding(16.dp))
    }
}

@Preview(name = "NumberStrip · 첫 주", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun NumberStripPreview() {
    MyNoteTheme {
        NumberStrip(
            period = StatsPeriod.Week,
            numbers =
                persistentListOf(
                    StatNumberUi(StatKind.Notes, 2, DeltaUi.FirstWeek),
                    StatNumberUi(StatKind.Minutes, 18, DeltaUi.FirstWeek),
                    StatNumberUi(StatKind.Quizzes, 1, DeltaUi.FirstWeek),
                    StatNumberUi(StatKind.Accuracy, 67, DeltaUi.Questions(3)),
                ),
            comparison = ComparisonUi.Since(LocalDate.of(2026, 9, 29)),
            formats = rememberStatsFormats(),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "SectionChips · 퀴즈", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun SectionChipsPreview() {
    MyNoteTheme {
        SectionChips(selected = StatsSection.Quiz, onSelect = {}, modifier = Modifier.padding(16.dp))
    }
}
