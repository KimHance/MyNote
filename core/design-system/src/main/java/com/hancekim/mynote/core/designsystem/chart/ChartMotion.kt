package com.hancekim.mynote.core.designsystem.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.hancekim.mynote.core.designsystem.motion.MyNoteEasing
import com.hancekim.mynote.core.designsystem.motion.rememberReduceMotion

enum class ChartBarKind {
    Past,
    Current,
    Pending,
}

@Composable
fun rememberChartProgress(key: Any?): () -> Float {
    val reduceMotion = rememberReduceMotion()
    val progress = remember(key, reduceMotion) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(progress) {
        if (progress.value < 1f) progress.animateTo(1f, tween(ChartMotion.TOTAL_MILLIS, easing = LinearEasing))
    }
    return remember(progress) { { progress.value } }
}

internal object ChartMotion {
    private const val BAR_MILLIS = 300f
    private const val STAGGER_MILLIS = 20f
    private const val MAX_STAGGER_MILLIS = 120f
    private const val LABEL_MILLIS = 120f
    const val TOTAL_MILLIS = (MAX_STAGGER_MILLIS + BAR_MILLIS + LABEL_MILLIS).toInt()

    fun bar(
        progress: Float,
        index: Int,
    ): Float {
        val local = (elapsed(progress) - delay(index)) / BAR_MILLIS
        return MyNoteEasing.transform(local.coerceIn(0f, 1f))
    }

    fun label(
        progress: Float,
        index: Int,
    ): Float = ((elapsed(progress) - delay(index) - BAR_MILLIS) / LABEL_MILLIS).coerceIn(0f, 1f)

    fun sweep(progress: Float): Float = MyNoteEasing.transform((elapsed(progress) / BAR_MILLIS).coerceIn(0f, 1f))

    private fun elapsed(progress: Float): Float = progress * TOTAL_MILLIS

    private fun delay(index: Int): Float = (index * STAGGER_MILLIS).coerceAtMost(MAX_STAGGER_MILLIS)
}
