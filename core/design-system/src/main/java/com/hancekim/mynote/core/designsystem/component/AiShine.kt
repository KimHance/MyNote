package com.hancekim.mynote.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * 둥근 테두리를 따라 도는 빛을 그린다. 매 프레임 다시 그리는 범위가 이 요소에 머물게 자기 레이어에 그린다.
 *
 * @param baseColor 빛 아래 깔리는 테두리 색
 * @param running false 면 빛 없이 테두리만 그린다
 */
fun Modifier.aiShine(
    color: Color,
    baseColor: Color,
    radius: Dp,
    width: Dp = 1.5.dp,
    running: Boolean = true,
): Modifier = this.graphicsLayer {} then AiShineElement(color, baseColor, radius, width, running)

private data class AiShineElement(
    val color: Color,
    val baseColor: Color,
    val radius: Dp,
    val width: Dp,
    val running: Boolean,
) : ModifierNodeElement<AiShineNode>() {
    override fun create() = AiShineNode(color, baseColor, radius, width, running)

    override fun update(node: AiShineNode) {
        node.color = color
        node.baseColor = baseColor
        node.radius = radius
        node.width = width
        node.setRunning(running)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "aiShine"
        properties["color"] = color
        properties["radius"] = radius
        properties["running"] = running
    }
}

private class AiShineNode(
    var color: Color,
    var baseColor: Color,
    var radius: Dp,
    var width: Dp,
    private var running: Boolean,
) : Modifier.Node(),
    DrawModifierNode {
    private val angle = Animatable(0f)
    private var spin: Job? = null

    private var cachedSize = Size.Unspecified
    private var cachedRing: Path? = null
    private var cachedBrush: Brush? = null

    override fun onAttach() = startSpin()

    override fun onDetach() {
        spin?.cancel()
        spin = null
    }

    fun setRunning(value: Boolean) {
        if (running == value) return
        running = value
        if (isAttached) startSpin()
    }

    private fun startSpin() {
        spin?.cancel()
        spin = null
        if (!running) return
        spin =
            coroutineScope.launch {
                angle.snapTo(0f)
                angle.animateTo(
                    targetValue = FULL_TURN,
                    animationSpec = infiniteRepeatable(tween(PERIOD_MILLIS, easing = LinearEasing)),
                )
            }
    }

    override fun ContentDrawScope.draw() {
        val stroke = width.toPx()
        if (size != cachedSize) {
            cachedSize = size
            cachedRing = buildRing(size, radius.toPx(), stroke)
            cachedBrush =
                Brush.sweepGradient(
                    colorStops = SHINE_STOPS.map { (stop, alpha) -> stop to color.copy(alpha = alpha) }.toTypedArray(),
                    center = Offset(size.width / 2f, size.height / 2f),
                )
        }

        drawRoundRect(
            color = baseColor,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(radius.toPx() - stroke / 2f),
            style = Stroke(stroke),
        )

        if (running) {
            val ring = cachedRing
            val brush = cachedBrush
            if (ring != null && brush != null) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val reach = hypot(size.width, size.height)
                clipPath(ring) {
                    rotate(angle.value, center) {
                        drawCircle(brush = brush, radius = reach, center = center)
                    }
                }
            }
        }

        drawContent()
    }

    private fun buildRing(
        size: Size,
        corner: Float,
        stroke: Float,
    ): Path =
        Path().apply {
            fillType = PathFillType.EvenOdd
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    cornerRadius = CornerRadius(corner),
                ),
            )
            addRoundRect(
                RoundRect(
                    rect = Rect(stroke, stroke, size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius((corner - stroke).coerceAtLeast(0f)),
                ),
            )
        }
}

private const val PERIOD_MILLIS = 3200
private const val FULL_TURN = 360f

private val SHINE_STOPS =
    listOf(
        0f to 0f,
        0.06f to 0.35f,
        0.13f to 0.9f,
        0.22f to 0.35f,
        0.34f to 0f,
        0.5f to 0f,
        0.56f to 0.35f,
        0.63f to 0.9f,
        0.72f to 0.35f,
        0.84f to 0f,
        1f to 0f,
    )

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 320, heightDp = 220)
@Composable
private fun AiShinePreview() {
    MyNoteTheme {
        val colors = MyNoteTheme.colors
        Box(Modifier.padding(20.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .aiShine(color = colors.accent, baseColor = colors.border, radius = 18.dp)
                    .padding(1.5.dp)
                    .background(colors.surface, RoundedCornerShape(16.5.dp)),
            )
        }
    }
}
