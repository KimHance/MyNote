package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

@Composable
internal fun GlassSurface(
    radius: Dp,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(radius)

    val glass =
        if (backdrop != null) {
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    blur(GLASS_BLUR_RADIUS.toPx())
                    lens(
                        refractionHeight = radius.toPx(),
                        refractionAmount = (radius * LENS_AMOUNT_RATIO).toPx(),
                    )
                },
                highlight = { Highlight.Default },
                shadow = { GlassShadow },
                onDrawSurface = { drawRect(colors.glass) },
            )
        } else {
            Modifier
                .clip(shape)
                .background(colors.glassOpaque)
                .border(1.dp, colors.glassBorder, shape)
        }

    Box(modifier = modifier.then(glass)) {
        content()
    }
}

private val GLASS_BLUR_RADIUS = 8.dp

private const val LENS_AMOUNT_RATIO = 2f

private val GlassShadow =
    Shadow(
        radius = 20.dp,
        offset = DpOffset(0.dp, 14.dp),
        color = Color.Black.copy(alpha = 0.45f),
    )

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 220, heightDp = 120)
@Composable
private fun GlassSurfacePreview() {
    MyNoteTheme {
        Box(Modifier.padding(24.dp)) {
            GlassSurface(radius = 24.dp, backdrop = null) {
                Text(
                    text = "유리 표면",
                    style = MyNoteTheme.textStyles.body,
                    color = MyNoteTheme.colors.text,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                )
            }
        }
    }
}
