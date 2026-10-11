package com.hancekim.mynote.core.common.throttle

import android.os.SystemClock

fun interface Clock {
    fun elapsedMillis(): Long
}

val SystemClockSource: Clock = Clock { SystemClock.elapsedRealtime() }

// 기록에 남는 시각(에포크 밀리초). 간격 재기는 시계를 돌려도 뒤틀리지 않는 Clock 으로 한다
fun interface WallClock {
    fun nowMillis(): Long
}

val SystemWallClockSource: WallClock = WallClock { System.currentTimeMillis() }
