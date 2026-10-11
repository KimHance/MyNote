package com.hancekim.mynote.shell

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.NavEntry
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.navigation.registry.destinationProvider
import com.hancekim.mynote.core.ui.element.NotesDrafts
import com.hancekim.mynote.core.ui.frame.ShellFrame
import com.hancekim.mynote.core.ui.shell.NotesScrollState
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.settings.SettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class ShellDestinationsTest {
    private val destinations =
        shellDestinations(
            notes = { NotesUiState() },
            onNotesIntent = {},
            settings = { SettingsUiState() },
            onSettingsIntent = {},
            onEnableNotifications = {},
            quizModelStatus = { ModelStatus.Missing },
            onQuizAction = { _, _ -> },
            hasConnection = { false },
            onShellBack = {},
        )

    @Test
    fun `FM-4 모든 목적지 키에 화면이 하나씩 등록된다`() {
        // 등록 하나씩 따로 묶어 어느 키를 아는지 센다 — 모르는 키는 표시 엔트리로 떨어진다
        val handledBy =
            ALL_KEYS.associateWith { key ->
                destinations.count { registration ->
                    destinationProvider(FRAME, listOf(registration), fallback = { NavEntry(it, UNKNOWN) {} })(key)
                        .contentKey != UNKNOWN
                }
            }

        assertEquals(ALL_KEYS.associateWith { 1 }, handledBy)
    }

    @Test
    fun `FM-4 등록 묶음 전체는 같은 키를 두 번 등록하지 않는다`() {
        // 두 번 등록하면 묶음을 만들 때 멈춘다 — 셸이 그리는 묶음 그대로 만들어 본다
        val provider = destinationProvider(FRAME, destinations)

        ALL_KEYS.forEach { provider(it) }
    }

    private companion object {
        const val UNKNOWN = "unknown"

        /** 목적지 키 7개. 키가 늘면 [covered] 가 컴파일을 멈춰 여기에 더하게 한다. */
        val ALL_KEYS: List<NotesNavKey> =
            listOf(
                NotesNavKey.Home,
                NotesNavKey.Bookmarks,
                NotesNavKey.Stats,
                NotesNavKey.StatsHistory,
                NotesNavKey.Reader("note"),
                NotesNavKey.Settings,
                NotesNavKey.Connect,
            ).onEach(::covered)

        fun covered(key: NotesNavKey) =
            when (key) {
                NotesNavKey.Home,
                NotesNavKey.Bookmarks,
                NotesNavKey.Stats,
                NotesNavKey.StatsHistory,
                is NotesNavKey.Reader,
                NotesNavKey.Settings,
                NotesNavKey.Connect,
                -> Unit
            }

        /** 등록은 틀 값을 화면 안에서만 읽는다 — 시험은 그리지 않으니 읽히면 멈춘다. */
        val FRAME =
            object : ShellFrame {
                override val contentTopPadding: Dp get() = error("시험은 그리지 않는다")
                override val contentBottomPadding: Dp get() = error("시험은 그리지 않는다")
                override val contentEndPadding: Dp get() = error("시험은 그리지 않는다")
                override val twoPane: Boolean get() = error("시험은 그리지 않는다")
                override val stackedScenes: Boolean get() = error("시험은 그리지 않는다")
                override val scroll: NotesScrollState get() = error("시험은 그리지 않는다")
                override val drafts: NotesDrafts get() = error("시험은 그리지 않는다")
                override val snackbarHostState: SnackbarHostState get() = error("시험은 그리지 않는다")
            }
    }
}
