package com.hancekim.mynote.core.navigation.motion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.dp
import androidx.navigation3.scene.Scene

internal object SceneMotion {
    const val DURATION_MILLIS: Int = 340

    val Easing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    const val SLIDE_FRACTION: Float = 1.05f

    const val FRONT_SCALE: Float = 0.90f

    const val BACK_SCALE: Float = 0.93f
    const val BACK_ALPHA: Float = 0.5f

    val FrontCornerRadius = 30.dp

    val FrontShadow = 26.dp

    const val BODY_FADE_OUT_MILLIS: Int = 160
    const val BODY_FADE_IN_MILLIS: Int = 200
    val BodyFadeOffset = 10.dp
}

private fun spec() = tween<Float>(SceneMotion.DURATION_MILLIS, easing = SceneMotion.Easing)

private fun intSpec() =
    tween<androidx.compose.ui.unit.IntOffset>(
        SceneMotion.DURATION_MILLIS,
        easing = SceneMotion.Easing,
    )

fun <T : Any> sceneTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
    {
        val enter =
            slideInHorizontally(intSpec()) { width -> (width * SceneMotion.SLIDE_FRACTION).toInt() } +
                scaleIn(spec(), initialScale = SceneMotion.FRONT_SCALE)
        val exit =
            scaleOut(spec(), targetScale = SceneMotion.BACK_SCALE) +
                fadeOut(spec(), targetAlpha = SceneMotion.BACK_ALPHA)
        enter togetherWith exit
    }

fun <T : Any> scenePopTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform =
    {
        val enter =
            scaleIn(spec(), initialScale = SceneMotion.BACK_SCALE) +
                fadeIn(spec(), initialAlpha = SceneMotion.BACK_ALPHA)
        val exit =
            slideOutHorizontally(intSpec()) { width -> (width * SceneMotion.SLIDE_FRACTION).toInt() } +
                scaleOut(spec(), targetScale = SceneMotion.FRONT_SCALE)
        enter togetherWith exit
    }

fun <T : Any> scenePredictivePopTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform =
    { _ -> scenePopTransitionSpec<T>().invoke(this) }
