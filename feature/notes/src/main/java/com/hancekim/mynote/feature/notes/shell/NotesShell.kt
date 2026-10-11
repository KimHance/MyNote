package com.hancekim.mynote.feature.notes.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.hancekim.mynote.core.designsystem.component.MyNoteSnackbarHost
import com.hancekim.mynote.core.designsystem.component.rememberFloatingNavScrollState
import com.hancekim.mynote.core.designsystem.motion.MyNoteEasing
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.CtaAction
import com.hancekim.mynote.feature.notes.element.NotesDrafts
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesSignal
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.screen.ListPanelScreen
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

@Composable
internal fun NotesShell(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    drafts: NotesDrafts,
    onConfirmDownload: () -> Unit,
    onEnableNotifications: () -> Unit,
) {
    val adaptive = MyNoteTheme.adaptive
    val colors = MyNoteTheme.colors
    val useDrawer = adaptive.useModalDrawer

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var panelExpanded by rememberSaveable { mutableStateOf(true) }

    val backdrop = rememberLayerBackdrop()
    val navScroll = rememberFloatingNavScrollState()
    LaunchedEffect(uiState.backStack.lastOrNull()) { navScroll.reset() }

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Closed) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }
    LaunchedEffect(panelExpanded) {
        if (!panelExpanded) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }

    val drawerEngaged = drawerState.isOpen || drawerState.targetValue == DrawerValue.Open
    val drawerOpening = drawerEngaged && !drawerState.isOpen
    val collapsiblePanelOpen = !useDrawer && panelExpanded

    BackHandler(
        enabled = !uiState.canPop && !drawerEngaged && !collapsiblePanelOpen,
    ) { onIntent(NotesIntent.Back) }

    BackHandler(enabled = collapsiblePanelOpen && !drawerEngaged) { panelExpanded = false }

    BackHandler(enabled = drawerOpening) { scope.launch { drawerState.close() } }

    val pillVisible = rememberModelPillVisible(uiState.modelStatus, uiState.isSettingsOpen || uiState.isConnectOpen)
    val contentTopPadding by animateDpAsState(
        targetValue = if (pillVisible) MODEL_PILL_RESERVE else 0.dp,
        animationSpec = tween(PILL_RESERVE_MILLIS, easing = MyNoteEasing),
        label = "pillReserve",
    )

    val layout = rememberNotesLayout(panelOpen = collapsiblePanelOpen)
    val scroll =
        rememberNotesScrollState(
            uiState.activeNoteId,
            uiState.isBookmarksTop,
            uiState.isStatsTop,
            uiState.isStatsHistoryTop,
        )
    ReaderProgressEffect(noteId = uiState.activeNoteId, reader = scroll.reader, onIntent = onIntent)
    ReaderScrollRequestEffect(
        request = uiState.readerScrollRequest,
        activeNoteId = uiState.activeNoteId,
        scroll = scroll,
    )

    val panelSlot = rememberNotesPanelSlot(uiState, onIntent)
    val headerSlot =
        rememberNotesHeaderSlot(
            uiState = uiState,
            onIntent = onIntent,
            onMenu = {
                if (useDrawer) scope.launch { drawerState.open() } else panelExpanded = !panelExpanded
            },
        )
    val sceneStrategies =
        rememberNotesSceneStrategies(useDrawer, panelSlot, headerSlot) { panelExpanded }

    val scaffold =
        @Composable {
            Scaffold(
                containerColor = colors.bg,
                contentWindowInsets = WindowInsets(0),
                snackbarHost = {
                    MyNoteSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    )
                },
                topBar = { if (useDrawer && !uiState.quiz.coversReader && !uiState.isConnectOpen) headerSlot() },
            ) { padding ->
                Box(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .nestedScroll(navScroll.nestedScrollConnection),
                ) {
                    CompositionLocalProvider(
                        LocalNavigationEventDispatcherOwner provides
                            rememberNavigationEventDispatcherOwner(enabled = !(useDrawer && drawerState.isOpen)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .layerBackdrop(backdrop),
                        ) {
                            NotesNavDisplay(
                                uiState = uiState,
                                onIntent = onIntent,
                                sceneStrategies = sceneStrategies,
                                stackedScenes = useDrawer,
                                contentBottomPadding = layout.contentBottomPadding,
                                contentEndPadding = layout.contentEndPadding,
                                contentTopPadding = contentTopPadding,
                                twoPane = layout.twoPane,
                                onEnableNotifications = onEnableNotifications,
                                scroll = scroll,
                                drafts = drafts,
                                onQuizAction = { noteId, action ->
                                    when (action) {
                                        CtaAction.OpenSetup -> onIntent(NotesIntent.OpenQuizSetup(noteId))
                                        CtaAction.ConfirmDownload -> onConfirmDownload()
                                        CtaAction.OpenSettings -> onIntent(NotesIntent.OpenSettings)
                                        CtaAction.StartDownload -> onIntent(NotesIntent.StartModelDownload)
                                    }
                                },
                            )
                        }

                        BackHandler(enabled = !uiState.isNavSettled && !drawerEngaged) {}

                        NotesOverlays(
                            uiState = uiState,
                            onIntent = onIntent,
                            layout = layout,
                            scroll = scroll,
                            navScroll = navScroll,
                            backdrop = backdrop,
                            pillVisible = pillVisible,
                            quizStartInset = if (collapsiblePanelOpen) adaptive.paneWidth + 1.dp else 0.dp,
                        )
                    }
                }
            }
        }

    if (useDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !uiState.isConnectOpen,
            drawerContent = {
                ModalDrawerSheet(
                    drawerState = drawerState,
                    drawerContainerColor = colors.surface,
                    modifier = Modifier.width(adaptive.paneWidth),
                ) {
                    ListPanelScreen(
                        uiState = uiState,
                        onIntent = { intent ->
                            onIntent(intent)
                            if (intent is NotesIntent.SelectNote || intent is NotesIntent.OpenSettings) {
                                scope.launch { drawerState.close() }
                            }
                        },
                    )
                }
            },
            content = scaffold,
        )
    } else {
        scaffold()
    }
}

@Composable
internal fun NotesExitEffects(
    uiState: NotesUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (NotesIntent) -> Unit,
    onExit: () -> Unit,
) {
    val exitHint = stringResource(R.string.notes_exit_hint)
    val currentOnIntent by rememberUpdatedState(onIntent)
    val currentOnExit by rememberUpdatedState(onExit)
    LaunchedEffect(uiState.exitHintId) {
        val id = uiState.exitHintId
        if (id > 0) {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(exitHint, duration = SnackbarDuration.Short)
            currentOnIntent(NotesIntent.SignalShown(NotesSignal.ExitHint, id))
        }
    }
    LaunchedEffect(uiState.exitRequestId) {
        if (uiState.exitRequestId > 0) currentOnExit()
    }
}

private val MODEL_PILL_RESERVE = MODEL_PILL_RESERVE_DP.dp

private const val PILL_RESERVE_MILLIS = 300
