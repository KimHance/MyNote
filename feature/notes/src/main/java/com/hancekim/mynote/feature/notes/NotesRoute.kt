package com.hancekim.mynote.feature.notes

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.screen.ModelConfirmDialog
import com.hancekim.mynote.feature.notes.shell.NotesShell
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import com.hancekim.mynote.core.designsystem.R as DsR

@Composable
fun NotesRoute(viewModel: NotesViewModel = hiltViewModel()) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsEnabled by remember { mutableStateOf(notificationsAllowed(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) notificationsEnabled = notificationsAllowed(context)
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationsEnabled = granted
            viewModel.onIntent(NotesIntent.StartModelDownload)
        }

    NotesScreen(
        uiState = uiState.copy(notificationsEnabled = notificationsEnabled),
        onIntent = { intent ->
            if (intent == NotesIntent.StartModelDownload && needsNotificationPermission(context)) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onIntent(intent)
            }
        },
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
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
    onEnableNotifications: () -> Unit = {},
) {
    val containerSize = LocalWindowInfo.current.containerDpSize
    val windowClass = MyNoteWindowClass.fromWidth(containerSize.width)
    val heightClass = MyNoteHeightClass.fromHeight(containerSize.height)

    val snackbarHostState = uiState.snackbarHostState
    val undoLabel = stringResource(DsR.string.ds_action_undo)
    LaunchedEffect(uiState.errorId) {
        if (uiState.errorId > 0) uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(uiState.infoId) {
        if (uiState.infoId > 0) uiState.infoMessage?.let { snackbarHostState.showSnackbar(it) }
    }
    val undoIntent by rememberUpdatedState(onIntent)
    LaunchedEffect(uiState.quizUndoId) {
        val message = uiState.quizUndoMessage
        if (uiState.quizUndoId > 0 && message != null) {
            val result =
                snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) undoIntent(NotesIntent.UndoQuizExit)
        }
    }

    var showDownloadConfirm by rememberSaveable { mutableStateOf(false) }

    val notice = uiState.notice
    LaunchedEffect(notice?.id) {
        if (notice != null) {
            val result =
                snackbarHostState.showSnackbar(
                    message = notice.message,
                    actionLabel = if (notice.undoable) undoLabel else null,
                    duration = SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) undoIntent(NotesIntent.UndoClearToken)
        }
    }

    MyNoteTheme(windowClass = windowClass, heightClass = heightClass, fontScale = uiState.fontScale) {
        NotesShell(
            uiState = uiState,
            onIntent = onIntent,
            snackbarHostState = snackbarHostState,
            onConfirmDownload = { showDownloadConfirm = true },
            onEnableNotifications = onEnableNotifications,
        )

        if (showDownloadConfirm) {
            ModelConfirmDialog(
                onDismiss = { showDownloadConfirm = false },
                onConfirm = {
                    showDownloadConfirm = false
                    onIntent(NotesIntent.StartModelDownload)
                },
            )
        }
    }
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
