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

    private var leaveGuard: LeaveGuard? = null

    /**
     * 사용자 이동을 막을 지킴을 건다(한 칸 — 새로 걸면 갈아 끼운다). [dispatch] 만 지나고 [dispatchImmediately] 는 지나지 않는다.
     * 지금 쓰는 곳은 편집기의 나가기 확인 하나다(local-notes LN-10).
     */
    fun setLeaveGuard(guard: LeaveGuard) {
        leaveGuard = guard
    }

    /** [guard] 가 지금 걸린 지킴일 때만 푼다 — 늦게 치워지는 옛 ViewModel 이 새 지킴을 지우지 않게. */
    fun clearLeaveGuard(guard: LeaveGuard) {
        if (leaveGuard === guard) leaveGuard = null
    }

    fun dispatch(command: NavCommand): NavResult {
        if (!throttle.isIdle()) return NavResult.Throttled
        val current = mutableStack.value
        val next = reduce(current, command) ?: return NavResult.Ignored
        if (leaveGuard?.blocks(current, next) == true) return NavResult.Guarded
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

/**
 * 사용자 이동을 막는 지킴. 막으면 true — 스택은 그대로이고 지킴이 [next] 를 기억했다가 사용자에게 묻는다.
 * 막지 않는 이동은 창을 열지 않은 채 그냥 지나가므로 지킴은 순수 판정 + 기억만 한다.
 */
fun interface LeaveGuard {
    fun blocks(
        current: List<MyNoteNavKey>,
        next: List<MyNoteNavKey>,
    ): Boolean
}

private val DEFAULT_MIN_INTERVAL_MILLIS: Long =
    maxOf(
        SceneMotion.DURATION_MILLIS,
        SceneMotion.BODY_FADE_OUT_MILLIS + SceneMotion.BODY_FADE_IN_MILLIS,
    ).toLong()
