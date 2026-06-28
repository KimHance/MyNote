package com.hancekim.mynote.feature.notes

import android.app.Activity
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
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
    val uiState by viewModel.models.collectAsStateWithLifecycle()
    NotesScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

/**
 * 상태 없는(stateless) 화면. `uiState` + `onIntent` 만 받아 프리뷰가 가능하다.
 * 테마 블록(`MyNoteTheme`)은 여기서 시작한다.
 *
 * 윈도우 폭으로 폼팩터를 판정한다. Compact 는 목록을 화면 위로 덮는 모달 드로어(오버레이),
 * Medium+ 는 목록을 옆에서 밀어 넣는(push) 접이식 사이드 패널 — 둘 다 ☰ 토글이 살아 있다.
 * 시스템바 인셋은 Material3 `Scaffold` + `TopAppBar` + `ModalDrawerSheet` 가 처리한다.
 */
@Composable
fun NotesScreen(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
) {
    // containerDpSize 는 이미 Dp 단위라 density 변환이 필요 없다.
    val widthDp = LocalWindowInfo.current.containerDpSize.width
    val windowClass = MyNoteWindowClass.fromWidth(widthDp)

    // 스낵바 호스트는 프레젠터(present)가 소유 — 에러/안내 발동도 거기서. UI 는 렌더만.
    val snackbarHostState = uiState.snackbarHostState

    // 토큰 입력 다이얼로그 표시 상태 — 두 Scaffold 에서 공유한다.
    var showTokenDialog by rememberSaveable { mutableStateOf(false) }

    MyNoteTheme(windowClass = windowClass, fontScale = uiState.fontScale) {
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
                    onRefresh = { onIntent(NotesIntent.Refresh) },
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

@Composable
private fun ListDetailScaffold(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    onShowTokenDialog: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    // 목록 패널은 옆에서 밀려 들어오는(push) 접이식 — 기본 펼침, ☰ 로 토글. 노트 선택해도 유지한다.
    var listExpanded by rememberSaveable { mutableStateOf(true) }
    val exitOnDoubleBack = rememberDoubleBackToExit(snackbarHostState)

    // 뒤로가기: 노트가 열려 있으면 닫아 홈으로 → 아니면 더블탭 종료.
    BackHandler {
        if (uiState.activeNote != null) onIntent(NotesIntent.CloseNote) else exitOnDoubleBack()
    }

    Scaffold(
        containerColor = colors.bg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NotesTopBar(
                title = uiState.activeNote?.title ?: "공부 기록",
                showMenu = true,
                onMenu = { listExpanded = !listExpanded },
                onToggleFont = { onIntent(NotesIntent.ToggleFontScale) },
                onTokenDialog = onShowTokenDialog,
                onRefresh = { onIntent(NotesIntent.Refresh) },
            )
        },
    ) { padding ->
        Row(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // 가로 expand/shrink 로 패널 폭이 0↔paneWidth 로 변하면 Row 가 reflow → 콘텐츠를 밀어내는 push 효과.
            AnimatedVisibility(
                visible = listExpanded,
                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
            ) {
                Row {
                    Box(
                        Modifier
                            .width(MyNoteTheme.adaptive.paneWidth)
                            .fillMaxHeight(),
                    ) {
                        ListPanelScreen(uiState = uiState, onIntent = onIntent)
                    }
                    VerticalDivider(color = colors.border)
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
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
        navigationIcon = {
            if (showMenu) {
                IconButton(onClick = onMenu) {
                    Text(text = "☰", style = MyNoteTheme.textStyles.sectionTitle, color = colors.text)
                }
            }
        },
        actions = {
            // 새로고침 — index 재조회 후 변경 있으면 스낵바 안내
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "새로고침",
                    tint = colors.accent,
                )
            }
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
