package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Composable
fun BookmarkRibbon(modifier: Modifier = Modifier) {
    val color = MyNoteTheme.colors.text2
    val path = remember { Path() }
    Canvas(modifier.size(BookmarkRibbonDefaults.Width, BookmarkRibbonDefaults.Height)) {
        val w = size.width
        val h = size.height
        path.rewind()
        path.moveTo(0f, 0f)
        path.lineTo(w, 0f)
        path.lineTo(w, h)
        path.lineTo(w / 2f, h - h * NOTCH_RATIO)
        path.lineTo(0f, h)
        path.close()
        drawPath(path, color)
    }
}

object BookmarkRibbonDefaults {
    val Width: Dp = 12.dp
    val Height: Dp = 17.dp
    val End: Dp = 16.dp
}

private const val NOTCH_RATIO = 4.5f / 17f

@Preview(showBackground = true, backgroundColor = 0xFF0E1217)
@Composable
private fun BookmarkRibbonPreview() {
    MyNoteTheme {
        Box(
            Modifier
                .padding(24.dp)
                .size(120.dp, 60.dp)
                .background(MyNoteTheme.colors.surface),
        ) {
            BookmarkRibbon(Modifier.align(Alignment.TopEnd).padding(end = BookmarkRibbonDefaults.End))
        }
    }
}
