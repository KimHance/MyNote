package com.hancekim.mynote.feature.notes

import android.app.Activity
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.navigation.NoteDetailKey
import com.hancekim.mynote.feature.notes.navigation.NotesListKey
import com.hancekim.mynote.feature.notes.screen.HomeScreen
import com.hancekim.mynote.feature.notes.screen.ListPanelScreen
import com.hancekim.mynote.feature.notes.screen.NoteReaderScreen
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch

/**
 * 진입점(Route): ViewModel 을 주입받아 상태를 수집하고 [NotesScreen] 에 넘긴다.
 * ViewModel 의존 때문에 프리뷰 대상이 아니며, 프리뷰는 [NotesScreen] 에서 한다.
 */
@Composable
fun NotesRoute(viewModel: NotesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    NotesScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
    )
}

/**
 * 상태 없는(stateless) 화면. `uiState` + `onIntent` 만 받아 프리뷰가 가능하다.
 * 테마 블록(`MyNoteTheme`)은 여기서 시작한다.
 *
 * 윈도우 폭으로 폼팩터를 판정해 Compact 는 모달 드로어, Medium+ 는 Nav3 `ListDetailSceneStrategy`(2-pane).
 * 시스템바 인셋은 Material3 `Scaffold` + `TopAppBar` + `ModalDrawerSheet` 가 처리한다.
 */
