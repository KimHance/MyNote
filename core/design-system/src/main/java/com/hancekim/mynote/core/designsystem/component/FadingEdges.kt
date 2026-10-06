package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.LayoutDirection
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

/**
 * 가로 스크롤 영역에서 가려진 쪽 가장자리를 바탕색으로 덮는다. 스크롤 값은 그리기 단계에서만 읽는다.
 *
 * @param state 이 Modifier 뒤에 거는 `horizontalScroll` 의 상태
 * @param width 덮는 폭
 * @param color 덮는 색 — 영역 뒤 바탕과 같아야 한다
 */
fun Modifier.horizontalScrollFade(
    state: ScrollState,
    width: Dp,
    color: Color,
): Modifier =
    drawWithContent {
        drawContent()
        val fade = minOf(width.toPx(), size.width / 2)
        if (fade <= 0f) return@drawWithContent
        val hiddenStart = state.value > 0
        val hiddenEnd = state.value < state.maxValue
        val rtl = layoutDirection == LayoutDirection.Rtl
        val clear = color.copy(alpha = 0f)
        if (if (rtl) hiddenEnd else hiddenStart) {
            drawRect(
                brush = Brush.horizontalGradient(listOf(color, clear), endX = fade),
                size = Size(fade, size.height),
            )
        }
        if (if (rtl) hiddenStart else hiddenEnd) {
            drawRect(
                brush = Brush.horizontalGradient(listOf(clear, color), startX = size.width - fade, endX = size.width),
                topLeft = Offset(size.width - fade, 0f),
                size = Size(fade, size.height),
            )
        }
    }

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 320)
@Composable
private fun HorizontalScrollFadePreview() {
    MyNoteTheme {
        val state = rememberScrollState()
        Row(
            Modifier
                .background(MyNoteTheme.colors.bg)
                .fillMaxWidth()
                .horizontalScrollFade(state, 24.dp, MyNoteTheme.colors.bg)
                .horizontalScroll(state)
                .padding(16.dp),
        ) {
            Text(
                text = "가로로 넘치는 줄 — 끝 쪽이 바탕색으로 흐려진다. ".repeat(3),
                style = MyNoteTheme.textStyles.body,
                color = MyNoteTheme.colors.text,
                softWrap = false,
            )
        }
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
