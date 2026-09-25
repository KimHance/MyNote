package com.hancekim.mynote.feature.notes.shell

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.navigation.motion.scenePopTransitionSpec
import com.hancekim.mynote.core.navigation.motion.scenePredictivePopTransitionSpec
import com.hancekim.mynote.core.navigation.motion.sceneTransitionSpec
import com.hancekim.mynote.core.navigation.scene.rememberSceneEdgeProgress
import com.hancekim.mynote.core.navigation.scene.sceneEdge
import com.hancekim.mynote.core.navigation.shared.LocalSharedTransitionScope
import com.hancekim.mynote.core.navigation.shared.ProvideNavAnimatedScope
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.screen.HomeScreen
import com.hancekim.mynote.feature.notes.screen.NoteReaderScreen

@Composable
internal fun NotesNavDisplay(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
    sceneStrategies: List<SceneStrategy<MyNoteNavKey>>,
    stackedScenes: Boolean,
    contentBottomPadding: Dp,
    scroll: NotesScrollState,
    onOpenQuiz: (String) -> Unit,
) {
    val currentState = rememberUpdatedState(uiState)
    val currentIntent = rememberUpdatedState(onIntent)
    val currentOpenQuiz = rememberUpdatedState(onOpenQuiz)

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavDisplay(
                backStack = uiState.backStack,
                onBack = { currentIntent.value(NotesIntent.Back) },
                sceneStrategies = sceneStrategies,
                sharedTransitionScope = this@SharedTransitionLayout,
                transitionSpec = sceneTransitionSpec(),
                popTransitionSpec = scenePopTransitionSpec(),
                predictivePopTransitionSpec = scenePredictivePopTransitionSpec(),
                entryProvider =
                    entryProvider {
                        entry<NotesNavKey.Home> {
                            ProvideNavAnimatedScope {
                                HomeScreen(
                                    recentNotes = uiState.recentNotes,
                                    isLoading = uiState.isLoading,
                                    onIntent = onIntent,
                                    contentBottomPadding = contentBottomPadding,
                                    gridState = scroll.homeGrid,
                                )
                            }
                        }
                        entry<NotesNavKey.Reader> { key ->
                            ProvideNavAnimatedScope {
                                val note = uiState.noteById(key.noteId)
                                if (note != null) {
                                    val edgeProgress = rememberSceneEdgeProgress()
                                    NoteReaderScreen(
                                        note = note,
                                        categoryName = uiState.categoryNameOf(note.id).orEmpty(),
                                        // 스택으로 쌓이는 Compact 에서만 모서리·그림자가 의미 있다
                                        modifier =
                                            if (stackedScenes) Modifier.sceneEdge(edgeProgress) else Modifier,
                                        contentBottomPadding = contentBottomPadding,
                                        scrollState = scroll.reader,
                                        onOpenQuiz = { currentOpenQuiz.value(note.id) },
                                        quizAvailable = currentState.value.isQuizAvailable,
                                    )
                                }
                            }
                        }
                    },
            )
        }
    }
}
