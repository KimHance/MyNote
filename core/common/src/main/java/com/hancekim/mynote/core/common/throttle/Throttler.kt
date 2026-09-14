package com.hancekim.mynote.core.common.throttle

import android.os.SystemClock

fun interface Clock {
    fun elapsedMillis(): Long
}

val SystemClockSource: Clock = Clock { SystemClock.elapsedRealtime() }

class Throttler(
    private val minIntervalMillis: Long = DEFAULT_MIN_INTERVAL_MILLIS,
    private val clock: Clock = SystemClockSource,
) {
    private var lastAcceptedAt = Long.MIN_VALUE

    fun isIdle(): Boolean =
        lastAcceptedAt == Long.MIN_VALUE || clock.elapsedMillis() - lastAcceptedAt >= minIntervalMillis

    fun tryAccept(): Boolean {
        if (!isIdle()) return false
        lastAcceptedAt = clock.elapsedMillis()
        return true
    }

    fun process(action: () -> Unit): Boolean {
        if (!tryAccept()) return false
        action()
        return true
    }

    fun reset() {
        lastAcceptedAt = Long.MIN_VALUE
    }

    companion object {
        const val DEFAULT_MIN_INTERVAL_MILLIS = 500L
    }
}
