package com.hancekim.mynote.core.designsystem.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class ChartBar(
    val label: String,
    val value: Int,
    val kind: ChartBarKind,
)

@Composable
fun TimeBars(
    bars: ImmutableList<ChartBar>,
    maxBarHeight: Dp,
    progress: () -> Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val styles = MyNoteTheme.textStyles
    val measurer = rememberTextMeasurer()
    val valueStyle = styles.monoSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.W400, color = colors.text2)
    val labelStyle = styles.caption.copy(fontSize = 11.sp, color = colors.text2)
    val currentStyle = labelStyle.copy(fontWeight = FontWeight.W600, color = colors.accent)
    val texts =
        remember(bars, measurer, valueStyle, labelStyle, currentStyle) {
            bars.map { bar ->
                BarTexts(
                    value =
                        if (bar.kind != ChartBarKind.Pending &&
                            bar.value > 0
                        ) {
                            measurer.measure(bar.value.toString(), valueStyle)
                        } else {
                            null
                        },
                    label =
                        measurer.measure(
                            bar.label,
                            if (bar.kind ==
                                ChartBarKind.Current
                            ) {
                                currentStyle
                            } else {
                                labelStyle
                            },
                        ),
                )
            }
        }
    val path = remember { Path() }
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(DASH_ON, DASH_OFF)) }

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .clearAndSetSemantics { this.contentDescription = contentDescription },
    ) {
        if (bars.isEmpty()) return@Canvas
        val gap = BAR_GAP.toPx()
        val cell = (size.width - gap * (bars.size - 1)) / bars.size
        val barWidth = minOf(cell, BAR_MAX_WIDTH.toPx())
        val labelHeight = texts.maxOf { it.label.size.height }.toFloat()
        val baseline = size.height - labelHeight - LABEL_GAP.toPx()
        val maxValue = bars.maxOf { it.value }.coerceAtLeast(1)
        val tall = maxBarHeight.toPx()
        val p = progress()

        bars.forEachIndexed { index, bar ->
            val center = index * (cell + gap) + cell / 2f
            val left = center - barWidth / 2f
            when {
                bar.kind == ChartBarKind.Pending -> {
                    val y = baseline - ZERO_HEIGHT.toPx() / 2f
                    drawLine(
                        color = colors.chartFuture,
                        start = Offset(left, y),
                        end = Offset(left + barWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash,
                    )
                }

                bar.value == 0 -> {
                    drawRect(
                        color = colors.border,
                        topLeft = Offset(left, baseline - ZERO_HEIGHT.toPx()),
                        size = Size(barWidth, ZERO_HEIGHT.toPx()),
                    )
                }

                else -> {
                    val full = maxOf(MIN_BAR.toPx(), bar.value.toFloat() / maxValue * tall)
                    val height = full * ChartMotion.bar(p, index)
                    if (height > 0f) {
                        path.reset()
                        path.addRoundRect(
                            RoundRect(
                                left = left,
                                top = baseline - height,
                                right = left + barWidth,
                                bottom = baseline,
                                topLeftCornerRadius = CornerRadius(TOP_RADIUS.toPx()),
                                topRightCornerRadius = CornerRadius(TOP_RADIUS.toPx()),
                                bottomRightCornerRadius = CornerRadius(BOTTOM_RADIUS.toPx()),
                                bottomLeftCornerRadius = CornerRadius(BOTTOM_RADIUS.toPx()),
                            ),
                        )
                        drawPath(path, if (bar.kind == ChartBarKind.Current) colors.accent else colors.chartAccent2)
                    }
                    texts[index].value?.let { layout ->
                        drawCentered(
                            layout,
                            center,
                            baseline - full - VALUE_GAP.toPx() - layout.size.height,
                            ChartMotion.label(p, index),
                        )
                    }
                }
            }
            drawCentered(texts[index].label, center, size.height - labelHeight, 1f)
        }
    }
}

private fun DrawScope.drawCentered(
    layout: TextLayoutResult,
    centerX: Float,
    top: Float,
    alpha: Float,
) {
    drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, top), alpha = alpha)
}

@Immutable
private class BarTexts(
    val value: TextLayoutResult?,
    val label: TextLayoutResult,
)

private val BAR_GAP = 12.dp
private val BAR_MAX_WIDTH = 28.dp
private val LABEL_GAP = 6.dp
private val VALUE_GAP = 6.dp
private val MIN_BAR = 4.dp
private val ZERO_HEIGHT = 2.dp
private val TOP_RADIUS = 4.dp
private val BOTTOM_RADIUS = 2.dp
private const val DASH_ON = 6f
private const val DASH_OFF = 5f

private val previewWeek =
    persistentListOf(
        ChartBar("월", 35, ChartBarKind.Past),
        ChartBar("화", 12, ChartBarKind.Past),
        ChartBar("수", 48, ChartBarKind.Past),
        ChartBar("목", 20, ChartBarKind.Current),
        ChartBar("금", 0, ChartBarKind.Pending),
        ChartBar("토", 0, ChartBarKind.Pending),
        ChartBar("일", 0, ChartBarKind.Pending),
    )

@Preview(name = "TimeBars · 이번 주", showBackground = true, backgroundColor = 0xFF161C24, widthDp = 360)
@Composable
private fun TimeBarsWeekPreview() {
    MyNoteTheme {
        TimeBars(
            bars = previewWeek,
            maxBarHeight = 70.dp,
            progress = { 1f },
            contentDescription = "",
            modifier = Modifier.padding(16.dp).height(104.dp),
        )
    }
}

@Preview(name = "TimeBars · 0 과 기록 전", showBackground = true, backgroundColor = 0xFF161C24, widthDp = 360)
@Composable
private fun TimeBarsEmptyPreview() {
    MyNoteTheme {
        TimeBars(
            bars =
                persistentListOf(
                    ChartBar("9/1", 0, ChartBarKind.Pending),
                    ChartBar("9/8", 0, ChartBarKind.Pending),
                    ChartBar("9/15", 0, ChartBarKind.Past),
                    ChartBar("9/22", 64, ChartBarKind.Past),
                    ChartBar("9/29", 3, ChartBarKind.Current),
                ),
            maxBarHeight = 70.dp,
            progress = { 1f },
            contentDescription = "",
            modifier = Modifier.padding(16.dp).height(104.dp),
        )
    }
}
