package com.hancekim.mynote.feature.notes.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.hancekim.mynote.core.designsystem.component.MyNoteSnackbarHost
import com.hancekim.mynote.core.designsystem.component.rememberFloatingNavScrollState
import com.hancekim.mynote.core.designsystem.motion.MyNoteEasing
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.ui.element.NotesDrafts
import com.hancekim.mynote.core.ui.element.NotesSignal
import com.hancekim.mynote.core.ui.frame.ShellFrame
import com.hancekim.mynote.core.ui.shell.NotesScrollState
import com.hancekim.mynote.core.ui.shell.NotesTopBar
import com.hancekim.mynote.core.ui.shell.rememberNotesScrollState
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.ConnectPlace
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.element.StartDestination
import com.hancekim.mynote.feature.notes.quiz.QuizOverlay
import com.hancekim.mynote.feature.notes.screen.ListPanelChrome
import com.hancekim.mynote.feature.notes.screen.ListPanelScreen
import com.hancekim.mynote.feature.notes.screen.ModelConfirmDialog
import com.hancekim.mynote.feature.notes.screen.connect.ConnectReadingScreen
import com.hancekim.mynote.feature.notes.screen.connect.ConnectRoute
import com.hancekim.mynote.feature.notes.settings.SettingsIntent
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
import com.hancekim.mynote.core.ui.R as UiR

// 결정 12 — 순서가 뜻을 갖는 호출(시작 분기의 읽는 중 · 연결 루트, 모델 확인 대화상자, 뒤로가기 핸들러, 목록 패널,
// 퀴즈 오버레이, 상단 바 행동)은 이 파일 한 곳에서 컴포즈 순서대로 직접 부른다. 순서 없는 집합으로 받지 않는다.
// 목적지 키 7개의 화면만 등록 묶음(NotesNavDisplay)을 거친다.

/** 시작 분기 — 셸 상태(연결 · 시드)와 노트 상태(불러옴)를 같은 프레임에 묶어 판정한다. 창 등급 분기 위다. */
@Composable
internal fun NotesStart(
    states: FeatureStates,
    intents: FeatureIntents,
    snackbarHostState: SnackbarHostState,
    drafts: NotesDrafts,
    onEnableNotifications: () -> Unit,
) {
    var showDownloadConfirm by rememberSaveable { mutableStateOf(false) }

    when (startOf(states.shell, states.notes)) {
        StartDestination.Reading -> {
            ConnectReadingScreen()
        }

        StartDestination.ConnectRoot -> {
            ConnectRoute(
                place = ConnectPlace.Root,
                hasConnection = false,
                drafts = drafts,
                snackbarHostState = snackbarHostState,
                onShellBack = { intents.shell(ShellIntent.Back) },
            )
        }

        StartDestination.Shell -> {
            NotesShell(
                states = states,
                intents = intents,
                snackbarHostState = snackbarHostState,
                drafts = drafts,
                onConfirmDownload = { showDownloadConfirm = true },
                onEnableNotifications = onEnableNotifications,
            )
        }
    }

    if (showDownloadConfirm) {
        ModelConfirmDialog(
            onDismiss = { showDownloadConfirm = false },
            onConfirm = {
                showDownloadConfirm = false
                intents.settings(SettingsIntent.StartModelDownload)
            },
        )
    }
}

