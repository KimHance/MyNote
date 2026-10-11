package com.hancekim.mynote.shell

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.hancekim.mynote.core.ui.shell.NotesScrollState
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.ReaderScrollRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

@Composable
internal fun ReaderProgressEffect(
    noteId: String?,
    reader: ScrollState,
    onIntent: (NotesIntent) -> Unit,
) {
    val currentIntent by rememberUpdatedState(onIntent)
    LaunchedEffect(noteId, reader) {
        if (noteId == null) return@LaunchedEffect
        snapshotFlow { seenPercent(reader.value, reader.maxValue, reader.viewportSize) }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { percent -> currentIntent(NotesIntent.ReaderProgress(noteId, percent)) }
    }
}

@Composable
internal fun ReaderScrollRequestEffect(
    request: ReaderScrollRequest?,
    activeNoteId: String?,
    scroll: NotesScrollState,
) {
    val currentActive by rememberUpdatedState(activeNoteId)
    val currentScroll by rememberUpdatedState(scroll)
    LaunchedEffect(request?.id) {
        val target = request ?: return@LaunchedEffect
        val reader =
            withTimeoutOrNull(MEASURE_WAIT_MILLIS) {
                snapshotFlow {
                    currentScroll.reader.takeIf {
                        currentActive == target.noteId && it.maxValue in 1 until Int.MAX_VALUE
                    }
                }.filterNotNull()
                    .first()
            } ?: return@LaunchedEffect
        val max = reader.maxValue
        val viewport = reader.viewportSize
        reader.scrollTo((target.fraction * (max + viewport) - viewport).roundToInt().coerceIn(0, max))
    }
}

internal fun seenPercent(
    value: Int,
    maxValue: Int,
    viewportSize: Int,
): Int? =
    when (maxValue) {
        Int.MAX_VALUE -> null
        0 -> FULL_PERCENT
        else -> ((value + viewportSize) * 100L / (maxValue + viewportSize)).toInt()
    }

private const val MEASURE_WAIT_MILLIS = 2_000L
private const val FULL_PERCENT = 100
