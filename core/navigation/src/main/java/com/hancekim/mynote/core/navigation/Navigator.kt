package com.hancekim.mynote.core.navigation

import com.hancekim.mynote.core.common.throttle.Clock
import com.hancekim.mynote.core.common.throttle.SystemClockSource
import com.hancekim.mynote.core.common.throttle.Throttler
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.motion.SceneMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class Navigator(
    private val scope: CoroutineScope,
    root: MyNoteNavKey,
    private val minIntervalMillis: Long = DEFAULT_MIN_INTERVAL_MILLIS,
    clock: Clock = SystemClockSource,
) {
    private val throttle = Throttler(minIntervalMillis, clock)

    private val mutableStack = MutableStateFlow(listOf(root))

    val stack: StateFlow<List<MyNoteNavKey>> = mutableStack.asStateFlow()

    private val mutableSettled = MutableStateFlow(true)

    val isSettled: StateFlow<Boolean> = mutableSettled.asStateFlow()

    private var settleJob: Job? = null

    fun dispatch(command: NavCommand): NavResult {
        if (!throttle.isIdle()) return NavResult.Throttled
        val next = reduce(mutableStack.value, command) ?: return NavResult.Ignored
        apply(next)
        return NavResult.Applied
    }

    fun dispatchImmediately(command: NavCommand) {
        val next = reduce(mutableStack.value, command) ?: return
        apply(next)
    }

    fun push(key: MyNoteNavKey): NavResult = dispatch(NavCommand.Push(key))

    fun pop(): NavResult = dispatch(NavCommand.Pop)

    private fun reduce(
        current: List<MyNoteNavKey>,
        command: NavCommand,
    ): List<MyNoteNavKey>? =
        when (command) {
            is NavCommand.Push -> {
                if (current.lastOrNull() == command.key) null else current + command.key
            }

            NavCommand.Pop -> {
                if (current.size <= 1) null else current.dropLast(1)
            }

            is NavCommand.ReplaceAll -> {
                if (command.keys.isEmpty() || command.keys == current) null else command.keys
            }
        }

    private fun apply(next: List<MyNoteNavKey>) {
        mutableStack.value = next
        throttle.tryAccept()
        mutableSettled.value = false
        settleJob?.cancel()
        settleJob =
            scope.launch {
                delay(minIntervalMillis.milliseconds)
                mutableSettled.value = true
            }
    }
}

private val DEFAULT_MIN_INTERVAL_MILLIS: Long =
    maxOf(
        SceneMotion.DURATION_MILLIS,
        SceneMotion.BODY_FADE_OUT_MILLIS + SceneMotion.BODY_FADE_IN_MILLIS,
    ).toLong()
