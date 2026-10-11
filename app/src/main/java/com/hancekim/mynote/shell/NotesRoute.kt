package com.hancekim.mynote.shell

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.ui.element.NotesDrafts
import com.hancekim.mynote.core.ui.element.NotesSignal
import com.hancekim.mynote.core.ui.element.NoticeUi
import com.hancekim.mynote.feature.notes.CategoryUi
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.NotesViewModel
import com.hancekim.mynote.feature.notes.RecentNoteUi
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.quiz.QuizIntent
import com.hancekim.mynote.feature.quiz.QuizViewModel
import com.hancekim.mynote.feature.settings.SettingsIntent
import com.hancekim.mynote.feature.settings.SettingsViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import com.hancekim.mynote.core.designsystem.R as DsR

/** 기능 ViewModel 넷은 모두 Activity 를 owner 로 받는다 — 창 등급이 바뀌어 NavDisplay 가 다시 만들어져도 남는다(결정 7). */
@Composable
fun NotesRoute(
    shellViewModel: ShellViewModel = hiltViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity),
    notesViewModel: NotesViewModel = hiltViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity),
    settingsViewModel: SettingsViewModel =
        hiltViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity),
    quizViewModel: QuizViewModel = hiltViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity),
) {
    val activity = LocalActivity.current
    val shell by shellViewModel.state.collectAsStateWithLifecycle()
    val notes by notesViewModel.state.collectAsStateWithLifecycle()
    val settings by settingsViewModel.state.collectAsStateWithLifecycle()
    val quiz by quizViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsEnabled by remember { mutableStateOf(notificationsAllowed(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, notesViewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> notificationsEnabled = notificationsAllowed(context)
                    Lifecycle.Event.ON_START -> notesViewModel.onIntent(NotesIntent.ForegroundChanged(visible = true))
                    Lifecycle.Event.ON_STOP -> notesViewModel.onIntent(NotesIntent.ForegroundChanged(visible = false))
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationsEnabled = granted
            settingsViewModel.onIntent(SettingsIntent.StartModelDownload)
        }
    val currentLauncher by rememberUpdatedState(permissionLauncher)

    val intents =
        remember(context) {
            FeatureIntents(
                shell = { shellViewModel.onIntent(it) },
                notes = { notesViewModel.onIntent(it) },
                settings = { intent ->
                    if (intent == SettingsIntent.StartModelDownload && needsNotificationPermission(context)) {
                        currentLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        settingsViewModel.onIntent(intent)
                    }
                },
                quiz = { quizViewModel.onIntent(it) },
            )
        }

    NotesScreen(
        states =
            FeatureStates(
                shell = shell,
                notes = notes,
                settings = settings.copy(notificationsEnabled = notificationsEnabled),
                quiz = quiz,
            ),
        intents = intents,
        onExit = { activity?.finish() },
        onEnableNotifications = { openNotificationSettings(context) },
    )
}

private fun notificationsAllowed(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

private fun openNotificationSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotesScreen(
    states: FeatureStates,
    intents: FeatureIntents,
    onExit: () -> Unit,
    onEnableNotifications: () -> Unit = {},
) {
    val containerSize = LocalWindowInfo.current.containerDpSize
    val windowClass = MyNoteWindowClass.fromWidth(containerSize.width)
    val heightClass = MyNoteHeightClass.fromHeight(containerSize.height)

    val snackbarHostState = remember { SnackbarHostState() }
    RootSnackbars(states, intents, snackbarHostState)

    val drafts = remember { NotesDrafts() }

    MyNoteTheme(windowClass = windowClass, heightClass = heightClass, fontScale = states.shell.fontScale) {
        NotesExitEffects(states.shell, snackbarHostState, intents.shell, onExit)
        NotesStart(
            states = states,
            intents = intents,
            snackbarHostState = snackbarHostState,
            drafts = drafts,
            onEnableNotifications = onEnableNotifications,
        )
    }
}

/**
 * 스낵바는 여기 한 곳이 호스트 하나로 받는다(FM-16) — 시작 분기 · 창 등급 분기 위라 설정에 있을 때 난 실패도 바로 뜨고,
 * 접기 · 펴기에도 보여 주던 것을 다시 띄우지 않는다. 다 보이면 신호를 낸 ViewModel 에 본 것을 알린다.
 */
@Composable
private fun RootSnackbars(
    states: FeatureStates,
    intents: FeatureIntents,
    host: SnackbarHostState,
) {
    val undoLabel = stringResource(DsR.string.ds_action_undo)
    val current by rememberUpdatedState(intents)
    val notes = states.notes
    LaunchedEffect(notes.errorId) {
        val id = notes.errorId
        if (id > 0) {
            notes.errorMessage?.let { host.showSnackbar(it) }
            current.notes(NotesIntent.SignalShown(NotesSignal.Error, id))
        }
    }
    LaunchedEffect(notes.infoId) {
        val id = notes.infoId
        if (id > 0) {
            notes.infoMessage?.let { host.showSnackbar(it) }
            current.notes(NotesIntent.SignalShown(NotesSignal.Info, id))
        }
    }
    val quiz = states.quiz
    LaunchedEffect(quiz.quizUndoId) {
        val id = quiz.quizUndoId
        val message = quiz.quizUndoMessage
        if (id > 0 && message != null) {
            val result =
                host.showSnackbar(
                    message = message,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) current.quiz(QuizIntent.UndoExit)
            current.quiz(QuizIntent.SignalShown(NotesSignal.QuizUndo, id))
        }
    }
    NoticeSnackbar(notes.notice, host, undoLabel, onUndo = { current.notes(it) }) { id ->
        current.notes(NotesIntent.SignalShown(NotesSignal.Notice, id))
    }
    NoticeSnackbar(states.settings.notice, host, undoLabel, onUndo = { current.settings(it) }) { id ->
        current.settings(SettingsIntent.SignalShown(NotesSignal.Notice, id))
    }
}

@Composable
private fun <E> NoticeSnackbar(
    notice: NoticeUi<E>?,
    host: SnackbarHostState,
    undoLabel: String,
    onUndo: (E) -> Unit,
    onFinish: (Int) -> Unit,
) {
    val currentUndo by rememberUpdatedState(onUndo)
    val currentFinish by rememberUpdatedState(onFinish)
    LaunchedEffect(notice?.id) {
        if (notice != null) {
            val result =
                host.showSnackbar(
                    message = notice.message,
                    actionLabel = if (notice.undo != null) undoLabel else null,
                    duration = SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) notice.undo?.let(currentUndo)
            currentFinish(notice.id)
        }
    }
}

private val previewRepo = RepoConnection("octo-learner", "android-notes", "main", "notes/index.json", isPublic = true)

private val previewStates: FeatureStates =
    run {
        val android =
            persistentListOf(
                NoteUi("compose-state", "Compose 상태 관리 정리", "2026.06.12"),
                NoteUi("nav3", "Navigation 3 첫인상", "2026.06.18"),
            )
        val categories =
            persistentListOf(
                CategoryUi("Android", android),
                CategoryUi("Kotlin", persistentListOf(NoteUi("coroutines", "코루틴 디스패처 메모", "2026.05.30"))),
            )
        val notes =
            NotesUiState(
                categories = categories,
                visibleCategories = categories,
                recentNotes =
                    persistentListOf(
                        RecentNoteUi(android[1], "Android"),
                        RecentNoteUi(android[0], "Android"),
                    ),
                expanded = persistentMapOf("Android" to true),
                isLoading = false,
                connection = ConnectionState.Connected(SourceId("s1"), previewRepo),
                notesLoaded = true,
            )
        FeatureStates(
            shell = ShellUiState(connection = notes.connection, seedPending = false),
            notes = notes,
        )
    }

private val previewIntents = FeatureIntents(shell = {}, notes = {}, settings = {}, quiz = {})

@Preview(name = "Notes · Compact", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun NotesScreenCompactPreview() {
    NotesScreen(
        states = previewStates,
        intents = previewIntents,
        onExit = {},
    )
}

@Preview(name = "Notes · Expanded", showBackground = true, widthDp = 900, heightDp = 720)
@Composable
private fun NotesScreenExpandedPreview() {
    NotesScreen(
        states = previewStates,
        intents = previewIntents,
        onExit = {},
    )
}

@Preview(name = "Notes · 짧은 창", showBackground = true, widthDp = 760, heightDp = 420)
@Composable
private fun NotesScreenShortPreview() {
    NotesScreen(
        states = previewStates,
        intents = previewIntents,
        onExit = {},
    )
}
