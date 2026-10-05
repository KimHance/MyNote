package com.hancekim.mynote.feature.notes.screen.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.component.PressIndication
import com.hancekim.mynote.core.designsystem.motion.MyNoteEasing
import com.hancekim.mynote.core.designsystem.motion.rememberReduceMotion
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.stats.GeneratedAgo
import com.hancekim.mynote.feature.notes.stats.SummaryCardUi
import com.hancekim.mynote.feature.notes.stats.SummaryLabel
import java.time.LocalDate

internal enum class SummaryForm {
    Regular,
    Wide,
    Short,
}

@Immutable
private class SummaryMetrics(
    val padTop: Dp,
    val padSide: Dp,
    val padBottom: Dp,
    val radius: Dp,
    val metaHeight: Dp,
    val headlineSize: Int,
    val headlineGap: Dp,
    val detailSize: Int,
    val detailLine: Float,
    val maxLines: Int,
    val bottomGap: Dp,
)

private fun SummaryForm.metrics(): SummaryMetrics =
    when (this) {
        SummaryForm.Regular -> {
            SummaryMetrics(
                16.dp,
                18.dp,
                18.dp,
                18.dp,
                32.dp,
                19,
                8.dp,
                15,
                1.7f,
                Int.MAX_VALUE,
                20.dp,
            )
        }

        SummaryForm.Wide -> {
            SummaryMetrics(18.dp, 22.dp, 20.dp, 18.dp, 32.dp, 21, 8.dp, 15, 1.7f, Int.MAX_VALUE, 20.dp)
        }

        SummaryForm.Short -> {
            SummaryMetrics(12.dp, 16.dp, 14.dp, 16.dp, 28.dp, 16, 4.dp, 14, 1.6f, 2, 12.dp)
        }
    }

@Composable
internal fun SummaryCardSlot(
    summary: SummaryCardUi?,
    form: SummaryForm,
    formats: StatsFormats,
    onOpenHistory: () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    val remembered = remember { arrayOfNulls<SummaryCardUi>(1) }
    if (summary != null) remembered[0] = summary
    AnimatedVisibility(
        visible = summary != null,
        enter =
            if (reduceMotion) {
                fadeIn(tween(0))
            } else {
                expandVertically(tween(ENTER_MILLIS, easing = MyNoteEasing)) +
                    fadeIn(tween(ENTER_MILLIS))
            },
        exit =
            if (reduceMotion) {
                fadeOut(tween(0))
            } else {
                shrinkVertically(tween(EXIT_MILLIS)) +
                    fadeOut(tween(EXIT_MILLIS))
            },
        label = "summaryCard",
    ) {
        val shown = summary ?: remembered[0]
        if (shown != null) {
            SummaryCard(
                summary = shown,
                form = form,
                formats = formats,
                onOpenHistory = onOpenHistory,
                modifier = Modifier.padding(bottom = form.metrics().bottomGap),
            )
        }
    }
}

@Composable
internal fun SummaryCard(
    summary: SummaryCardUi,
    form: SummaryForm,
    formats: StatsFormats,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val metrics = form.metrics()
    val shape = RoundedCornerShape(metrics.radius)
    val reduceMotion = rememberReduceMotion()
    val label = summaryLabel(summary.label, formats)
    val meta = generatedText(summary.generated, formats)
    val status =
        when {
            summary.running -> stringResource(R.string.notes_stats_summary_running)
            summary.failed -> stringResource(R.string.notes_stats_summary_failed)
            else -> null
        }
    val focus = summary.focus?.takeIf { !summary.stale }
    val description =
        stringResource(R.string.notes_stats_summary_desc, label, summary.headline, summary.detail) +
            (focus?.let { SEPARATOR + stringResource(R.string.notes_stats_focus_desc, it) } ?: "") +
            (status?.let { SEPARATOR + it } ?: "")

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.border, shape)
                .padding(
                    start = metrics.padSide,
                    end = metrics.padSide,
                    top = metrics.padTop,
                    bottom = metrics.padBottom,
                ).animateContentSize(tween(if (reduceMotion) 0 else RESIZE_MILLIS, easing = MyNoteEasing)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MyNoteTheme.textStyles.overline,
                color = colors.text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).clearAndSetSemantics {},
            )
            MetaButton(
                text = meta,
                height = metrics.metaHeight,
                description = stringResource(R.string.notes_stats_summary_history_desc, meta),
                onClick = onOpenHistory,
            )
        }
        Crossfade(
            targetState = summary,
            animationSpec = tween(if (reduceMotion) 0 else CONTENT_FADE_MILLIS),
            label = "summaryContent",
            modifier =
                Modifier.semantics(mergeDescendants = true) {
                    contentDescription = description
                },
        ) { shown ->
            SummaryBody(shown, metrics, form)
        }
        if (status != null) StatusLine(running = summary.running, status = status)
    }
}