@Composable
fun NotesScreen(
    uiState: NotesUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (NotesIntent) -> Unit,
) {
    // containerDpSize 는 이미 Dp 단위라 density 변환이 필요 없다.
    val widthDp = LocalWindowInfo.current.containerDpSize.width
    val windowClass = MyNoteWindowClass.fromWidth(widthDp)

    // 토큰 입력 다이얼로그 표시 상태 — 두 Scaffold 에서 공유한다.
    var showTokenDialog by rememberSaveable { mutableStateOf(false) }

    MyNoteTheme(windowClass = windowClass, fontScale = uiState.fontScale) {
        // 에러 발생 시 스낵바로 안내 (Toast 금지, 룰 05). errorId 가 바뀔 때마다 발동(같은 메시지 재알림 포함).
        LaunchedEffect(uiState.errorId) {
            uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
        }

        if (windowClass == MyNoteWindowClass.Compact) {
            CompactScaffold(uiState, onIntent, snackbarHostState, onShowTokenDialog = { showTokenDialog = true })
        } else {
            ListDetailScaffold(uiState, onIntent, snackbarHostState, onShowTokenDialog = { showTokenDialog = true })
        }

        // 토큰 입력 다이얼로그
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
private fun CompactScaffold(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    onShowTokenDialog: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val exitOnDoubleBack = rememberDoubleBackToExit(snackbarHostState)

    // 뒤로가기 우선순위: 드로어 닫기 → 노트 닫기(홈) → 더블탭 종료
    BackHandler {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            uiState.activeNote != null -> onIntent(NotesIntent.CloseNote)
            else -> exitOnDoubleBack()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // ModalDrawerSheet 가 windowInsets(기본 systemBars)로 드로어 콘텐츠 인셋을 처리한다.
            ModalDrawerSheet(
                drawerContainerColor = colors.surface,
                modifier = Modifier.width(MyNoteTheme.adaptive.paneWidth),
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
    ) {
        Scaffold(
            containerColor = colors.bg,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                NotesTopBar(
                    title = uiState.activeNote?.title ?: "공부 기록",
                    showMenu = true,
                    onMenu = { scope.launch { drawerState.open() } },
                    onToggleFont = { onIntent(NotesIntent.ToggleFontScale) },
                    onTokenDialog = onShowTokenDialog,
                )
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                val note = uiState.activeNote
                if (note == null) {
                    HomeScreen(uiState.recentNotes, uiState.isLoading, onIntent)
                } else {
                    NoteReaderScreen(note, uiState.categoryNameOf(note.id) ?: "")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ListDetailScaffold(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    onShowTokenDialog: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val backStack = rememberNavBackStack(NotesListKey)
    val sceneStrategy = rememberListDetailSceneStrategy<NavKey>()
    val exitOnDoubleBack = rememberDoubleBackToExit(snackbarHostState)

    // 상세 스택이 있으면 NavDisplay 가 pop 을 처리하고, 루트(목록)에서만 더블탭 종료를 잡는다.
    BackHandler(enabled = backStack.size <= 1) { exitOnDoubleBack() }

    fun openNote(id: String) {
        onIntent(NotesIntent.SelectNote(id))
        backStack.add(NoteDetailKey(id))
    }

    Scaffold(
        containerColor = colors.bg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NotesTopBar(
                title = "공부 기록",
                showMenu = false,
                onMenu = {},
                onToggleFont = { onIntent(NotesIntent.ToggleFontScale) },
                onTokenDialog = onShowTokenDialog,
            )
        },
    ) { padding ->
        NavDisplay(
            modifier = Modifier.padding(padding),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            sceneStrategies = listOf(sceneStrategy),
            entryProvider = entryProvider {
                entry<NotesListKey>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = {
                            HomeScreen(
                                recentNotes = uiState.recentNotes,
                                isLoading = uiState.isLoading,
                                onIntent = { intent ->
                                    if (intent is NotesIntent.SelectNote) openNote(intent.id) else onIntent(intent)
                                },
                            )
                        },
                    ),
                ) {
                    ListPanelScreen(
                        uiState = uiState,
                        onIntent = { intent ->
                            if (intent is NotesIntent.SelectNote) openNote(intent.id) else onIntent(intent)
                        },
                    )
                }
                entry<NoteDetailKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                    val note = uiState.noteById(key.id)
                    if (note != null) {
                        NoteReaderScreen(note, uiState.categoryNameOf(key.id) ?: "")
                    } else {
                        HomeScreen(uiState.recentNotes, uiState.isLoading, onIntent)
                    }
                }
            },
        )
    }
}

/**
 * 1초 안에 뒤로가기를 두 번 누르면 앱을 종료, 첫 번째엔 스낵바로 안내하는 핸들러를 반환한다.
 */
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

/**
 * 핸드오프 헤더: (Compact) ☰ + 현재 노트 제목 + 글자 크기 토글 + 토큰 설정 아이콘.
 * Material3 `TopAppBar` 를 써 상태바 인셋은 `windowInsets` 기본값이 처리한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesTopBar(
    title: String,
    showMenu: Boolean,
    onMenu: () -> Unit,
    onToggleFont: () -> Unit,
    onTokenDialog: () -> Unit,
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
        navigationIcon = {
            if (showMenu) {
                IconButton(onClick = onMenu) {
                    Text(text = "☰", style = MyNoteTheme.textStyles.sectionTitle, color = colors.text)
                }
            }
        },
        actions = {
            // 토큰 설정 아이콘(자물쇠) — Icons.Default.Lock 은 material-icons-core 에 포함됨
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
        colors = TopAppBarDefaults.topAppBarColors(
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

// --- Preview ---

private val previewUiState: NotesUiState = run {
    val notes = listOf(
        Note("compose-state", "Compose 상태 관리 정리", "2026.06.12", emptyList()),
        Note("nav3", "Navigation 3 첫인상", "2026.06.18", emptyList()),
    )
    val categories = persistentListOf(
        Category("Android", notes),
        Category("Kotlin", listOf(Note("coroutines", "코루틴 디스패처 메모", "2026.05.30", emptyList()))),
    )
    NotesUiState(
        categories = categories,
        visibleCategories = categories,
        recentNotes = persistentListOf(
            RecentNote(notes[1], "Android"),
            RecentNote(notes[0], "Android"),
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
        snackbarHostState = remember { SnackbarHostState() },
        onIntent = {},
    )
}

@Preview(name = "Notes · Expanded", showBackground = true, widthDp = 900, heightDp = 720)
@Composable
private fun NotesScreenExpandedPreview() {
    NotesScreen(
        uiState = previewUiState,
        snackbarHostState = remember { SnackbarHostState() },
        onIntent = {},
    )
}
