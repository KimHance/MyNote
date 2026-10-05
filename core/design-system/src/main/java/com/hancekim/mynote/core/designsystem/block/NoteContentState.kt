package com.hancekim.mynote.core.designsystem.block

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Stable
class NoteContentState internal constructor(
    expanded: Set<Int>,
    showingAll: Set<Int>,
) {
    private var expandedBlocks by mutableStateOf(expanded)
    private var showingAllBlocks by mutableStateOf(showingAll)

    internal fun isExpanded(index: Int): Boolean = index in expandedBlocks

    internal fun isShowingAll(index: Int): Boolean = index in showingAllBlocks

    internal fun toggle(index: Int) {
        if (index in expandedBlocks) {
            expandedBlocks = expandedBlocks - index
            showingAllBlocks = showingAllBlocks - index
        } else {
            expandedBlocks = expandedBlocks + index
        }
    }

    internal fun showAll(index: Int) {
        showingAllBlocks = showingAllBlocks + index
    }

    companion object {
        val Saver: Saver<NoteContentState, Any> =
            listSaver(
                save = { listOf(it.expandedBlocks.toIntArray(), it.showingAllBlocks.toIntArray()) },
                restore = { NoteContentState(it[0].toSet(), it[1].toSet()) },
            )
    }
}

@Composable
fun rememberNoteContentState(): NoteContentState =
    rememberSaveable(saver = NoteContentState.Saver) { NoteContentState(emptySet(), emptySet()) }
