package com.hancekim.mynote.feature.notes.element

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import com.hancekim.mynote.core.common.mvi.MviState
import com.hancekim.mynote.core.designsystem.theme.MyNoteFontScale
import com.hancekim.mynote.feature.notes.CategoryUi
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.RecentNoteUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

@Stable
data class NotesUiState(
    val categories: ImmutableList<CategoryUi> = persistentListOf(),
    val visibleCategories: ImmutableList<CategoryUi> = persistentListOf(),
    val recentNotes: ImmutableList<RecentNoteUi> = persistentListOf(),
    val activeNoteId: String? = null,
    val query: String = "",
    val expanded: ImmutableMap<String, Boolean> = persistentMapOf(),
    val fontScale: MyNoteFontScale = MyNoteFontScale.Normal,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val errorId: Int = 0,
    val hasToken: Boolean = false,
    val infoMessage: String? = null,
    val infoId: Int = 0,
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
) : MviState {
    val totalCount: Int get() = categories.sumOf { it.notes.size }

    fun isExpanded(categoryName: String): Boolean =
        query.isNotBlank() || expanded[categoryName] == true

    fun noteById(id: String?): NoteUi? {
        if (id == null) return null
        return categories.firstNotNullOfOrNull { c -> c.notes.firstOrNull { it.id == id } }
    }

    fun categoryNameOf(id: String?): String? {
        if (id == null) return null
        return categories.firstOrNull { c -> c.notes.any { it.id == id } }?.name
    }

    val activeNote: NoteUi? get() = noteById(activeNoteId)
}
