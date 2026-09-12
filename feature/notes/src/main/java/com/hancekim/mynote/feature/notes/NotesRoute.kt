package com.hancekim.mynote.feature.notes

import android.app.Activity
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.hancekim.mynote.core.designfoundation.window.AdaptiveTokens
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.component.FloatingNavBar
import com.hancekim.mynote.core.designsystem.component.FloatingNavDefaults
import com.hancekim.mynote.core.designsystem.component.FloatingNavItem
import com.hancekim.mynote.core.designsystem.component.FloatingNavRail
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.ScrollToTopButton
import com.hancekim.mynote.core.designsystem.component.ScrollToTopDefaults
import com.hancekim.mynote.core.designsystem.component.rememberFloatingNavScrollState
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.navigation.key.notesBackStackOf
import com.hancekim.mynote.core.navigation.motion.scenePopTransitionSpec
import com.hancekim.mynote.core.navigation.motion.scenePredictivePopTransitionSpec
import com.hancekim.mynote.core.navigation.motion.sceneTransitionSpec
import com.hancekim.mynote.core.navigation.scene.ListDetailSceneStrategy
import com.hancekim.mynote.core.navigation.scene.rememberSceneEdgeProgress
import com.hancekim.mynote.core.navigation.scene.sceneEdge
import com.hancekim.mynote.core.navigation.shared.LocalSharedTransitionScope
import com.hancekim.mynote.core.navigation.shared.ProvideNavAnimatedScope
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.screen.HomeScreen
import com.hancekim.mynote.feature.notes.screen.ListPanelScreen
import com.hancekim.mynote.feature.notes.screen.NoteReaderScreen
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch

@Composable
fun NotesRoute(viewModel: NotesViewModel = hiltViewModel()) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    NotesScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

