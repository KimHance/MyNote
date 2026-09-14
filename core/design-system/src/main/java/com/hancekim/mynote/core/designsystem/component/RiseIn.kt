package com.hancekim.mynote.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import kotlinx.coroutines.launch

val RiseEasing: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

fun Modifier.riseIn(
    delayMillis: Int = 0,
    durationMillis: Int = DEFAULT_RISE_MILLIS,
    distance: Dp = DEFAULT_RISE_DISTANCE,
): Modifier = this then RiseInElement(delayMillis, durationMillis, distance)

private data class RiseInElement(
    val delayMillis: Int,
    val durationMillis: Int,
    val distance: Dp,
) : ModifierNodeElement<RiseInNode>() {
    override fun create() = RiseInNode(delayMillis, durationMillis, distance)

    override fun update(node: RiseInNode) {
        node.distance = distance
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "riseIn"
        properties["delayMillis"] = delayMillis
        properties["durationMillis"] = durationMillis
        properties["distance"] = distance
    }
}

private class RiseInNode(
    private val delayMillis: Int,
    private val durationMillis: Int,
    var distance: Dp,
) : Modifier.Node(),
    LayoutModifierNode {
    private val progress = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis, delayMillis, RiseEasing),
            )
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                val value = progress.value
                alpha = value
                translationY = (1f - value) * distance.toPx()
            }
        }
    }
}

const val DEFAULT_RISE_MILLIS = 300
private val DEFAULT_RISE_DISTANCE = 12.dp

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 320)
@Composable
private fun RiseInPreview() {
    MyNoteTheme {
        val colors = MyNoteTheme.colors
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .riseIn()
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(colors.surface, RoundedCornerShape(14.dp))
                        .padding(16.dp),
            ) {
                Text("떠오르며 나타난다", style = MyNoteTheme.textStyles.body, color = colors.text)
            }
        }
    }
}
