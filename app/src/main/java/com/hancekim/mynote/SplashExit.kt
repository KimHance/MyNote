package com.hancekim.mynote

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.ViewPropertyAnimator
import android.window.SplashScreenView
import com.hancekim.mynote.core.designsystem.motion.MyNoteExitEasing

/** 아이콘 움직임이 끝나면 스플래시를 떠남 곡선으로 지운다. 기다림과 내려감 길이는 시스템 애니메이터 배율을 따른다. */
internal fun SplashScreenView.fadeOutAfterIcon() {
    val waitMillis =
        splashWaitMillis(
            startMillis = iconAnimationStart?.toEpochMilli(),
            durationMillis = iconAnimationDuration?.toMillis(),
            scale = context.animatorScale(),
            nowMillis = System.currentTimeMillis(),
        )
    postDelayed(::fadeOut, waitMillis)
}

/**
 * 아이콘 움직임 끝(시작 + 길이 × 배율)까지 남은 시간. 시작이나 길이를 모르거나 이미 지났으면 0 이라 바로 내려간다.
 * 시작과 지금은 같은 벽시계 ms 다 — `iconAnimationStart` 가 [java.time.Instant] 다.
 */
internal fun splashWaitMillis(
    startMillis: Long?,
    durationMillis: Long?,
    scale: Float,
    nowMillis: Long,
): Long {
    if (startMillis == null || durationMillis == null) return 0L
    val endMillis = startMillis + (durationMillis * scale).toLong()
    return (endMillis - nowMillis).coerceAtLeast(0L)
}

/**
 * 살아 있는 [MainActivity] 들의 태스크. 같은 태스크에 아직 끝나지 않은 이전 화면이 있으면(`--activity-clear-task` 웜 시작)
 * 그 화면이 스플래시 도중 끝날 때 시스템이 넘겨준 아이콘 표면을 태스크 단위로 거둬 가, 마크가 사라지고 빈 밤 바탕만 남는다.
 * 그때는 앱이 내리지 않고 시스템이 첫 프레임에 앱 메인 스레드와 상관없이 내리게 둔다. 메인 스레드에서만 부른다.
 */
internal class LiveTasks {
    private val tasks = mutableListOf<Int>()

    /** [taskId] 의 화면이 생겼다고 적고, 같은 태스크에 이전 화면이 없어 앱이 스플래시를 내려도 되면 true 를 준다. */
    fun enter(taskId: Int): Boolean {
        val alone = taskId !in tasks
        tasks += taskId
        return alone
    }

    fun leave(taskId: Int) {
        tasks.remove(taskId)
    }
}

internal fun Context.animatorScale(): Float =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ValueAnimator.getDurationScale()
    } else {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }

private fun SplashScreenView.fadeOut() {
    val easing = TimeInterpolator(MyNoteExitEasing::transform)
    iconView?.leave(easing)
    leave(easing).withEndAction(::remove)
}

private fun View.leave(easing: TimeInterpolator): ViewPropertyAnimator =
    animate().alpha(0f).setDuration(EXIT_MILLIS).setInterpolator(easing)

private const val EXIT_MILLIS = 180L
