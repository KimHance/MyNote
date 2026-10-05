package com.hancekim.mynote.core.designsystem.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Composable
fun HBarRow(
    title: String,
    subtitle: String,
    value: String,
    fraction: Float?,
    highlighted: Boolean,
    progress: () -> Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
    index: Int = 0,
) {
    val colors = MyNoteTheme.colors
    val styles = MyNoteTheme.textStyles
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clearAndSetSemantics { this.contentDescription = contentDescription }
                .padding(vertical = ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GAP),
    ) {
        Column(Modifier.width(LABEL_WIDTH)) {
            Text(
                text = title,
                style = styles.caption.copy(fontWeight = if (highlighted) FontWeight.W600 else FontWeight.W400),
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = styles.caption.copy(fontSize = 11.sp),
                color = colors.text3,
                maxLines = 1,
            )
        }
        Canvas(
            Modifier
                .weight(1f)
                .height(BAR_HEIGHT),
        ) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(colors.surface2, cornerRadius = radius)
            val filled = (fraction ?: 0f).coerceIn(0f, 1f) * ChartMotion.bar(progress(), index)
            if (filled > 0f) {
                drawRoundRect(
                    color = if (highlighted) colors.accent else colors.text2,
                    size = Size(size.width * filled, size.height),
                    cornerRadius = radius,
                )
            }
        }
        Text(
            text = value,
            style = styles.monoSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.W400),
            color = colors.bodyText,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(VALUE_WIDTH),
        )
    }
}

private val ROW_PADDING = 6.dp
private val GAP = 12.dp
private val LABEL_WIDTH = 104.dp
private val VALUE_WIDTH = 40.dp
private val BAR_HEIGHT = 8.dp

@Preview(name = "HBarRow · 가장 낮음", showBackground = true, backgroundColor = 0xFF161C24, widthDp = 360)
@Composable
private fun HBarRowLowestPreview() {
    MyNoteTheme {
        HBarRow(
            title = "android",
            subtitle = "6문항",
            value = "50%",
            fraction = 0.5f,
            highlighted = true,
            progress = { 1f },
            contentDescription = "",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Preview(name = "HBarRow · 응시 없음", showBackground = true, backgroundColor = 0xFF161C24, widthDp = 360)
@Composable
private fun HBarRowEmptyPreview() {
    MyNoteTheme {
        HBarRow(
            title = "cs",
            subtitle = "응시 없음",
            value = "—",
            fraction = null,
            highlighted = false,
            progress = { 1f },
            contentDescription = "",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
