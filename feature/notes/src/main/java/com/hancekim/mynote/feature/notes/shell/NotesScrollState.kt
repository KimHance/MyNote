package com.hancekim.mynote.feature.notes.shell

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Stable
internal class NotesScrollState(
    val homeGrid: LazyGridState,
    val reader: ScrollState,
    private val isReader: Boolean,
    thresholdPx: Float,
) {
    val showScrollToTop by derivedStateOf {
        if (isReader) reader.value > thresholdPx else homeGrid.firstVisibleItemIndex >= ITEM_THRESHOLD
    }

    suspend fun scrollToTop() {
        if (isReader) reader.animateScrollTo(0) else homeGrid.animateScrollToItem(0)
    }
}

@Composable
internal fun rememberNotesScrollState(activeNoteId: String?): NotesScrollState {
    val homeGrid = rememberLazyGridState()
    val reader = key(activeNoteId) { rememberScrollState() }
    val thresholdPx = with(LocalDensity.current) { READER_THRESHOLD.toPx() }
    val isReader = activeNoteId != null
    return remember(homeGrid, reader, isReader, thresholdPx) {
        NotesScrollState(homeGrid, reader, isReader, thresholdPx)
    }
}

private val READER_THRESHOLD = 400.dp
private const val ITEM_THRESHOLD = 3
