package com.hancekim.mynote.core.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class EngineSlot<E : AutoCloseable>(
    private val create: () -> E,
    private val work: CoroutineContext = Dispatchers.Default,
    private val log: (String) -> Unit = { Log.i(TAG, it) },
) {
    private val mutex = Mutex()
    private var engine: E? = null

    val loaded: Boolean get() = engine != null

    suspend fun <T> use(block: suspend (E) -> T): T =
        mutex.withLock {
            val ready = engine ?: createHeld()
            withContext(work) { block(ready) }
        }

    suspend fun <T> tryUse(
        shouldRelease: suspend () -> Boolean,
        block: suspend (E) -> T,
    ): EngineUse<T> {
        if (!mutex.tryLock()) return EngineUse.Busy
        try {
            val existing = engine
            val started = System.currentTimeMillis()
            val ready = existing ?: createHeld()
            val initMillis = if (existing == null) System.currentTimeMillis() - started else null
            var released = false
            val value =
                try {
                    currentCoroutineContext().ensureActive()
                    withContext(work) { block(ready) }
                } finally {
                    if (existing == null) released = withContext(NonCancellable) { closeIfWanted(ready, shouldRelease) }
                }
            return EngineUse.Done(value, initMillis, released)
        } finally {
            mutex.unlock()
        }
    }

    suspend fun release() =
        mutex.withLock {
            val open = engine ?: return@withLock
            engine = null
            withContext(NonCancellable + work) { open.close() }
        }

    private suspend fun createHeld(): E = withContext(NonCancellable + work) { create().also { engine = it } }

    private suspend fun closeIfWanted(
        created: E,
        shouldRelease: suspend () -> Boolean,
    ): Boolean {
        if (!shouldRelease()) {
            log("엔진 유지 · 앱이 포그라운드")
            return false
        }
        engine = null
        withContext(work) { created.close() }
        log("release · 워커가 올린 엔진을 닫음")
        return true
    }

    private companion object {
        const val TAG = "MyNoteAi"
    }
}

sealed interface EngineUse<out T> {
    data object Busy : EngineUse<Nothing>

    data class Done<T>(
        val value: T,
        val initMillis: Long?,
        val released: Boolean,
    ) : EngineUse<T>
}
