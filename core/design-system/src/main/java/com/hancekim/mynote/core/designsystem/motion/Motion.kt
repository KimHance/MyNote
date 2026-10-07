package com.hancekim.mynote.core.designsystem.motion

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

val MyNoteEasing: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

val MyNoteExitEasing: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

@Composable
fun rememberReduceMotion(): Boolean {
    val inspecting = LocalInspectionMode.current
    val context = LocalContext.current
    return remember(inspecting, context) {
        inspecting ||
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
    }
}