@Composable
private fun StatusLine(
    running: Boolean,
    status: String,
) {
    val colors = MyNoteTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        HorizontalDivider(color = colors.border, modifier = Modifier.padding(top = STATUS_TOP))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = STATUS_INNER),
        ) {
            if (running) {
                RunningRing()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = status,
                style = MyNoteTheme.textStyles.caption.copy(fontSize = 13.sp),
                color = colors.text2,
            )
        }
    }
}

@Composable
private fun SummaryBody(
    summary: SummaryCardUi,
    metrics: SummaryMetrics,
    form: SummaryForm,
) {
    val colors = MyNoteTheme.colors
    val stale = summary.stale
    Column(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
        Text(
            text = summary.headline,
            style =
                MyNoteTheme.textStyles.title.copy(
                    fontSize = (if (stale && form != SummaryForm.Short) STALE_HEADLINE else metrics.headlineSize).sp,
                    fontWeight = if (stale) FontWeight.W600 else FontWeight.W700,
                    lineHeight = 1.4.em,
                ),
            color = if (stale) colors.text2 else colors.text,
            maxLines = metrics.maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = metrics.headlineGap, bottom = metrics.headlineGap),
        )
        Text(
            text = summary.detail,
            style =
                MyNoteTheme.textStyles.body.copy(
                    fontSize = (if (stale) STALE_DETAIL else metrics.detailSize).sp,
                    lineHeight = metrics.detailLine.em,
                ),
            color = if (stale) colors.text2 else colors.bodyText,
            maxLines = metrics.maxLines,
            overflow = TextOverflow.Ellipsis,
        )
        val focus = summary.focus
        if (focus != null && !stale && form != SummaryForm.Short) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = FOCUS_TOP),
            ) {
                Text(
                    text = stringResource(R.string.notes_stats_focus),
                    style = MyNoteTheme.textStyles.caption.copy(fontSize = 12.sp),
                    color = colors.text3,
                )
                Spacer(Modifier.width(8.dp))
                FocusChip(focus)
            }
        }
    }
}

@Composable
internal fun FocusChip(
    name: String,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(7.dp)
    Text(
        text = name,
        style = MyNoteTheme.textStyles.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.W500),
        color = colors.accent,
        maxLines = 1,
        modifier =
            modifier
                .clip(shape)
                .background(colors.accent.copy(alpha = FOCUS_TINT))
                .border(1.dp, colors.accent.copy(alpha = FOCUS_BORDER), shape)
                .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

@Composable
private fun MetaButton(
    text: String,
    height: Dp,
    description: String,
    onClick: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(10.dp)
    val indication = remember(colors, shape) { PressIndication(shape, pressColor = colors.border) }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .offset(x = META_OUTSET)
                .heightIn(min = META_TOUCH)
                .clip(shape)
                .clickable(interactionSource = null, indication = indication, onClick = onClick)
                .semantics {
                    role = Role.Button
                    contentDescription = description
                },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(height).padding(start = 10.dp, end = 6.dp).clearAndSetSemantics {},
        ) {
            Text(
                text = text,
                style = MyNoteTheme.textStyles.monoSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.W400),
                color = colors.text2,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.text2,
                modifier = Modifier.padding(start = 2.dp).size(CHEVRON_SIZE),
            )
        }
    }
}

