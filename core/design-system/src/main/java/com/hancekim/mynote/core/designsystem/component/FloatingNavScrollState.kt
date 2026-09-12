package com.hancekim.mynote.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val ScrollNoiseThreshold = 6.dp
private val AlwaysVisibleZone = 48.dp

@Stable
class FloatingNavScrollState(
    private val noiseThresholdPx: Float,
    private val alwaysVisibleZonePx: Float,
) {
    var hidden by mutableStateOf(false)
        private set

    private var offsetY = 0f
    private var lastY = 0f

    fun onScroll(deltaY: Float) {
        offsetY = (offsetY + deltaY).coerceAtLeast(0f)
        if (abs(offsetY - lastY) < noiseThresholdPx) return

        val next = offsetY > alwaysVisibleZonePx && offsetY > lastY
        lastY = offsetY
        if (next != hidden) hidden = next
    }

    fun reset() {
        offsetY = 0f
        lastY = 0f
        hidden = false
    }

    val nestedScrollConnection: NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                onScroll(-available.y)
                return Offset.Zero
            }
        }
}

@Composable
fun rememberFloatingNavScrollState(): FloatingNavScrollState {
    val density = LocalDensity.current
    return remember(density) {
        with(density) {
            FloatingNavScrollState(
                noiseThresholdPx = ScrollNoiseThreshold.toPx(),
                alwaysVisibleZonePx = AlwaysVisibleZone.toPx(),
            )
        }
    }
}