@Composable
internal fun NotesScreen(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
) {
    val containerSize = LocalWindowInfo.current.containerDpSize
    val windowClass = MyNoteWindowClass.fromWidth(containerSize.width)
    val heightClass = MyNoteHeightClass.fromHeight(containerSize.height)

    val snackbarHostState = uiState.snackbarHostState
    LaunchedEffect(uiState.errorId) {
        if (uiState.errorId > 0) uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(uiState.infoId) {
        if (uiState.infoId > 0) uiState.infoMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    var showTokenDialog by rememberSaveable { mutableStateOf(false) }

    MyNoteTheme(windowClass = windowClass, heightClass = heightClass, fontScale = uiState.fontScale) {
        NotesNavHost(
            uiState = uiState,
            onIntent = onIntent,
            snackbarHostState = snackbarHostState,
            onShowTokenDialog = { showTokenDialog = true },
        )

        if (showTokenDialog) {
            TokenDialog(
                onDismiss = { showTokenDialog = false },
                onSave = { token ->
                    onIntent(NotesIntent.SaveToken(token))
                    showTokenDialog = false
                },
            )
        }
    }
}

@Composable
private fun NotesNavHost(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    onShowTokenDialog: () -> Unit,
) {
    val adaptive = MyNoteTheme.adaptive
    val colors = MyNoteTheme.colors
    val useDrawer = adaptive.useModalDrawer

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var panelExpanded by rememberSaveable { mutableStateOf(true) }
    var selectedNavItem by rememberSaveable { mutableStateOf(NAV_HOME) }

    val backdrop = rememberLayerBackdrop()
    val navScroll = rememberFloatingNavScrollState()
    LaunchedEffect(uiState.activeNoteId) { navScroll.reset() }

    val exitOnDoubleBack = rememberDoubleBackToExit(snackbarHostState)
    BackHandler(enabled = uiState.activeNoteId == null && !drawerState.isOpen) { exitOnDoubleBack() }

    val backStack = remember(uiState.activeNoteId) { notesBackStackOf(uiState.activeNoteId) }
    val layoutDirection = LocalLayoutDirection.current
    val navInsets = WindowInsets.navigationBars.asPaddingValues()
    val bottomInset = navInsets.calculateBottomPadding()
    val startInset = navInsets.calculateStartPadding(layoutDirection)
    val endInset = navInsets.calculateEndPadding(layoutDirection)
    val headerOffset =
        if (adaptive.useModalDrawer) {
            0.dp
        } else {
            adaptive.headerHeight + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        }
    val contentBottomPadding =
        if (adaptive.useFloatingRail) bottomInset else FLOATING_BAR_RESERVE + bottomInset

    val homeGridState = rememberLazyGridState()
    val readerScrollState = key(uiState.activeNoteId) { rememberScrollState() }
    val scrollToTopThresholdPx = with(LocalDensity.current) { SCROLL_TO_TOP_THRESHOLD.toPx() }
    val isReader = uiState.activeNoteId != null
    val showScrollToTop by remember(isReader, scrollToTopThresholdPx) {
        derivedStateOf {
            if (isReader) {
                readerScrollState.value > scrollToTopThresholdPx
            } else {
                homeGridState.firstVisibleItemIndex >= SCROLL_TO_TOP_ITEM_THRESHOLD
            }
        }
    }

    val currentState = rememberUpdatedState(uiState)
    val currentIntent = rememberUpdatedState(onIntent)
    val currentPanelExpanded = rememberUpdatedState(panelExpanded)
    val currentUseDrawer = rememberUpdatedState(useDrawer)
    val currentShowTokenDialog = rememberUpdatedState(onShowTokenDialog)

    val panelSlot =
        remember {
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
    val headerSlot =
        remember {
            @Composable {
                NotesTopBar(
                    title = currentState.value.activeNote?.title ?: HOME_TITLE,
                    onMenu = {
                        if (currentUseDrawer.value) {
                            scope.launch { drawerState.open() }
                        } else {
                            panelExpanded = !panelExpanded
                        }
                    },
                    onToggleFont = { currentIntent.value(NotesIntent.ToggleFontScale) },
                    onTokenDialog = { currentShowTokenDialog.value() },
                    onRefresh = { currentIntent.value(NotesIntent.Refresh) },
                )
            }
        }

    val sceneStrategies: List<SceneStrategy<MyNoteNavKey>> =
        remember(useDrawer, panelSlot, headerSlot) {
            if (useDrawer) {
                emptyList()
            } else {
                listOf(
                    ListDetailSceneStrategy(
                        sceneKey = NOTES_SCENE_KEY,
                        panel = panelSlot,
                        header = headerSlot,
                        panelExpanded = { currentPanelExpanded.value },
                    ),
                )
            }
        }

    val scaffold =
        @Composable {
            Scaffold(
                containerColor = colors.bg,
                contentWindowInsets = WindowInsets(0),
                snackbarHost = {
                    SnackbarHost(
                        snackbarHostState,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    )
                },
                topBar = { if (useDrawer) headerSlot() },
            ) { padding ->
                Box(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .nestedScroll(navScroll.nestedScrollConnection),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop),
                    ) {
                        SharedTransitionLayout {
                            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                                NavDisplay(
                                    backStack = backStack,
                                    onBack = { currentIntent.value(NotesIntent.CloseNote) },
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
                                                        gridState = homeGridState,
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
                                                                if (useDrawer) {
                                                                    Modifier.sceneEdge(edgeProgress)
                                                                } else {
                                                                    Modifier
                                                                },
                                                            contentBottomPadding = contentBottomPadding,
                                                            scrollState = readerScrollState,
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                )
                            }
                        }
                    }

                    // 유리보다 먼저 그려 뒤에 깔린다 — 바/버튼이 덮으므로 실제로는 둘 사이의 목만 보인다
                    FloatingNav(
                        selectedId = selectedNavItem,
                        onSelect = { id ->
                            selectedNavItem = id
                            if (id == NAV_HOME) onIntent(NotesIntent.CloseNote)
                        },
                        hidden = navScroll.hidden,
                        topOffset = headerOffset,
                        bottomInset = bottomInset,
                        startInset = startInset,
                        endInset = endInset,
                        backdrop = backdrop,
                    )

                    ScrollToTopButton(
                        visible = showScrollToTop,
                        onClick = {
                            navScroll.reset()
                            scope.launch {
                                if (isReader) {
                                    readerScrollState.animateScrollTo(0)
                                } else {
                                    homeGridState.animateScrollToItem(0)
                                }
                            }
                        },
                        backdrop = backdrop,
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(
                                    end = adaptive.railRightMargin + endInset,
                                    bottom = scrollToTopBottom(adaptive, bottomInset),
                                ),
                    )
                }
            }
        }

    if (useDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
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
                            if (intent is NotesIntent.SelectNote) scope.launch { drawerState.close() }
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
private fun BoxScope.FloatingNav(
    selectedId: String,
    onSelect: (String) -> Unit,
    hidden: Boolean,
    topOffset: Dp,
    bottomInset: Dp,
    startInset: Dp,
    endInset: Dp,
    backdrop: Backdrop,
) {
    val adaptive = MyNoteTheme.adaptive
    if (adaptive.useFloatingRail) {
        FloatingNavRail(
            items = floatingNavItems,
            selectedId = selectedId,
            onSelect = onSelect,
            hidden = hidden,
            rightMargin = adaptive.railRightMargin,
            backdrop = backdrop,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = topOffset + adaptive.padY, end = adaptive.railRightMargin + endInset),
        )
    } else {
        FloatingNavBar(
            items = floatingNavItems,
            selectedId = selectedId,
            onSelect = onSelect,
            hidden = hidden,
            backdrop = backdrop,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = startInset,
                        end = endInset,
                        bottom = adaptive.barBottomMargin + bottomInset,
                    ),
        )
    }
}

