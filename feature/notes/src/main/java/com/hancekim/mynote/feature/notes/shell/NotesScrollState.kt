package com.hancekim.mynote.feature.notes.shell

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.block.NoteContentState
import com.hancekim.mynote.core.designsystem.block.rememberNoteContentState

internal enum class ScrollTarget {
    Home,
    Bookmarks,
    Stats,
    StatsHistory,
    Reader,
}

@Stable
internal class NotesScrollState(
    val homeGrid: LazyGridState,
    val bookmarksList: LazyListState,
    val statsList: LazyListState,
    val statsSide: LazyListState,
    val statsHistory: LazyListState,
    val reader: ScrollState,
    val readerContent: NoteContentState,
    private val target: ScrollTarget,
    thresholdPx: Float,
) {
    val showScrollToTop by derivedStateOf {
        when (target) {
            ScrollTarget.Reader -> reader.value > thresholdPx
            ScrollTarget.Bookmarks -> bookmarksList.firstVisibleItemIndex >= ITEM_THRESHOLD
            ScrollTarget.Stats -> statsList.firstVisibleItemIndex >= ITEM_THRESHOLD
            ScrollTarget.StatsHistory -> statsHistory.firstVisibleItemIndex >= ITEM_THRESHOLD
            ScrollTarget.Home -> homeGrid.firstVisibleItemIndex >= ITEM_THRESHOLD
        }
    }

    suspend fun scrollToTop() {
        when (target) {
            ScrollTarget.Reader -> {
                reader.animateScrollTo(0)
            }

            ScrollTarget.Bookmarks -> {
                bookmarksList.animateScrollToItem(0)
            }

            ScrollTarget.Stats -> {
                statsList.animateScrollToItem(0)
                statsSide.animateScrollToItem(0)
            }

            ScrollTarget.StatsHistory -> {
                statsHistory.animateScrollToItem(0)
            }

            ScrollTarget.Home -> {
                homeGrid.animateScrollToItem(0)
            }
        }
    }
}

@Composable
internal fun rememberNotesScrollState(
    activeNoteId: String?,
    isBookmarksTop: Boolean,
    isStatsTop: Boolean,
    isStatsHistoryTop: Boolean,
): NotesScrollState {
    val homeGrid = rememberLazyGridState()
    val bookmarksList = rememberLazyListState()
    val statsList = rememberLazyListState()
    val statsSide = rememberLazyListState()
    val statsHistory = rememberLazyListState()
    val lastNoteId = remember { mutableStateOf(activeNoteId) }
    if (activeNoteId != null) lastNoteId.value = activeNoteId
    val reader = key(lastNoteId.value) { rememberScrollState() }
    val readerContent = key(lastNoteId.value) { rememberNoteContentState() }
    val thresholdPx = with(LocalDensity.current) { READER_THRESHOLD.toPx() }
    val target =
        when {
            activeNoteId != null -> ScrollTarget.Reader
            isBookmarksTop -> ScrollTarget.Bookmarks
            isStatsTop -> ScrollTarget.Stats
            isStatsHistoryTop -> ScrollTarget.StatsHistory
            else -> ScrollTarget.Home
        }
    return remember(
        homeGrid,
        bookmarksList,
        statsList,
        statsSide,
        statsHistory,
        reader,
        readerContent,
        target,
        thresholdPx,
    ) {
        NotesScrollState(
            homeGrid,
            bookmarksList,
            statsList,
            statsSide,
            statsHistory,
            reader,
            readerContent,
            target,
            thresholdPx,
        )
    }
}

private val READER_THRESHOLD = 400.dp
private const val ITEM_THRESHOLD = 3
