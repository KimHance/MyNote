package com.hancekim.mynote.core.designsystem.font

import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 제목 글꼴을 받아 오는 곳. */
interface TitleFontSource {
    /** 글꼴 제공자가 이 기기에 있는지. */
    suspend fun isAvailable(): Boolean

    /** 받은 글꼴. 못 받으면 예외를 던진다. */
    suspend fun load(): FontFamily
}

/**
 * 제목 글꼴을 정한다. [giveUp] 이 끝나기 전에 받으면 그 글꼴이고, 제공자가 없거나 받지 못하거나 늦으면 기기 글꼴([FontFamily.Default])이다.
 *
 * @param giveUp 더 기다리지 않을 때 돌아오는 대기. 받기와 함께 시작한다.
 */
suspend fun decideTitleFont(
    source: TitleFontSource,
    giveUp: suspend () -> Unit,
): FontFamily =
    coroutineScope {
        val loading = async { loadOrNull(source) }
        val stopping = async { giveUp() }
        val loaded =
            select {
                loading.onAwait { it }
                stopping.onAwait { null }
            }
        loading.cancel()
        stopping.cancel()
        loaded ?: FontFamily.Default
    }

private suspend fun loadOrNull(source: TitleFontSource): FontFamily? =
    try {
        coroutineScope {
            val font = async { source.load() }
            if (source.isAvailable()) {
                font.await()
            } else {
                font.cancel()
                null
            }
        }
    } catch (failure: Exception) {
        currentCoroutineContext().ensureActive()
        null
    }

/**
 * 불 켜짐이 끝날 때까지 남은 시간. 기준은 실행 요청 시각이다 — 프로세스가 이 실행으로 막 떴으면 프로세스 시작, 아니면 화면을 만든 때.
 *
 * @param processStartMillis 프로세스 시작을 요청한 시각(uptime).
 * @param createdMillis 화면을 만든 시각(uptime).
 * @param nowMillis 지금(uptime).
 * @param lampMillis 불 켜짐 길이(애니메이터 배율을 곱한 값).
 */
fun titleFontWaitMillis(
    processStartMillis: Long,
    createdMillis: Long,
    nowMillis: Long,
    lampMillis: Long,
): Long {
    val coldStart = createdMillis - processStartMillis < COLD_START_LIMIT_MILLIS
    val launchedAt = if (coldStart) processStartMillis else createdMillis
    return launchedAt + lampMillis - nowMillis
}

/** 제목 글꼴을 한 번 정해 들고 있는다. 정한 뒤에는 다시 받지 않는다. */
class TitleFontGate {
    private val mutex = Mutex()

    /** 정한 글꼴. 아직 안 정했으면 null. */
    @Volatile
    var fontFamily: FontFamily? = null
        private set

    /** 정한 글꼴을 돌려준다. 처음 부를 때만 [decideTitleFont] 로 정한다. */
    suspend fun decide(
        source: TitleFontSource,
        giveUp: suspend () -> Unit,
    ): FontFamily =
        mutex.withLock {
            fontFamily ?: decideTitleFont(source, giveUp).also { fontFamily = it }
        }
}

private const val COLD_START_LIMIT_MILLIS = 5_000L
