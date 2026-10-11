package com.hancekim.mynote.feature.notes.shell

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.navigation.motion.scenePopTransitionSpec
import com.hancekim.mynote.core.navigation.motion.scenePredictivePopTransitionSpec
import com.hancekim.mynote.core.navigation.motion.sceneTransitionSpec
import com.hancekim.mynote.core.navigation.scene.rememberSceneEdgeProgress
import com.hancekim.mynote.core.navigation.scene.sceneEdge
import com.hancekim.mynote.core.navigation.shared.LocalSharedTransitionScope
import com.hancekim.mynote.core.navigation.shared.ProvideNavAnimatedScope
import com.hancekim.mynote.feature.notes.element.ConnectPlace
import com.hancekim.mynote.feature.notes.element.CtaAction
import com.hancekim.mynote.feature.notes.element.NotesDrafts
import com.hancekim.mynote.feature.notes.screen.HomeScreen
import com.hancekim.mynote.feature.notes.screen.NoteReaderScreen
import com.hancekim.mynote.feature.notes.screen.SettingsScreen
import com.hancekim.mynote.feature.notes.screen.bookmarks.BookmarksScreen
import com.hancekim.mynote.feature.notes.screen.connect.ConnectRoute
import com.hancekim.mynote.feature.notes.screen.stats.StatsHistoryRoute
import com.hancekim.mynote.feature.notes.screen.stats.StatsRoute

@Composable
internal fun NotesNavDisplay(
    states: FeatureStates,
    intents: FeatureIntents,
    snackbarHostState: SnackbarHostState,
    sceneStrategies: List<SceneStrategy<MyNoteNavKey>>,
    stackedScenes: Boolean,
    contentBottomPadding: Dp,
    contentEndPadding: Dp,
    contentTopPadding: Dp,
    twoPane: Boolean,
    scroll: NotesScrollState,
    drafts: NotesDrafts,
    onQuizAction: (String, CtaAction) -> Unit,
    onEnableNotifications: () -> Unit,
) {
    val currentStates = rememberUpdatedState(states)
    val currentIntents = rememberUpdatedState(intents)
    val notes = states.notes
    val currentQuizAction = rememberUpdatedState(onQuizAction)
    val currentEnableNotifications = rememberUpdatedState(onEnableNotifications)

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavDisplay(
                backStack = states.shell.backStack,
                onBack = { currentIntents.value.shell(ShellIntent.Back) },
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
                                    recentNotes = notes.recentNotes,
                                    body = notes.homeBody,
                                    repoName = notes.heroRepoName,
                                    showConnectBand = notes.showConnectBand && MyNoteTheme.adaptive.useModalDrawer,
                                    onIntent = intents.notes,
                                    contentBottomPadding = contentBottomPadding,
                                    contentEndPadding = contentEndPadding,
                                    contentTopPadding = contentTopPadding,
                                    gridState = scroll.homeGrid,
                                    bookmarkedIds = notes.bookmarkedIds,
                                )
                            }
                        }
                        entry<NotesNavKey.Bookmarks> {
                            ProvideNavAnimatedScope {
                                val edgeProgress = rememberSceneEdgeProgress()
                                BookmarksScreen(
                                    bookmarks = notes.bookmarks,
                                    isLoading = notes.isLoading && notes.categories.isEmpty(),
                                    twoPane = twoPane,
                                    onIntent = intents.notes,
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
                                    uiState = currentStates.value.settings,
                                    onIntent = currentIntents.value.settings,
                                    onEnableNotifications = currentEnableNotifications.value,
                                    drafts = drafts,
                                    snackbarHostState = snackbarHostState,
                                    modifier =
                                        if (stackedScenes) Modifier.sceneEdge(edgeProgress) else Modifier,
                                )
                            }
                        }
                        entry<NotesNavKey.Connect> {
                            ProvideNavAnimatedScope {
                                val edgeProgress = rememberSceneEdgeProgress()
                                ConnectRoute(
                                    place = ConnectPlace.Stacked,
                                    hasConnection = currentStates.value.shell.connectedRepo != null,
                                    drafts = drafts,
                                    snackbarHostState = snackbarHostState,
                                    onShellBack = { currentIntents.value.shell(ShellIntent.Back) },
                                    modifier = if (stackedScenes) Modifier.sceneEdge(edgeProgress) else Modifier,
                                )
                            }
                        }
                        entry<NotesNavKey.Reader> { key ->
                            ProvideNavAnimatedScope {
                                val note = notes.noteById(key.noteId)
                                if (note != null) {
                                    val edgeProgress = rememberSceneEdgeProgress()
                                    NoteReaderScreen(
                                        note = note,
                                        categoryName = notes.categoryNameOf(note.id).orEmpty(),
                                        modifier = if (stackedScenes) Modifier.sceneEdge(edgeProgress) else Modifier,
                                        contentBottomPadding = contentBottomPadding,
                                        contentTopPadding = contentTopPadding,
                                        contentEndPadding = contentEndPadding,
                                        scrollState = scroll.reader,
                                        contentState = scroll.readerContent,
                                        onQuizAction = { action -> currentQuizAction.value(note.id, action) },
                                        modelStatus = currentStates.value.quiz.modelStatus,
                                    )
                                }
                            }
                        }
                    },
            )
        }
    }
}
