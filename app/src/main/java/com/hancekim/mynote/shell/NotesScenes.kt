package com.hancekim.mynote.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation3.scene.SceneStrategy
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.scene.ListDetailSceneStrategy

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
