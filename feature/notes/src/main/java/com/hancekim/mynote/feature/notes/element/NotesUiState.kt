package com.hancekim.mynote.feature.notes.element

import androidx.compose.runtime.Stable
import com.hancekim.mynote.core.common.mvi.MviState
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.ui.element.NoticeUi
import com.hancekim.mynote.feature.notes.BookmarkUi
import com.hancekim.mynote.feature.notes.CategoryUi
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.RecentNoteUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

@Stable
data class ReaderScrollRequest(
    val noteId: String,
    val fraction: Float,
    val id: Int,
)

@Stable
data class NotesUiState(
    val categories: ImmutableList<CategoryUi> = persistentListOf(),
    val visibleCategories: ImmutableList<CategoryUi> = persistentListOf(),
    val recentNotes: ImmutableList<RecentNoteUi> = persistentListOf(),
    val bookmarkedIds: ImmutableSet<String> = persistentSetOf(),
    val bookmarks: ImmutableList<BookmarkUi> = persistentListOf(),
    val query: String = "",
    val expanded: ImmutableMap<String, Boolean> = persistentMapOf(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val errorId: Int = 0,
    val connection: ConnectionState? = null,
    val notesLoaded: Boolean = false,
    val awaitingNotes: Boolean = false,
    val notice: NoticeUi<NotesIntent>? = null,
    val infoMessage: String? = null,
    val infoId: Int = 0,
    val readerScrollRequest: ReaderScrollRequest? = null,
) : MviState {
    val connectedRepo: RepoConnection? get() = (connection as? ConnectionState.Connected)?.connection

    val heroRepoName: String? get() = connectedRepo?.fullName

    val homeBody: HomeBody get() = homeBodyOf(connectedRepo != null, totalCount, isLoading || awaitingNotes)

    val showConnectBand: Boolean get() = connection == ConnectionState.None && totalCount > 0

    val totalCount: Int get() = categories.sumOf { it.notes.size }

    fun isExpanded(categoryName: String): Boolean =
        query.isNotBlank() || expanded[categoryName] == true

    fun isBookmarked(id: String?): Boolean = id != null && id in bookmarkedIds

    fun noteById(id: String?): NoteUi? {
        if (id == null) return null
        return categories.firstNotNullOfOrNull { c -> c.notes.firstOrNull { it.id == id } }
    }

    fun categoryNameOf(id: String?): String? {
        if (id == null) return null
        return categories.firstOrNull { c -> c.notes.any { it.id == id } }?.name
    }
}
