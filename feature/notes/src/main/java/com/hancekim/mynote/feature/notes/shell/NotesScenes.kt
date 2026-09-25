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
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.screen.ListPanelScreen

@Composable
internal fun rememberNotesPanelSlot(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
): @Composable () -> Unit {
    val currentState = rememberUpdatedState(uiState)
    val currentIntent = rememberUpdatedState(onIntent)
    return remember {
        @Composable {
            Row {
                Box(
                    Modifier
                        .width(MyNoteTheme.adaptive.paneWidth)
                        .fillMaxHeight(),
                ) {
                    ListPanelScreen(
                        uiState = currentState.value,
                        onIntent = currentIntent.value,
                        applyStatusInset = true,
                    )
                }
                VerticalDivider(color = MyNoteTheme.colors.border)
            }
        }
    }
}

@Composable
internal fun rememberNotesHeaderSlot(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
    onMenu: () -> Unit,
    onShowTokenDialog: () -> Unit,
): @Composable () -> Unit {
    val currentState = rememberUpdatedState(uiState)
    val currentIntent = rememberUpdatedState(onIntent)
    val currentMenu = rememberUpdatedState(onMenu)
    val currentTokenDialog = rememberUpdatedState(onShowTokenDialog)
    val homeTitle = stringResource(R.string.notes_app_title)

    return remember(homeTitle) {
        @Composable {
            NotesTopBar(
                title = currentState.value.activeNote?.title ?: homeTitle,
                onMenu = { currentMenu.value() },
                onToggleFont = { currentIntent.value(NotesIntent.ToggleFontScale) },
                onTokenDialog = { currentTokenDialog.value() },
                onRefresh = { currentIntent.value(NotesIntent.Refresh) },
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
