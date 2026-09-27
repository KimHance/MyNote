package com.hancekim.mynote.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.kyant.backdrop.Backdrop

@Composable
fun FloatingStatusPill(
    label: String,
    tone: Color,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    progress: Float? = null,
    icon: ImageVector? = null,
    backdrop: Backdrop? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = MyNoteTheme.colors
    GlassSurface(
        radius = PILL_HEIGHT / 2,
        backdrop = backdrop,
        modifier = modifier.height(PILL_HEIGHT),
        blurRadius = PILL_BLUR,
        tint = colors.glass,
    ) {
        Row(
            modifier =
                Modifier
                    .height(PILL_HEIGHT)
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(role = Role.Button, onClick = onClick)
                        } else {
                            Modifier
                        },
                    ).padding(start = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier.size(RING_SIZE),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tone,
                        modifier = Modifier.size(RING_SIZE - 4.dp),
                    )
                }
            } else {
                StatusRing(progress = progress, tone = tone)
            }
            Text(
                text = label,
                style = MyNoteTheme.textStyles.caption.copy(fontWeight = FontWeight.W500),
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = MyNoteTheme.textStyles.monoSmall,
                    color = colors.text2,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun StatusRing(
    progress: Float?,
    tone: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = RING_SIZE,
) {
    val track = Color.White.copy(alpha = TRACK_ALPHA)
    val spin by rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(SPIN_MILLIS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "spin",
    )
    Canvas(modifier.size(diameter)) {
        val stroke = Stroke(width = RING_STROKE.toPx(), cap = StrokeCap.Round)
        val inset = stroke.width / 2
        val arcSize =
            androidx.compose.ui.geometry
                .Size(size.width - stroke.width, size.height - stroke.width)
        val topLeft =
            androidx.compose.ui.geometry
                .Offset(inset, inset)
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
        drawArc(
            color = tone,
            startAngle = if (progress == null) spin else -90f,
            sweepAngle = if (progress == null) INDETERMINATE_SWEEP else 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
    }
}

private val PILL_HEIGHT = 40.dp
private val PILL_BLUR = 20.dp
private val RING_SIZE = 18.dp
private val RING_STROKE = 2.4.dp
private const val TRACK_ALPHA = 0.12f
private const val SPIN_MILLIS = 1100
private const val INDETERMINATE_SWEEP = 90f

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun FloatingStatusPillPreview() {
    MyNoteTheme {
        val colors = MyNoteTheme.colors
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FloatingStatusPill(label = "퀴즈 모델 받는 중", tone = colors.accent, trailing = "42%", progress = 0.42f)
            FloatingStatusPill(label = "Wi-Fi 기다리는 중", tone = colors.warn, progress = null)
            FloatingStatusPill(label = "퀴즈 모델 준비됨", tone = colors.correct, icon = MyNoteIcons.Check)
            FloatingStatusPill(label = "퀴즈 모델을 받지 못했습니다", tone = colors.wrong, icon = MyNoteIcons.Warn)
        }
    }
}
