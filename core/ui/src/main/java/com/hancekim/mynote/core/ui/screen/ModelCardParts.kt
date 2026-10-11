package com.hancekim.mynote.core.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.ui.element.ModelTone

@Composable
fun ModelTone.color(): Color {
    val colors = MyNoteTheme.colors
    return when (this) {
        ModelTone.Neutral -> colors.text3
        ModelTone.Accent -> colors.accent
        ModelTone.Warn -> colors.warn
        ModelTone.Wrong -> colors.wrong
        ModelTone.Correct -> colors.correct
    }
}

@Composable
fun ModelPreviewFrame(
    largeFont: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    MyNoteTheme {
        val density = LocalDensity.current.density
        val fontScale = if (largeFont) PREVIEW_LARGE_FONT_SCALE else 1f
        CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
            Box(
                modifier
                    .background(MyNoteTheme.colors.bg)
                    .safeDrawingPadding()
                    .width(PREVIEW_WINDOW_WIDTH)
                    .padding(horizontal = MyNoteTheme.adaptive.page, vertical = 16.dp),
            ) {
                content()
            }
        }
    }
}

private val PREVIEW_WINDOW_WIDTH = 360.dp

private const val PREVIEW_LARGE_FONT_SCALE = 1.3f
