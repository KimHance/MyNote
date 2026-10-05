package com.hancekim.mynote.core.designsystem.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class ChartSlice(
    val value: Float,
    val color: Color,
)

@Composable
fun CategoryDonut(
    slices: ImmutableList<ChartSlice>,
    total: String,
    unit: String,
    progress: () -> Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    Box(
        modifier =
            modifier
                .size(DONUT_SIZE)
                .clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = RING_WIDTH.toPx()
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val sum = slices.sumOf { it.value.toDouble() }.toFloat()
            if (sum <= 0f) {
                drawArc(colors.border, 0f, FULL_TURN, false, topLeft, arcSize, style = Stroke(stroke))
                return@Canvas
            }
            val limit = FULL_TURN * ChartMotion.sweep(progress())
            var start = START_ANGLE
            var drawn = 0f
            slices.forEach { slice ->
                if (slice.value <= 0f) return@forEach
                val sweep = slice.value / sum * FULL_TURN
                val visible = minOf(sweep, limit - drawn)
                if (visible > 0f) drawArc(slice.color, start, visible, false, topLeft, arcSize, style = Stroke(stroke))
                start += sweep
                drawn += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = total,
                style =
                    MyNoteTheme.textStyles.cardTitle.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.W700,
                        lineHeight = 22.sp,
                    ),
                color = colors.text,
            )
            Text(
                text = unit,
                style = MyNoteTheme.textStyles.caption.copy(fontSize = 10.sp),
                color = colors.text3,
            )
        }
    }
}

private val DONUT_SIZE = 120.dp
private val RING_WIDTH = 20.dp
private const val FULL_TURN = 360f
private const val START_ANGLE = -90f

@Preview(name = "CategoryDonut · 다섯 조각", showBackground = true, backgroundColor = 0xFF161C24)
@Composable
private fun CategoryDonutPreview() {
    MyNoteTheme {
        val colors = MyNoteTheme.colors
        CategoryDonut(
            slices =
                persistentListOf(
                    ChartSlice(38f, colors.accent),
                    ChartSlice(31f, colors.chartAccent2),
                    ChartSlice(0f, colors.chartAccent3),
                    ChartSlice(14f, colors.text2),
                    ChartSlice(32f, colors.text3),
                ),
            total = "115",
            unit = "분",
            progress = { 1f },
            contentDescription = "",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "CategoryDonut · 0", showBackground = true, backgroundColor = 0xFF161C24)
@Composable
private fun CategoryDonutEmptyPreview() {
    MyNoteTheme {
        CategoryDonut(
            slices = persistentListOf(),
            total = "0",
            unit = "분",
            progress = { 1f },
            contentDescription = "",
            modifier = Modifier.padding(16.dp),
        )
    }
}
