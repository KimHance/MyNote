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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class DivergingBar(
    val label: String,
    val up: Int,
    val down: Int,
    val kind: ChartBarKind,
)

@Composable
fun DivergingBars(
    bars: ImmutableList<DivergingBar>,
    progress: () -> Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = MyNoteTheme.textStyles.caption.copy(fontSize = 11.sp, color = colors.text2)
    val currentStyle = labelStyle.copy(fontWeight = FontWeight.W600, color = colors.accent)
    val labels =
        remember(bars, measurer, labelStyle, currentStyle) {
            bars.map { measurer.measure(it.label, if (it.kind == ChartBarKind.Current) currentStyle else labelStyle) }
        }
    val path = remember { Path() }
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(DASH_ON, DASH_OFF)) }

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(UP_AREA + DOWN_AREA + LABEL_GAP + LABEL_RESERVE)
                .clearAndSetSemantics { this.contentDescription = contentDescription },
    ) {
        if (bars.isEmpty()) return@Canvas
        val gap = BAR_GAP.toPx()
        val cell = (size.width - gap * (bars.size - 1)) / bars.size
        val barWidth = minOf(cell, BAR_MAX_WIDTH.toPx())
        val baseline = UP_AREA.toPx()
        val maxUp = bars.maxOf { it.up }.coerceAtLeast(1)
        val maxDown = bars.maxOf { it.down }.coerceAtLeast(1)
        val unit = minOf(UNIT.toPx(), UP_AREA.toPx() / maxUp, DOWN_AREA.toPx() / maxDown)
        val radius = CornerRadius(BAR_RADIUS.toPx())
        val p = progress()

        bars.forEachIndexed { index, bar ->
            val cellLeft = index * (cell + gap)
            val center = cellLeft + cell / 2f
            val left = center - barWidth / 2f
            val grow = ChartMotion.bar(p, index)
            drawLine(
                color = if (bar.kind == ChartBarKind.Pending) colors.chartFuture else colors.border,
                start = Offset(cellLeft, baseline),
                end = Offset(cellLeft + cell, baseline),
                strokeWidth = 1.dp.toPx(),
                pathEffect = if (bar.kind == ChartBarKind.Pending) dash else null,
            )
            val up = bar.up * unit * grow
            if (up > 0f) {
                path.reset()
                path.addRoundRect(
                    RoundRect(
                        left = left,
                        top = baseline - up,
                        right = left + barWidth,
                        bottom = baseline,
                        topLeftCornerRadius = radius,
                        topRightCornerRadius = radius,
                        bottomRightCornerRadius = CornerRadius.Zero,
                        bottomLeftCornerRadius = CornerRadius.Zero,
                    ),
                )
                drawPath(path, colors.accent)
            }
            val down = bar.down * unit * grow
            if (down > 0f) {
                val top = baseline + 1.dp.toPx()
                path.reset()
                path.addRoundRect(
                    RoundRect(
                        left = left,
                        top = top,
                        right = left + barWidth,
                        bottom = top + down,
                        topLeftCornerRadius = CornerRadius.Zero,
                        topRightCornerRadius = CornerRadius.Zero,
                        bottomRightCornerRadius = radius,
                        bottomLeftCornerRadius = radius,
                    ),
                )
                drawPath(path, colors.text3)
            }
            val label = labels[index]
            drawText(
                label,
                topLeft = Offset(center - label.size.width / 2f, (UP_AREA + DOWN_AREA + LABEL_GAP).toPx()),
            )
        }
    }
}

private val UP_AREA = 44.dp
private val DOWN_AREA = 28.dp
private val LABEL_GAP = 6.dp
private val LABEL_RESERVE = 16.dp
private val BAR_GAP = 12.dp
private val BAR_MAX_WIDTH = 22.dp
private val UNIT = 18.dp
private val BAR_RADIUS = 3.dp
private const val DASH_ON = 6f
private const val DASH_OFF = 5f

@Preview(name = "DivergingBars · 이번 주", showBackground = true, backgroundColor = 0xFF161C24, widthDp = 360)
@Composable
private fun DivergingBarsPreview() {
    MyNoteTheme {
        DivergingBars(
            bars =
                persistentListOf(
                    DivergingBar("월", 2, 0, ChartBarKind.Past),
                    DivergingBar("화", 0, 0, ChartBarKind.Past),
                    DivergingBar("수", 1, 1, ChartBarKind.Past),
                    DivergingBar("목", 1, 0, ChartBarKind.Current),
                    DivergingBar("금", 0, 0, ChartBarKind.Pending),
                    DivergingBar("토", 0, 0, ChartBarKind.Pending),
                    DivergingBar("일", 0, 0, ChartBarKind.Pending),
                ),
            progress = { 1f },
            contentDescription = "",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "DivergingBars · 넘쳐서 축소 + 기록 전", showBackground = true, backgroundColor = 0xFF161C24, widthDp = 360)
@Composable
private fun DivergingBarsOverflowPreview() {
    MyNoteTheme {
        DivergingBars(
            bars =
                persistentListOf(
                    DivergingBar("9/1", 0, 0, ChartBarKind.Pending),
                    DivergingBar("9/8", 0, 0, ChartBarKind.Pending),
                    DivergingBar("9/15", 6, 0, ChartBarKind.Past),
                    DivergingBar("9/22", 2, 5, ChartBarKind.Past),
                    DivergingBar("9/29", 1, 0, ChartBarKind.Current),
                ),
            progress = { 1f },
            contentDescription = "",
            modifier = Modifier.padding(16.dp),
        )
    }
}
