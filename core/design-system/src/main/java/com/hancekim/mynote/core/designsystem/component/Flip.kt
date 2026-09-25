package com.hancekim.mynote.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteEasing
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Stable
class FlipState internal constructor(
    private val turn: State<Float>,
    private val grow: State<Float>,
    private val degrees: Float,
    val transformOrigin: TransformOrigin,
    val cameraDistance: Float,
) {
    val rotationX: () -> Float = { degrees * (turn.value - 1f) }
    val scale: () -> Float = { grow.value }
}

@Composable
fun AnimatedVisibilityScope.rememberFlipState(
    degrees: Float = DEFAULT_FLIP_DEGREES,
    startScale: Float = DEFAULT_FLIP_START_SCALE,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
    cameraDistance: Float = DEFAULT_FLIP_CAMERA_DISTANCE,
    inMillis: Int = DEFAULT_FLIP_IN_MILLIS,
    outMillis: Int = DEFAULT_FLIP_OUT_MILLIS,
    bounce: Float = DEFAULT_FLIP_BOUNCE,
): FlipState {
    val turn =
        transition.animateFloat(
            transitionSpec = {
                if (targetState == EnterExitState.Visible) {
                    tween(inMillis, easing = MyNoteEasing)
                } else {
                    tween(outMillis, easing = MyNoteEasing)
                }
            },
            label = "flipTurn",
        ) { if (it == EnterExitState.Visible) 1f else 0f }

    val grow =
        transition.animateFloat(
            transitionSpec = {
                if (targetState == EnterExitState.Visible) {
                    spring(dampingRatio = bounce, stiffness = Spring.StiffnessMediumLow)
                } else {
                    tween(outMillis, easing = MyNoteEasing)
                }
            },
            label = "flipGrow",
        ) { if (it == EnterExitState.Visible) 1f else startScale }

    return remember(degrees, transformOrigin, cameraDistance) {
        FlipState(turn, grow, degrees, transformOrigin, cameraDistance)
    }
}

fun Modifier.flip(state: FlipState): Modifier =
    graphicsLayer {
        cameraDistance = state.cameraDistance
        transformOrigin = state.transformOrigin
        rotationX = state.rotationX()
        val value = state.scale()
        scaleX = value
        scaleY = value
    }

const val DEFAULT_FLIP_DEGREES = 78f
const val DEFAULT_FLIP_START_SCALE = 0.52f
const val DEFAULT_FLIP_CAMERA_DISTANCE = 16f
const val DEFAULT_FLIP_BOUNCE = 0.68f
const val DEFAULT_FLIP_IN_MILLIS = 460
const val DEFAULT_FLIP_OUT_MILLIS = 260

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 260, heightDp = 140)
@Composable
private fun FlipPreview() {
    MyNoteTheme {
        Box(Modifier.padding(20.dp)) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(DEFAULT_FLIP_IN_MILLIS)),
                exit = fadeOut(tween(DEFAULT_FLIP_OUT_MILLIS)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .flip(rememberFlipState())
                            .background(MyNoteTheme.colors.surface, RoundedCornerShape(20.dp))
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                ) {
                    Text(
                        text = "세워지며 커진다",
                        style = MyNoteTheme.textStyles.body,
                        color = MyNoteTheme.colors.text,
                    )
                }
            }
        }
    }
}
