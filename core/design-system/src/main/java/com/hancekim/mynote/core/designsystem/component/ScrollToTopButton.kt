package com.hancekim.mynote.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.kyant.backdrop.Backdrop

object ScrollToTopDefaults {
    val Size: Dp = 46.dp
}

private const val APPEAR_MILLIS = 220
private const val DISAPPEAR_MILLIS = 160
private val ButtonRadius = 23.dp

@Composable
fun ScrollToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            scaleIn(tween(APPEAR_MILLIS, easing = FastOutSlowInEasing), initialScale = 0.7f) +
                fadeIn(tween(APPEAR_MILLIS, easing = FastOutSlowInEasing)),
        exit =
            scaleOut(tween(DISAPPEAR_MILLIS, easing = FastOutSlowInEasing), targetScale = 0.7f) +
                fadeOut(tween(DISAPPEAR_MILLIS, easing = FastOutSlowInEasing)),
    ) {
        val colors = MyNoteTheme.colors
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()

        GlassSurface(radius = ButtonRadius, backdrop = backdrop) {
            Box(
                modifier =
                    Modifier
                        .size(ScrollToTopDefaults.Size)
                        .clip(RoundedCornerShape(ButtonRadius))
                        .background(if (pressed) colors.navItemPressed else Color.Transparent)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = "맨 위로",
                            onClick = onClick,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MyNoteIcons.ArrowUp,
                    contentDescription = "맨 위로",
                    tint = colors.text2,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 140, heightDp = 120)
@Composable
private fun ScrollToTopButtonPreview() {
    MyNoteTheme {
        Box(Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            ScrollToTopButton(visible = true, onClick = {})
        }
    }
}
