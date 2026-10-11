package com.hancekim.mynote.feature.notes.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.scene.SceneStrategy
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.scene.ListDetailSceneStrategy
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.screen.ListPanelScreen

@Composable
internal fun rememberNotesPanelSlot(
    states: FeatureStates,
    intents: FeatureIntents,
): @Composable () -> Unit {
    val currentStates = rememberUpdatedState(states)
    val currentIntents = rememberUpdatedState(intents)
    return remember {
        @Composable {
            Row {
                Box(
                    Modifier
                        .width(MyNoteTheme.adaptive.paneWidth)
                        .fillMaxHeight(),
                ) {
                    ListPanelScreen(
                        uiState = currentStates.value.notes,
                        chrome = currentStates.value.listPanel,
                        onIntent = currentIntents.value.notes,
                        inFixedPanel = true,
                    )
                }
                VerticalDivider(color = MyNoteTheme.colors.border)
            }
        }
    }
}

@Composable
internal fun rememberNotesHeaderSlot(
    states: FeatureStates,
    intents: FeatureIntents,
    onMenu: () -> Unit,
): @Composable () -> Unit {
    val currentStates = rememberUpdatedState(states)
    val currentIntents = rememberUpdatedState(intents)
    val currentMenu = rememberUpdatedState(onMenu)
    val homeTitle = stringResource(R.string.notes_app_title)
    val settingsTitle = stringResource(R.string.notes_settings)
    val bookmarksTitle = stringResource(R.string.notes_nav_bookmark)
    val statsTitle = stringResource(R.string.notes_nav_stats)
    val historyTitle = stringResource(R.string.notes_stats_history_title)

    return remember(homeTitle, settingsTitle, bookmarksTitle, statsTitle, historyTitle) {
        @Composable {
            val states = currentStates.value
            val shell = states.shell
            NotesTopBar(
                title =
                    when {
                        shell.isSettingsOpen -> settingsTitle
                        shell.isBookmarksTop -> bookmarksTitle
                        shell.isStatsTop -> statsTitle
                        shell.isStatsHistoryTop -> historyTitle
                        else -> states.activeNote?.title ?: homeTitle
                    },
                showBack = shell.isSettingsOpen || shell.isStatsHistoryTop,
                showRefresh = !shell.isStatsHistoryTop,
                bookmarked = if (shell.activeNoteId != null) states.notes.isBookmarked(shell.activeNoteId) else null,
                onMenu = { currentMenu.value() },
                onBack = { currentIntents.value.shell(ShellIntent.Back) },
                onRefresh = { currentIntents.value.notes(NotesIntent.Refresh) },
                onToggleBookmark = { currentIntents.value.notes(NotesIntent.ToggleBookmark) },
            )
        }
    }
}

@Composable
internal fun rememberNotesSceneStrategies(
    useDrawer: Boolean,
    panelSlot: @Composable () -> Unit,
    headerSlot: @Composable () -> Unit,
    panelExpanded: () -> Boolean,
): List<SceneStrategy<MyNoteNavKey>> {
    val currentPanelExpanded = rememberUpdatedState(panelExpanded)
    return remember(useDrawer, panelSlot, headerSlot) {
        if (useDrawer) {
            emptyList()
        } else {
            listOf(
                ListDetailSceneStrategy(
                    sceneKey = NOTES_SCENE_KEY,
                    panel = panelSlot,
                    header = headerSlot,
                    panelExpanded = { currentPanelExpanded.value() },
                ),
            )
        }
    }
}

private const val NOTES_SCENE_KEY = "notes-list-detail"
