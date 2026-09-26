package com.hancekim.mynote.feature.notes.quiz.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.motion.rememberReduceMotion
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Composable
internal fun QuizOrb(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val colors = MyNoteTheme.colors
    val still = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "orb")

    val breathe = transition.pingPong(still, BREATHE_PERIOD, label = "orbBreathe")
    val twinkle = transition.pingPong(still, TWINKLE_PERIOD, label = "orbTwinkle")

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .graphicsLayer {
                    val scale = GLOW_SCALE_MAX - breathe.value * (GLOW_SCALE_MAX - GLOW_SCALE_MIN)
                    scaleX = scale
                    scaleY = scale
                }.drawWithCache {
                    val glow =
                        Brush.radialGradient(
                            colors = listOf(colors.accent.copy(alpha = 0.26f), Color.Transparent),
                            center = Offset(this.size.width / 2f, this.size.height / 2f),
                            radius = this.size.minDimension * 0.68f,
                        )
                    onDrawBehind { drawCircle(glow) }
                },
        )

        Icon(
            imageVector = MyNoteIcons.Sparkle,
            contentDescription = null,
            tint = colors.accent,
            modifier =
                Modifier
                    .size(size * SPARKLE_RATIO)
                    .graphicsLayer {
                        val scale = SCALE_MIN + breathe.value * (SCALE_MAX - SCALE_MIN)
                        scaleX = scale
                        scaleY = scale
                        alpha = ALPHA_MIN + twinkle.value * (ALPHA_MAX - ALPHA_MIN)
                    },
        )
    }
}

@Composable
private fun InfiniteTransition.pingPong(
    still: Boolean,
    periodMillis: Int,
    label: String,
): State<Float> =
    if (still) {
        remember { mutableFloatStateOf(0.5f) }
    } else {
        animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(periodMillis, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = label,
        )
    }

private const val BREATHE_PERIOD = 1100
private const val TWINKLE_PERIOD = 720
private const val SCALE_MIN = 0.86f
private const val SCALE_MAX = 1.14f
private const val GLOW_SCALE_MIN = 0.94f
private const val GLOW_SCALE_MAX = 1.06f
private const val ALPHA_MIN = 0.55f
private const val ALPHA_MAX = 1f
private const val SPARKLE_RATIO = 0.354f

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 160, heightDp = 160)
@Composable
private fun QuizOrbPreview() {
    MyNoteTheme {
        Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) { QuizOrb() }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 120, heightDp = 120)
@Composable
private fun QuizOrbShortPreview() {
    MyNoteTheme {
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) { QuizOrb(size = 64.dp) }
    }
}