@Composable
internal fun NotesShell(
    states: FeatureStates,
    intents: FeatureIntents,
    snackbarHostState: SnackbarHostState,
    drafts: NotesDrafts,
    onConfirmDownload: () -> Unit,
    onEnableNotifications: () -> Unit,
) {
    val adaptive = MyNoteTheme.adaptive
    val colors = MyNoteTheme.colors
    val useDrawer = adaptive.useModalDrawer
    val shell = states.shell

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var panelExpanded by rememberSaveable { mutableStateOf(true) }

    val backdrop = rememberLayerBackdrop()
    val navScroll = rememberFloatingNavScrollState()
    LaunchedEffect(shell.backStack.lastOrNull()) { navScroll.reset() }

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

    // 뒤로가기 ⑦ ⑥ ⑤ — 셸 본문. 먼저 컴포즈될수록 나중에 받는다
    BackHandler(
        enabled = !shell.canPop && !drawerEngaged && !collapsiblePanelOpen,
    ) { intents.shell(ShellIntent.Back) }

    BackHandler(enabled = collapsiblePanelOpen && !drawerEngaged) { panelExpanded = false }

    BackHandler(enabled = drawerOpening) { scope.launch { drawerState.close() } }

    val pillVisible = rememberModelPillVisible(shell.modelStatus, shell.isSettingsOpen || shell.isConnectOpen)
    // State 그대로 틀에 넘긴다 — 300ms 동안 셸 본문이 아니라 여백을 읽는 화면만 다시 그린다
    val contentTopPadding =
        animateDpAsState(
            targetValue = if (pillVisible) MODEL_PILL_RESERVE else 0.dp,
            animationSpec = tween(PILL_RESERVE_MILLIS, easing = MyNoteEasing),
            label = "pillReserve",
        )

    val layout = rememberNotesLayout(panelOpen = collapsiblePanelOpen)
    val scroll =
        rememberNotesScrollState(
            shell.activeNoteId,
            shell.isBookmarksTop,
            shell.isStatsTop,
            shell.isStatsHistoryTop,
        )
    ReaderProgressEffect(noteId = shell.activeNoteId, reader = scroll.reader, onIntent = intents.notes)
    ReaderScrollRequestEffect(
        request = states.notes.readerScrollRequest,
        activeNoteId = shell.activeNoteId,
        scroll = scroll,
    )

    // 등록 화면 · 슬롯은 자기 기능 상태만 읽는다 — 다른 기능의 상태가 바뀌어도 다시 그리지 않는다(D41)
    val notesState = rememberUpdatedState(states.notes)
    val settingsState = rememberUpdatedState(states.settings)
    val quizModelStatus = rememberUpdatedState(states.quiz.modelStatus)
    val hasConnection = rememberUpdatedState(shell.connectedRepo != null)
    val panelChrome = rememberUpdatedState(states.listPanel)
    val header = rememberUpdatedState(headerOf(states))
    val currentIntents = rememberUpdatedState(intents)
    val currentConfirmDownload = rememberUpdatedState(onConfirmDownload)
    val currentEnableNotifications = rememberUpdatedState(onEnableNotifications)

    val frame =
        rememberShellFrame(
            contentTopPadding = { contentTopPadding.value },
            layout = layout,
            stackedScenes = useDrawer,
            scroll = scroll,
            drafts = drafts,
            snackbarHostState = snackbarHostState,
        )
    val destinations =
        remember {
            shellDestinations(
                notes = { notesState.value },
                onNotesIntent = { currentIntents.value.notes(it) },
                settings = { settingsState.value },
                onSettingsIntent = { currentIntents.value.settings(it) },
                onEnableNotifications = { currentEnableNotifications.value() },
                quizModelStatus = { quizModelStatus.value },
                onQuizAction = { noteId, action ->
                    val category = notesState.value.categoryNameOf(noteId).orEmpty()
                    currentIntents.value.send(ctaTarget(noteId, category, action), currentConfirmDownload.value)
                },
                hasConnection = { hasConnection.value },
                onShellBack = { currentIntents.value.shell(ShellIntent.Back) },
            )
        }

    val panelSlot =
        rememberNotesPanelSlot(
            notes = { notesState.value },
            chrome = { panelChrome.value },
            onIntent = { currentIntents.value.notes(it) },
        )
    val headerSlot =
        rememberNotesHeaderSlot(
            header = { header.value },
            intents = { currentIntents.value },
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
                topBar = { if (useDrawer && !states.quiz.quiz.coversReader && !shell.isConnectOpen) headerSlot() },
            ) { padding ->
                Box(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .nestedScroll(navScroll.nestedScrollConnection),
                ) {
                    // Scaffold 내용은 드로어가 열린 동안 통째로 꺼져 드로어 ① 이 받는다(D28)
                    CompositionLocalProvider(
                        LocalNavigationEventDispatcherOwner provides
                            rememberNavigationEventDispatcherOwner(enabled = !(useDrawer && drawerState.isOpen)),
                    ) {
                        // layerBackdrop 상자 안에는 NavDisplay 하나 — 오버레이가 들어오면 유리가 자기를 샘플링한다
                        Box(
                            Modifier
                                .fillMaxSize()
                                .layerBackdrop(backdrop),
                        ) {
                            // ④ pop
                            NotesNavDisplay(
                                backStack = shell.backStack,
                                onBack = { intents.shell(ShellIntent.Back) },
                                sceneStrategies = sceneStrategies,
                                frame = frame,
                                destinations = destinations,
                            )
                        }

                        // ③ 전환 중 삼킴 — NavDisplay 뒤라야 예측 뒤로가기 커밋 뒤에도 스택과 화면이 맞는다
                        BackHandler(enabled = !shell.isNavSettled && !drawerEngaged) {}

                        NotesOverlays(
                            states = states,
                            intents = intents,
                            layout = layout,
                            scroll = scroll,
                            navScroll = navScroll,
                            backdrop = backdrop,
                            pillVisible = pillVisible,
                        )

                        // ② 퀴즈 오버레이 — 맨 마지막이라 삼킴 · pop 보다 먼저 받는다
                        QuizOverlay(
                            state = states.quiz.quiz,
                            noteTitle = states.activeNote?.title.orEmpty(),
                            onIntent = intents.quiz,
                            startInset = if (collapsiblePanelOpen) adaptive.paneWidth + 1.dp else 0.dp,
                            backdrop = backdrop,
                        )
                    }
                }
            }
        }

    if (useDrawer) {
        // ① 드로어 — 본 컴포지션의 M3 핸들러
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !shell.isConnectOpen,
            drawerContent = {
                ModalDrawerSheet(
                    drawerState = drawerState,
                    drawerContainerColor = colors.surface,
                    modifier = Modifier.width(adaptive.paneWidth),
                ) {
                    ListPanelScreen(
                        uiState = notesState.value,
                        chrome = panelChrome.value,
                        onIntent = { intent ->
                            currentIntents.value.notes(intent)
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

/** 고정 패널 슬롯 — 넓은 창의 목록 패널. 노트 상태와 셸이 묶은 크롬만 읽는다. */
@Composable
private fun rememberNotesPanelSlot(
    notes: () -> NotesUiState,
    chrome: () -> ListPanelChrome,
    onIntent: (NotesIntent) -> Unit,
): @Composable () -> Unit =
    remember {
        @Composable {
            Row {
                Box(
                    Modifier
                        .width(MyNoteTheme.adaptive.paneWidth)
                        .fillMaxHeight(),
                ) {
                    ListPanelScreen(
                        uiState = notes(),
                        chrome = chrome(),
                        onIntent = onIntent,
                        inFixedPanel = true,
                    )
                }
                VerticalDivider(color = MyNoteTheme.colors.border)
            }
        }
    }

/** 헤더 슬롯 — 상단 바와 그 행동(메뉴 · 뒤로 · 새로고침 · 북마크). 셸이 묶은 [HeaderUi] 만 읽는다. */
@Composable
private fun rememberNotesHeaderSlot(
    header: () -> HeaderUi,
    intents: () -> FeatureIntents,
    onMenu: () -> Unit,
): @Composable () -> Unit {
    val currentMenu = rememberUpdatedState(onMenu)
    val homeTitle = stringResource(UiR.string.ui_app_title)
    val settingsTitle = stringResource(UiR.string.ui_settings)
    val bookmarksTitle = stringResource(R.string.notes_nav_bookmark)
    val statsTitle = stringResource(R.string.notes_nav_stats)
    val historyTitle = stringResource(R.string.notes_stats_history_title)

    return remember(homeTitle, settingsTitle, bookmarksTitle, statsTitle, historyTitle) {
        @Composable {
            val ui = header()
            NotesTopBar(
                title =
                    when (ui.place) {
                        HeaderPlace.Settings -> settingsTitle
                        HeaderPlace.Bookmarks -> bookmarksTitle
                        HeaderPlace.Stats -> statsTitle
                        HeaderPlace.StatsHistory -> historyTitle
                        HeaderPlace.Other -> ui.noteTitle ?: homeTitle
                    },
                showBack = ui.showBack,
                showRefresh = ui.showRefresh,
                bookmarked = ui.bookmarked,
                onMenu = { currentMenu.value() },
                onBack = { intents().shell(ShellIntent.Back) },
                onRefresh = { intents().notes(NotesIntent.Refresh) },
                onToggleBookmark = { intents().notes(NotesIntent.ToggleBookmark) },
            )
        }
    }
}

private enum class HeaderPlace { Settings, Bookmarks, Stats, StatsHistory, Other }

/** 상단 바가 보이는 값 — 셸 · 노트 상태에서 묶는다. 같으면 헤더를 다시 그리지 않는다. */
@Immutable
private data class HeaderUi(
    val place: HeaderPlace,
    val noteTitle: String?,
    val showBack: Boolean,
    val showRefresh: Boolean,
    val bookmarked: Boolean?,
)

private fun headerOf(states: FeatureStates): HeaderUi {
    val shell = states.shell
    val place =
        when {
            shell.isSettingsOpen -> HeaderPlace.Settings
            shell.isBookmarksTop -> HeaderPlace.Bookmarks
            shell.isStatsTop -> HeaderPlace.Stats
            shell.isStatsHistoryTop -> HeaderPlace.StatsHistory
            else -> HeaderPlace.Other
        }
    return HeaderUi(
        place = place,
        noteTitle = if (place == HeaderPlace.Other) states.activeNote?.title else null,
        showBack = shell.isSettingsOpen || shell.isStatsHistoryTop,
        showRefresh = !shell.isStatsHistoryTop,
        bookmarked = if (shell.activeNoteId != null) states.notes.isBookmarked(shell.activeNoteId) else null,
    )
}

/** 셸 틀 계약의 셸 쪽 — 값은 State 로 들고 화면이 읽을 때 꺼낸다. 홀더는 창 등급 분기 위에서 만든 것이다. */
@Composable
private fun rememberShellFrame(
    contentTopPadding: () -> Dp,
    layout: NotesLayout,
    stackedScenes: Boolean,
    scroll: NotesScrollState,
    drafts: NotesDrafts,
    snackbarHostState: SnackbarHostState,
): ShellFrame {
    val currentLayout = rememberUpdatedState(layout)
    val currentStacked = rememberUpdatedState(stackedScenes)
    val currentScroll = rememberUpdatedState(scroll)
    val currentTopPadding = rememberUpdatedState(contentTopPadding)
    return remember(drafts, snackbarHostState) {
        object : ShellFrame {
            override val contentTopPadding: Dp get() = currentTopPadding.value()
            override val contentBottomPadding: Dp get() = currentLayout.value.contentBottomPadding
            override val contentEndPadding: Dp get() = currentLayout.value.contentEndPadding
            override val twoPane: Boolean get() = currentLayout.value.twoPane
            override val stackedScenes: Boolean get() = currentStacked.value
            override val scroll: NotesScrollState get() = currentScroll.value
            override val drafts: NotesDrafts = drafts
            override val snackbarHostState: SnackbarHostState = snackbarHostState
        }
    }
}

@Composable
internal fun NotesExitEffects(
    shell: ShellUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ShellIntent) -> Unit,
    onExit: () -> Unit,
) {
    val exitHint = stringResource(R.string.notes_exit_hint)
    val currentOnIntent by rememberUpdatedState(onIntent)
    val currentOnExit by rememberUpdatedState(onExit)
    LaunchedEffect(shell.exitHintId) {
        val id = shell.exitHintId
        if (id > 0) {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(exitHint, duration = SnackbarDuration.Short)
            currentOnIntent(ShellIntent.SignalShown(NotesSignal.ExitHint, id))
        }
    }
    LaunchedEffect(shell.exitRequestId) {
        if (shell.exitRequestId > 0) currentOnExit()
    }
}

private val MODEL_PILL_RESERVE = MODEL_PILL_RESERVE_DP.dp

private const val PILL_RESERVE_MILLIS = 300
