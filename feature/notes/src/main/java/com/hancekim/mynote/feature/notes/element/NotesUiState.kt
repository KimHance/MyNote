package com.hancekim.mynote.feature.notes.element

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.hancekim.mynote.core.common.mvi.MviState
import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.feature.notes.BookmarkUi
import com.hancekim.mynote.feature.notes.CategoryUi
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.RecentNoteUi
import com.hancekim.mynote.feature.notes.quiz.QuizUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

@Stable
data class NoticeUi(
    val message: String,
    val undo: NotesIntent?,
    val id: Int,
)

@Stable
data class ReaderScrollRequest(
    val noteId: String,
    val fraction: Float,
    val id: Int,
)

@Immutable
data class DisconnectDialogUi(
    val notes: Int,
    val bookmarks: Int,
)

@Stable
data class NotesUiState(
    val categories: ImmutableList<CategoryUi> = persistentListOf(),
    val visibleCategories: ImmutableList<CategoryUi> = persistentListOf(),
    val recentNotes: ImmutableList<RecentNoteUi> = persistentListOf(),
    val bookmarkedIds: ImmutableSet<String> = persistentSetOf(),
    val bookmarks: ImmutableList<BookmarkUi> = persistentListOf(),
    val backStack: ImmutableList<MyNoteNavKey> = persistentListOf(NotesNavKey.Home),
    val activeNoteId: String? = null,
    val isNavSettled: Boolean = true,
    val exitHintId: Int = 0,
    val exitRequestId: Int = 0,
    val query: String = "",
    val expanded: ImmutableMap<String, Boolean> = persistentMapOf(),
    val fontScale: MyNoteFontScale = MyNoteFontScale.Normal,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val errorId: Int = 0,
    val hasToken: Boolean = false,
    val maskedToken: String? = null,
    val tokenEdit: TokenEditState = TokenEditState.Viewing,
    val connection: ConnectionState? = null,
    val notesLoaded: Boolean = false,
    val seedPending: Boolean = true,
    val lastSync: LastSync = LastSync.None,
    val disconnectDialog: DisconnectDialogUi? = null,
    val awaitingNotes: Boolean = false,
    val notice: NoticeUi? = null,
    val infoMessage: String? = null,
    val infoId: Int = 0,
    val modelStatus: ModelStatus = ModelStatus.Missing,
    val notificationsEnabled: Boolean = true,
    val quiz: QuizUiState = QuizUiState.Idle,
    val quizUndoMessage: String? = null,
    val quizUndoId: Int = 0,
    val studyLogEnabled: Boolean = true,
    val readerScrollRequest: ReaderScrollRequest? = null,
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
) : MviState {
    val canPop: Boolean get() = backStack.size > 1

    val isQuizAvailable: Boolean get() = modelStatus == ModelStatus.Ready

    val isSettingsOpen: Boolean get() = backStack.lastOrNull() is NotesNavKey.Settings

    val isConnectOpen: Boolean get() = backStack.lastOrNull() is NotesNavKey.Connect

    val tokenEditing: Boolean get() = tokenEdit.editing

    val connectedRepo: RepoConnection? get() = (connection as? ConnectionState.Connected)?.connection

    val start: StartDestination
        get() =
            startDestination(
                connection =
                    when (connection) {
                        null -> ConnectionKnown.Unknown
                        ConnectionState.None -> ConnectionKnown.None
                        is ConnectionState.Connected -> ConnectionKnown.Connected
                    },
                notes =
                    when {
                        !notesLoaded -> NotesKnown.Unknown
                        totalCount == 0 -> NotesKnown.Empty
                        else -> NotesKnown.Some
                    },
                seedPending = seedPending,
            )

    val repoCard: RepoCardUi get() = repoCardUi(connectedRepo, hasToken, lastSync)

    val homeBody: HomeBody get() = homeBodyOf(connectedRepo != null, totalCount, isLoading || awaitingNotes)

    val showConnectBand: Boolean get() = connection == ConnectionState.None && totalCount > 0

    val isBookmarksTop: Boolean get() = backStack.lastOrNull() is NotesNavKey.Bookmarks

    val isStatsTop: Boolean get() = backStack.lastOrNull() is NotesNavKey.Stats

    val isStatsHistoryTop: Boolean get() = backStack.lastOrNull() is NotesNavKey.StatsHistory

    val selectedTab: NotesTab
        get() =
            when (
                backStack.lastOrNull {
                    it is NotesNavKey.Home || it is NotesNavKey.Bookmarks ||
                        it is NotesNavKey.Stats
                }
            ) {
                NotesNavKey.Bookmarks -> NotesTab.Bookmarks
                NotesNavKey.Stats -> NotesTab.Stats
                else -> NotesTab.Home
            }

    val isActiveNoteBookmarked: Boolean get() = activeNoteId != null && activeNoteId in bookmarkedIds

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