private val SCROLL_TO_TOP_THRESHOLD = 400.dp
private const val SCROLL_TO_TOP_ITEM_THRESHOLD = 3

private fun scrollToTopBottom(
    adaptive: AdaptiveTokens,
    bottomInset: Dp,
): Dp =
    if (adaptive.useFloatingRail) {
        adaptive.padY + bottomInset
    } else {
        adaptive.barBottomMargin + bottomInset +
            (FloatingNavDefaults.BarHeight - ScrollToTopDefaults.Size) / 2
    }

private const val NOTES_SCENE_KEY = "notes-list-detail"
private const val NAV_HOME = "home"
private const val HOME_TITLE = "공부 기록"
private val FLOATING_BAR_RESERVE = 92.dp

private val floatingNavItems =
    persistentListOf(
        FloatingNavItem(NAV_HOME, "홈", MyNoteIcons.Home),
        FloatingNavItem("bookmark", "북마크", MyNoteIcons.Bookmark),
        FloatingNavItem("stats", "통계", MyNoteIcons.Stats),
    )

@Composable
private fun rememberDoubleBackToExit(snackbarHostState: SnackbarHostState): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lastBackMillis by remember { mutableLongStateOf(0L) }

    return {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBackMillis < EXIT_WINDOW_MILLIS) {
            (context as? Activity)?.finish()
        } else {
            lastBackMillis = now
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = "한 번 더 누르면 종료돼요",
                    duration = SnackbarDuration.Short,
                )
            }
        }
    }
}

private const val EXIT_WINDOW_MILLIS = 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesTopBar(
    title: String,
    onMenu: () -> Unit,
    onToggleFont: () -> Unit,
    onTokenDialog: () -> Unit,
    onRefresh: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MyNoteTheme.textStyles.sectionTitle,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        expandedHeight = MyNoteTheme.adaptive.headerHeight,
        navigationIcon = {
            IconButton(onClick = onMenu) {
                Text(text = "☰", style = MyNoteTheme.textStyles.sectionTitle, color = colors.text)
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "새로고침",
                    tint = colors.accent,
                )
            }
            IconButton(onClick = onTokenDialog) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "토큰 설정",
                    tint = colors.accent,
                )
            }
            IconButton(onClick = onToggleFont) {
                Text(text = "가", style = MyNoteTheme.textStyles.treeLabel, color = colors.accent)
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = colors.bg,
                titleContentColor = colors.text,
                navigationIconContentColor = colors.text,
                actionIconContentColor = colors.accent,
            ),
    )
}

/**
 * GitHub 토큰 입력 다이얼로그. feature 전용 Composable — design-system 재사용 컴포넌트 아님.
 * PasswordVisualTransformation 으로 토큰 값을 가린다.
 */
@Composable
private fun TokenDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val colors = MyNoteTheme.colors
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = colors.accent,
            )
        },
        title = {
            Text(
                text = "GitHub 토큰 설정",
                style = MyNoteTheme.textStyles.sectionTitle,
                color = colors.text,
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = {
                    Text(
                        text = "토큰 입력",
                        style = MyNoteTheme.textStyles.treeLabel,
                        color = colors.text2,
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text(
                    text = "저장",
                    style = MyNoteTheme.textStyles.treeLabel,
                    color = colors.accent,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "취소",
                    style = MyNoteTheme.textStyles.treeLabel,
                    color = colors.text2,
                )
            }
        },
        containerColor = colors.surface,
    )
}

private val previewUiState: NotesUiState =
    run {
        val notes =
            persistentListOf(
                NoteUi("compose-state", "Compose 상태 관리 정리", "2026.06.12"),
                NoteUi("nav3", "Navigation 3 첫인상", "2026.06.18"),
            )
        val categories =
            persistentListOf(
                CategoryUi("Android", notes),
                CategoryUi("Kotlin", persistentListOf(NoteUi("coroutines", "코루틴 디스패처 메모", "2026.05.30"))),
            )
        NotesUiState(
            categories = categories,
            visibleCategories = categories,
            recentNotes =
                persistentListOf(
                    RecentNoteUi(notes[1], "Android"),
                    RecentNoteUi(notes[0], "Android"),
                ),
            expanded = persistentMapOf("Android" to true),
            isLoading = false,
        )
    }

@Preview(name = "Notes · Compact", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun NotesScreenCompactPreview() {
    NotesScreen(
        uiState = previewUiState,
        onIntent = {},
    )
}

@Preview(name = "Notes · Expanded", showBackground = true, widthDp = 900, heightDp = 720)
@Composable
private fun NotesScreenExpandedPreview() {
    NotesScreen(
        uiState = previewUiState,
        onIntent = {},
    )
}

@Preview(name = "Notes · 짧은 창", showBackground = true, widthDp = 760, heightDp = 420)
@Composable
private fun NotesScreenShortPreview() {
    NotesScreen(
        uiState = previewUiState,
        onIntent = {},
    )
}
