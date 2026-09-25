package com.hancekim.mynote.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import kotlinx.coroutines.launch

@Immutable
class PressIndication(
    private val shape: Shape = RectangleShape,
    private val color: Color = Color.Unspecified,
    private val pressColor: Color = Color.Unspecified,
    private val scale: Float = 1f,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressIndicationNode(interactionSource, shape, color, pressColor, scale)

    override fun equals(other: Any?): Boolean =
        other is PressIndication &&
            other.shape == shape &&
            other.color == color &&
            other.pressColor == pressColor &&
            other.scale == scale

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + pressColor.hashCode()
        result = 31 * result + scale.hashCode()
        return result
    }
}

private class PressIndicationNode(
    private val interactionSource: InteractionSource,
    private val shape: Shape,
    private val color: Color,
    private val pressColor: Color,
    private val scale: Float,
) : Modifier.Node(),
    DrawModifierNode {
    private val progress = Animatable(0f)
    private var outline: Outline? = null
    private var outlineSize = androidx.compose.ui.geometry.Size.Unspecified

    override fun onAttach() {
        coroutineScope.launch {
            var presses = 0
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses++
                    is PressInteraction.Release, is PressInteraction.Cancel -> presses--
                    else -> return@collect
                }
                val target = if (presses > 0) 1f else 0f
                if (progress.targetValue != target) {
                    if (target == 1f) {
                        progress.animateTo(target, tween(PRESS_IN_MILLIS))
                    } else {
                        progress.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val amount = progress.value
        val factor = 1f - (1f - scale) * amount
        if (factor == 1f) {
            drawSurface(amount)
            drawContent()
        } else {
            scale(factor, factor) {
                this@draw.drawSurface(amount)
                this@draw.drawContent()
            }
        }
    }

    private fun ContentDrawScope.drawSurface(amount: Float) {
        if (!color.isSpecified && !pressColor.isSpecified) return
        val base = if (color.isSpecified) color else Color.Transparent
        val target = if (pressColor.isSpecified) pressColor else base
        val fill = lerp(base, target, amount)
        if (fill.alpha == 0f) return

        if (size != outlineSize) {
            outlineSize = size
            outline = shape.createOutline(size, layoutDirection, this)
        }
        outline?.let { drawOutline(it, fill) }
    }
}

private const val PRESS_IN_MILLIS = 90

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 320)
@Composable
private fun PressIndicationPreview() {
    MyNoteTheme {
        val colors = MyNoteTheme.colors
        val shape = RoundedCornerShape(15.dp)
        val interaction = remember { MutableInteractionSource() }
        val filled =
            remember(colors, shape) {
                PressIndication(shape, colors.accent, colors.accentPressed, scale = 0.98f)
            }
        val outlined = remember(colors, shape) { PressIndication(shape, pressColor = colors.surface) }
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable(interactionSource = interaction, indication = filled, role = Role.Button) {},
                contentAlignment = Alignment.Center,
            ) {
                Text("면까지 같이 줄어든다", style = MyNoteTheme.textStyles.body, color = colors.bg)
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable(interactionSource = null, indication = outlined, role = Role.Button) {}
                        .border(1.dp, colors.border, shape),
                contentAlignment = Alignment.Center,
            ) {
                Text("테두리형", style = MyNoteTheme.textStyles.body, color = colors.text)
            }
        }
    }
}
