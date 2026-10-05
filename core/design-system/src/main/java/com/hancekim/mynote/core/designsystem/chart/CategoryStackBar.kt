package com.hancekim.mynote.core.designsystem.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun CategoryStackBar(
    slices: ImmutableList<ChartSlice>,
    progress: () -> Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .clip(RoundedCornerShape(BAR_HEIGHT / 2))
                .clearAndSetSemantics { this.contentDescription = contentDescription },
    ) {
        val visible = slices.filter { it.value > 0f }
        val sum = visible.sumOf { it.value.toDouble() }.toFloat()
        if (sum <= 0f) {
            drawRect(colors.border)
            return@Canvas
        }
        val gap = SLICE_GAP.toPx()
        val usable = size.width - gap * (visible.size - 1)
        clipRect(right = size.width * ChartMotion.sweep(progress())) {
            var x = 0f
            visible.forEach { slice ->
                val width = usable * slice.value / sum
                drawRect(slice.color, topLeft = Offset(x, 0f), size = Size(width, size.height))
                x += width + gap
            }
        }
    }
}

private val BAR_HEIGHT = 12.dp
private val SLICE_GAP = 2.dp

@Preview(name = "CategoryStackBar · 다섯 조각", showBackground = true, backgroundColor = 0xFF161C24, widthDp = 360)
@Composable
private fun CategoryStackBarPreview() {
    MyNoteTheme {
        val colors = MyNoteTheme.colors
        CategoryStackBar(
            slices =
                persistentListOf(
                    ChartSlice(38f, colors.accent),
                    ChartSlice(31f, colors.chartAccent2),
                    ChartSlice(0f, colors.chartAccent3),
                    ChartSlice(14f, colors.text2),
                    ChartSlice(32f, colors.text3),
                ),
            progress = { 1f },
            contentDescription = "",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "CategoryStackBar · 0", showBackground = true, backgroundColor = 0xFF161C24, widthDp = 360)
@Composable
private fun CategoryStackBarEmptyPreview() {
    MyNoteTheme {
        CategoryStackBar(
            slices = persistentListOf(),
            progress = { 1f },
            contentDescription = "",
            modifier = Modifier.padding(16.dp),
        )
    }
}
