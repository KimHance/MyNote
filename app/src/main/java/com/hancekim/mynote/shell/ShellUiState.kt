package com.hancekim.mynote.shell

import androidx.compose.runtime.Stable
import com.hancekim.mynote.core.common.mvi.MviEvent
import com.hancekim.mynote.core.common.mvi.MviState
import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.navigation.NotesTab
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.element.NotesSignal
import com.hancekim.mynote.feature.notes.element.NotesUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** 셸이 그리는 값 — 백스택 파생 · 종료 · 시작 분기의 연결 · 시드 · 테마 글자 크기 · 알약의 모델 상태. */
@Stable
data class ShellUiState(
    val backStack: ImmutableList<MyNoteNavKey> = persistentListOf(NotesNavKey.Home),
    val activeNoteId: String? = null,
    val isNavSettled: Boolean = true,
    val exitHintId: Int = 0,
    val exitRequestId: Int = 0,
    val connection: ConnectionState? = null,
    val seedPending: Boolean = true,
    val fontScale: MyNoteFontScale = MyNoteFontScale.Normal,
    val modelStatus: ModelStatus = ModelStatus.Missing,
) : MviState {
    val canPop: Boolean get() = backStack.size > 1

    val isSettingsOpen: Boolean get() = backStack.lastOrNull() is NotesNavKey.Settings

    val isConnectOpen: Boolean get() = backStack.lastOrNull() is NotesNavKey.Connect

    val isBookmarksTop: Boolean get() = backStack.lastOrNull() is NotesNavKey.Bookmarks

    val isStatsTop: Boolean get() = backStack.lastOrNull() is NotesNavKey.Stats

    val isStatsHistoryTop: Boolean get() = backStack.lastOrNull() is NotesNavKey.StatsHistory

    val connectedRepo: RepoConnection? get() = (connection as? ConnectionState.Connected)?.connection

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
}

sealed interface ShellIntent : MviEvent {
    data object Back : ShellIntent

    data class SwitchTab(
        val tab: NotesTab,
    ) : ShellIntent

    data object OpenSettings : ShellIntent

    data class SignalShown(
        val signal: NotesSignal,
        val id: Int,
    ) : ShellIntent
}

/** 시작 분기 — 셸의 연결 · 시드와 노트의 읽음 · 노트 수를 같은 프레임에 묶어 판정한다. */
fun startOf(
    shell: ShellUiState,
    notes: NotesUiState,
): StartDestination =
    startDestination(
        connection =
            when (shell.connection) {
                null -> ConnectionKnown.Unknown
                ConnectionState.None -> ConnectionKnown.None
                is ConnectionState.Connected -> ConnectionKnown.Connected
            },
        notes =
            when {
                !notes.notesLoaded -> NotesKnown.Unknown
                notes.totalCount == 0 -> NotesKnown.Empty
                else -> NotesKnown.Some
            },
        seedPending = shell.seedPending,
    )