@Composable
private fun RunningRing() {
    if (rememberReduceMotion()) {
        RingArc(Modifier.graphicsLayer { alpha = REDUCED_ALPHA })
        return
    }
    val transition = rememberInfiniteTransition(label = "summaryRing")
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = FULL_TURN,
        animationSpec = infiniteRepeatable(tween(RING_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "summaryRingTurn",
    )
    RingArc(Modifier.graphicsLayer { rotationZ = turn })
}

@Composable
private fun RingArc(modifier: Modifier = Modifier) {
    val colors = MyNoteTheme.colors
    Canvas(modifier.size(RING)) {
        val stroke = Stroke(width = RING_STROKE.toPx())
        drawArc(color = colors.border, startAngle = 0f, sweepAngle = FULL_TURN, useCenter = false, style = stroke)
        drawArc(color = colors.accent, startAngle = -90f, sweepAngle = RING_SWEEP, useCenter = false, style = stroke)
    }
}

@Composable
@ReadOnlyComposable
internal fun summaryLabel(
    label: SummaryLabel,
    formats: StatsFormats,
): String =
    when (label) {
        SummaryLabel.ThisWeek -> {
            stringResource(R.string.notes_stats_summary_this_week)
        }

        SummaryLabel.LastWeek -> {
            stringResource(R.string.notes_stats_summary_last_week)
        }

        is SummaryLabel.Range -> {
            stringResource(
                R.string.notes_stats_summary_range,
                rangeText(label.start, label.end, formats),
            )
        }
    }

@Composable
@ReadOnlyComposable
internal fun rangeText(
    start: LocalDate,
    end: LocalDate,
    formats: StatsFormats,
): String =
    if (start.month == end.month && start.year == end.year) {
        stringResource(R.string.notes_stats_range_same_month, formats.date(start), end.dayOfMonth)
    } else {
        stringResource(R.string.notes_stats_range, formats.date(start), formats.date(end))
    }

@Composable
@ReadOnlyComposable
internal fun generatedText(
    generated: GeneratedAgo,
    formats: StatsFormats,
): String =
    when (generated) {
        GeneratedAgo.Today -> stringResource(R.string.notes_stats_generated_today)
        GeneratedAgo.Yesterday -> stringResource(R.string.notes_stats_generated_yesterday)
        is GeneratedAgo.Recent -> stringResource(R.string.notes_stats_generated_date, formats.date(generated.date))
        is GeneratedAgo.Stale -> stringResource(R.string.notes_stats_generated_days_ago, generated.days)
    }

private val FOCUS_TOP = 14.dp
private val STATUS_TOP = 14.dp
private val STATUS_INNER = 12.dp
private val META_TOUCH = 44.dp
private val META_OUTSET = 10.dp
private val RING = 14.dp
private val CHEVRON_SIZE = 14.dp
private val RING_STROKE = 2.dp
private const val RING_SWEEP = 100f
private const val FULL_TURN = 360f
private const val RING_MILLIS = 900
private const val REDUCED_ALPHA = 0.5f
private const val STALE_HEADLINE = 17
private const val STALE_DETAIL = 14
private const val FOCUS_TINT = 0.14f
private const val FOCUS_BORDER = 0.26f
private const val ENTER_MILLIS = 240
private const val EXIT_MILLIS = 180
private const val RESIZE_MILLIS = 300
private const val CONTENT_FADE_MILLIS = 200
private const val SEPARATOR = ", "

private fun previewSummary(
    stale: Boolean = false,
    running: Boolean = false,
    failed: Boolean = false,
) = SummaryCardUi(
    label =
        if (stale) {
            SummaryLabel.Range(
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 20),
            )
        } else {
            SummaryLabel.ThisWeek
        },
    generated = if (stale) GeneratedAgo.Stale(12) else GeneratedAgo.Yesterday,
    headline = "android와 kotlin을 고르게 읽었고, cs 노트는 이번 주에 한 번도 열지 않았습니다.",
    detail = "남은 사흘 중 하루는 cs 노트로 시작해 보세요.",
    focus = "cs",
    stale = stale,
    running = running,
    failed = failed,
    focusInLegend = !stale,
)

@Composable
private fun SummaryPreviewFrame(
    form: SummaryForm,
    summary: SummaryCardUi,
) {
    MyNoteTheme(
        windowClass = if (form == SummaryForm.Wide) MyNoteWindowClass.Expanded else MyNoteWindowClass.Compact,
        heightClass = if (form == SummaryForm.Short) MyNoteHeightClass.Compact else MyNoteHeightClass.Medium,
    ) {
        Column(
            Modifier.background(MyNoteTheme.colors.bg).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryCard(summary = summary, form = form, formats = rememberStatsFormats(), onOpenHistory = {})
        }
    }
}

@Preview(name = "요약 카드 · 정상", widthDp = 380)
@Composable
private fun SummaryCardPreview() = SummaryPreviewFrame(SummaryForm.Regular, previewSummary())

@Preview(name = "요약 카드 · 분석 중", widthDp = 380)
@Composable
private fun SummaryCardRunningPreview() = SummaryPreviewFrame(SummaryForm.Regular, previewSummary(running = true))

@Preview(name = "요약 카드 · 실패", widthDp = 380)
@Composable
private fun SummaryCardFailedPreview() = SummaryPreviewFrame(SummaryForm.Regular, previewSummary(failed = true))

@Preview(name = "요약 카드 · 오래됨", widthDp = 380)
@Composable
private fun SummaryCardStalePreview() = SummaryPreviewFrame(SummaryForm.Regular, previewSummary(stale = true))

@Preview(name = "요약 카드 · 높이 Compact", widthDp = 440)
@Composable
private fun SummaryCardShortPreview() = SummaryPreviewFrame(SummaryForm.Short, previewSummary())
