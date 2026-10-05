package com.hancekim.mynote.core.common.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hancekim.mynote.core.common.result.runCatchingCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

abstract class MviViewModel<S : MviState, E : MviEvent> : ViewModel() {
    private val triggers = mutableListOf<Flow<Any?>>()

    val state: StateFlow<S> by lazy {
        val initial = build()
        triggers
            .merge()
            .map { build() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds),
                initialValue = initial,
            )
    }

    protected abstract fun build(): S

    fun onIntent(event: E) {
        runCatchingCancellable { handleIntent(event) }.onFailure { onFailure(it) }
    }

    protected abstract fun handleIntent(event: E)

    protected open fun onFailure(cause: Throwable) = Unit

    protected fun launchSafely(block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.launch { runCatchingCancellable { block() }.onFailure { onFailure(it) } }

    protected fun <T> trigger(initial: T): MutableStateFlow<T> =
        MutableStateFlow(initial).also { triggers += it }

    protected fun <T> Flow<T>.triggerIn(
        initial: T,
        started: SharingStarted = SharingStarted.WhileSubscribed(0),
    ): StateFlow<T> =
        stateIn(
            scope = viewModelScope,
            started = started,
            initialValue = initial,
        ).also { triggers += it }
}
