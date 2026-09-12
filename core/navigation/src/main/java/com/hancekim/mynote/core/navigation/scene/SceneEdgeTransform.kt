package com.hancekim.mynote.core.navigation.scene

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import com.hancekim.mynote.core.navigation.motion.SceneMotion
import com.hancekim.mynote.core.navigation.shared.LocalNavAnimatedScope

private val NoSceneEdge: () -> Float = { 0f }

@Composable
fun rememberSceneEdgeProgress(): () -> Float {
    val scope = LocalNavAnimatedScope.current ?: return NoSceneEdge

    val entered by scope.transition.animateFloat(
        transitionSpec = { tween(SceneMotion.DURATION_MILLIS, easing = SceneMotion.Easing) },
        label = "sceneEdge",
    ) { state -> if (state == EnterExitState.Visible) 1f else 0f }

    return remember { { 1f - entered } }
}

fun Modifier.sceneEdge(progress: () -> Float): Modifier = this then SceneEdgeElement(progress)

private data class SceneEdgeElement(
    val progress: () -> Float,
) : ModifierNodeElement<SceneEdgeNode>() {
    override fun create(): SceneEdgeNode = SceneEdgeNode(progress)

    override fun update(node: SceneEdgeNode) {
        node.progress = progress
        node.invalidatePlacement()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "sceneEdge"
        properties["progress"] = progress
    }
}

private class SceneEdgeNode(
    var progress: () -> Float,
) : LayoutModifierNode,
    Modifier.Node() {
    override val shouldAutoInvalidate: Boolean get() = false

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val k = progress()
        shape = RoundedCornerShape(SceneMotion.FrontCornerRadius * k)
        clip = k > 0.001f
        shadowElevation = SceneMotion.FrontShadow.toPx() * k
        spotShadowColor = Color.Black
        ambientShadowColor = Color.Black
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }
}
