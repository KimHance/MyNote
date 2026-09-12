package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

fun Modifier.fadingEdges(
    top: Dp,
    bottom: Dp,
): Modifier =
    this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val topPx = top.toPx()
            val bottomPx = bottom.toPx()
            if (topPx > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), endY = topPx),
                    size = Size(size.width, topPx),
                    blendMode = BlendMode.DstIn,
                )
            }
            if (bottomPx > 0f) {
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            listOf(Color.Black, Color.Transparent),
                            startY = size.height - bottomPx,
                            endY = size.height,
                        ),
                    topLeft = Offset(0f, size.height - bottomPx),
                    size = Size(size.width, bottomPx),
                    blendMode = BlendMode.DstIn,
                )
            }
        }

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 320, heightDp = 240)
@Composable
private fun FadingEdgesPreview() {
    MyNoteTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(MyNoteTheme.colors.bg),
        ) {
            Column(
                Modifier
                    .fadingEdges(top = 24.dp, bottom = 30.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                repeat(12) { index ->
                    Text(
                        text = "가장자리 페이드 확인용 ${index + 1}",
                        style = MyNoteTheme.textStyles.body,
                        color = MyNoteTheme.colors.text,
                        modifier = Modifier.height(28.dp),
                    )
                }
            }
        }
    }
}
