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
import com.hancekim.mynote.feature.notes.element.CtaAction
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.screen.HomeScreen
import com.hancekim.mynote.feature.notes.screen.NoteReaderScreen
import com.hancekim.mynote.feature.notes.screen.SettingsScreen
import com.hancekim.mynote.feature.notes.screen.bookmarks.BookmarksScreen
import com.hancekim.mynote.feature.notes.screen.stats.StatsHistoryRoute
import com.hancekim.mynote.feature.notes.screen.stats.StatsRoute

@Composable
internal fun NotesNavDisplay(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
    sceneStrategies: List<SceneStrategy<MyNoteNavKey>>,
    stackedScenes: Boolean,
    contentBottomPadding: Dp,
    contentEndPadding: Dp,
    contentTopPadding: Dp,
    twoPane: Boolean,
    scroll: NotesScrollState,
    onQuizAction: (String, CtaAction) -> Unit,
    onEnableNotifications: () -> Unit,
) {
    val currentState = rememberUpdatedState(uiState)
    val currentIntent = rememberUpdatedState(onIntent)
    val currentQuizAction = rememberUpdatedState(onQuizAction)
    val currentEnableNotifications = rememberUpdatedState(onEnableNotifications)

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
                                    contentEndPadding = contentEndPadding,
                                    contentTopPadding = contentTopPadding,
                                    gridState = scroll.homeGrid,
                                    bookmarkedIds = uiState.bookmarkedIds,
                                )
                            }
                        }
                        entry<NotesNavKey.Bookmarks> {
                            ProvideNavAnimatedScope {
                                val edgeProgress = rememberSceneEdgeProgress()
                                BookmarksScreen(
                                    bookmarks = uiState.bookmarks,
                                    isLoading = uiState.isLoading && uiState.categories.isEmpty(),
                                    twoPane = twoPane,
                                    onIntent = onIntent,
                                    modifier = if (stackedScenes) Modifier.sceneEdge(edgeProgress) else Modifier,
                                    contentBottomPadding = contentBottomPadding,
                                    contentEndPadding = contentEndPadding,
                                    contentTopPadding = contentTopPadding,
                                    listState = scroll.bookmarksList,
                                )
                            }
                        }
                        entry<NotesNavKey.Stats> {
                            ProvideNavAnimatedScope {
                                val edgeProgress = rememberSceneEdgeProgress()
                                StatsRoute(
                                    twoPane = twoPane,
                                    onNotesIntent = { currentIntent.value(it) },
                                    listState = scroll.statsList,
                                    sideState = scroll.statsSide,
                                    modifier = if (stackedScenes) Modifier.sceneEdge(edgeProgress) else Modifier,
                                    contentBottomPadding = contentBottomPadding,
                                    contentTopPadding = contentTopPadding,
                                    contentEndPadding = contentEndPadding,
                                )
                            }
                        }
                        entry<NotesNavKey.StatsHistory> {
                            ProvideNavAnimatedScope {
                                val edgeProgress = rememberSceneEdgeProgress()
                                StatsHistoryRoute(
                                    listState = scroll.statsHistory,
                                    modifier = if (stackedScenes) Modifier.sceneEdge(edgeProgress) else Modifier,
                                    contentBottomPadding = contentBottomPadding,
                                    contentTopPadding = contentTopPadding,
                                    contentEndPadding = contentEndPadding,
                                )
                            }
                        }
                        entry<NotesNavKey.Settings> {
                            ProvideNavAnimatedScope {
                                val edgeProgress = rememberSceneEdgeProgress()
                                SettingsScreen(
                                    uiState = currentState.value,
                                    onIntent = currentIntent.value,
                                    onEnableNotifications = currentEnableNotifications.value,
                                    modifier =
                                        if (stackedScenes) Modifier.sceneEdge(edgeProgress) else Modifier,
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
                                        modifier = if (stackedScenes) Modifier.sceneEdge(edgeProgress) else Modifier,
                                        contentBottomPadding = contentBottomPadding,
                                        contentTopPadding = contentTopPadding,
                                        scrollState = scroll.reader,
                                        contentState = scroll.readerContent,
                                        onQuizAction = { action -> currentQuizAction.value(note.id, action) },
                                        modelStatus = currentState.value.modelStatus,
                                    )
                                }
                            }
                        }
                    },
            )
        }
    }
}
