package com.hancekim.mynote

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.ViewPropertyAnimator
import android.window.SplashScreenView
import com.hancekim.mynote.core.designsystem.motion.MyNoteExitEasing

/** 아이콘 움직임이 끝나면 스플래시를 떠남 곡선으로 지운다. 기다림과 내려감 길이는 시스템 애니메이터 배율을 따른다. */
internal fun SplashScreenView.fadeOutAfterIcon() {
    val start = iconAnimationStart
    val duration = iconAnimationDuration
    val waitMillis =
        if (start == null || duration == null) {
            0L
        } else {
            val endMillis = start.toEpochMilli() + (duration.toMillis() * animatorScale()).toLong()
            (endMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        }
    postDelayed(::fadeOut, waitMillis)
}

private fun SplashScreenView.animatorScale(): Float =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ValueAnimator.getDurationScale()
    } else {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }

private fun SplashScreenView.fadeOut() {
    val easing = TimeInterpolator(MyNoteExitEasing::transform)
    iconView?.leave(easing)
    leave(easing).withEndAction(::remove)
}

private fun View.leave(easing: TimeInterpolator): ViewPropertyAnimator =
    animate().alpha(0f).setDuration(EXIT_MILLIS).setInterpolator(easing)

private const val EXIT_MILLIS = 180L
