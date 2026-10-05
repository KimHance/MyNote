package com.hancekim.mynote.core.ai

import android.util.Log
import com.google.ai.edge.litertlm.BenchmarkInfo
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.Locale

internal suspend fun Conversation.sendCancellable(prompt: String): Message =
    cancellable(::cancelProcess) {
        sendMessage(prompt)
    }

internal suspend fun <T> cancellable(
    cancel: () -> Unit,
    call: () -> T,
): T =
    try {
        coroutineScope {
            val running = async { call() }
            try {
                running.await()
            } finally {
                if (!running.isCompleted) {
                    cancel()
                    Log.i(TAG, "cancelProcess · 생성 중 취소")
                }
            }
        }
    } catch (failure: Exception) {
        currentCoroutineContext().ensureActive()
        throw failure
    }

internal fun Message.text(): String = contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }

@OptIn(ExperimentalApi::class)
internal fun Conversation.benchmarkOrNull(): BenchmarkInfo? = runCatching { getBenchmarkInfo() }.getOrNull()

internal fun BenchmarkInfo.backendGuess(): String = if (timeToFirstTokenInSecond < GPU_TTFT_SECONDS) "GPU" else "CPU"

internal fun BenchmarkInfo.describe(): String =
    "출력 ${lastDecodeTokenCount}tok · 입력 ${lastPrefillTokenCount}tok · ttft ${String.format(
        Locale.US,
        "%.2f",
        timeToFirstTokenInSecond,
    )}s · " +
        "prefill ${lastPrefillTokensPerSecond.toInt()}tok/s · decode ${lastDecodeTokensPerSecond.toInt()}tok/s · " +
        "backend=${backendGuess()}"

private const val GPU_TTFT_SECONDS = 3.0
private const val TAG = "MyNoteAi"
