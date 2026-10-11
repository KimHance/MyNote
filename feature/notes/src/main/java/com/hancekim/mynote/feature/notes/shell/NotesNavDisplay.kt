package com.hancekim.mynote.feature.notes.shell

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.motion.scenePopTransitionSpec
import com.hancekim.mynote.core.navigation.motion.scenePredictivePopTransitionSpec
import com.hancekim.mynote.core.navigation.motion.sceneTransitionSpec
import com.hancekim.mynote.core.navigation.registry.destinationProvider
import com.hancekim.mynote.core.navigation.shared.LocalSharedTransitionScope
import com.hancekim.mynote.core.ui.element.CtaAction
import com.hancekim.mynote.core.ui.frame.ShellDestinations
import com.hancekim.mynote.core.ui.frame.ShellFrame
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.notesDestinations
import com.hancekim.mynote.feature.notes.screen.connect.connectDestinations
import com.hancekim.mynote.feature.notes.screen.stats.statsDestinations
import com.hancekim.mynote.feature.notes.settings.SettingsIntent
import com.hancekim.mynote.feature.notes.settings.SettingsUiState
import com.hancekim.mynote.feature.notes.settings.settingsDestinations

/**
 * 등록 묶음 — 목적지 키 7개의 화면은 기능별 등록 파일이 등록한다(FM-4). 셸은 기능마다 자기 상태 · 입구만 넘긴다.
 * 순서는 상관없다(같은 키를 두 번 등록하면 묶음을 만들 때 멈춘다). 순서가 뜻을 갖는 호출은 `NotesShell` 에 있다(결정 12).
 */
internal fun shellDestinations(
    notes: () -> NotesUiState,
    onNotesIntent: (NotesIntent) -> Unit,
    settings: () -> SettingsUiState,
    onSettingsIntent: (SettingsIntent) -> Unit,
    onEnableNotifications: () -> Unit,
    quizModelStatus: () -> ModelStatus,
    onQuizAction: (String, CtaAction) -> Unit,
    hasConnection: () -> Boolean,
    onShellBack: () -> Unit,
): List<ShellDestinations> =
    listOf(
        notesDestinations(notes, onNotesIntent, quizModelStatus, onQuizAction),
        statsDestinations(),
        settingsDestinations(settings, onSettingsIntent, onEnableNotifications),
        connectDestinations(hasConnection, onShellBack),
    )

@Composable
internal fun NotesNavDisplay(
    backStack: List<MyNoteNavKey>,
    onBack: () -> Unit,
    sceneStrategies: List<SceneStrategy<MyNoteNavKey>>,
    frame: ShellFrame,
    destinations: List<ShellDestinations>,
) {
    val entryProvider = remember(frame, destinations) { destinationProvider(frame, destinations) }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavDisplay(
                backStack = backStack,
                onBack = onBack,
                sceneStrategies = sceneStrategies,
                sharedTransitionScope = this@SharedTransitionLayout,
                transitionSpec = sceneTransitionSpec(),
                popTransitionSpec = scenePopTransitionSpec(),
                predictivePopTransitionSpec = scenePredictivePopTransitionSpec(),
                entryProvider = entryProvider,
            )
        }
    }
}
