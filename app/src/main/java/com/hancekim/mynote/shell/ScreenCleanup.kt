package com.hancekim.mynote.shell

import com.hancekim.mynote.core.domain.model.ConnectionState

data class ScreenCleanup(
    val resetStack: Boolean,
    val dismissQuiz: Boolean,
)

fun screenCleanupOf(
    previous: ConnectionState?,
    next: ConnectionState,
): ScreenCleanup? {
    if (previous == null || previous == next) return null
    val sameSource =
        previous is ConnectionState.Connected && next is ConnectionState.Connected &&
            previous.sourceId == next.sourceId
    return ScreenCleanup(resetStack = true, dismissQuiz = !sameSource)
}
