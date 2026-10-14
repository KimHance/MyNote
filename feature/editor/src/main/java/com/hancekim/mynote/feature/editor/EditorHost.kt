package com.hancekim.mynote.feature.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.ui.screen.DestructiveButton
import com.hancekim.mynote.core.ui.screen.SecondaryButton
import com.hancekim.mynote.core.ui.screen.connect.ActionDialog
import com.hancekim.mynote.core.ui.R as UiR

// 골격 — 대화상자 모양은 편집기 · 가져오기 화면 단위가 채운다. 자리(셸이 늘 컴포즈한다)와 배선만 여기서 정한다.

/**
 * 편집기 기능이 셸 위에 띄우는 것 — 시스템 파일 고르기 런처 · 가져오기 그릇 · 나가기 확인 · 지우기 확인.
 *
 * 셸이 시작 분기 · 창 등급 분기 위에서 늘 컴포즈한다. 런처가 사라지는 자리(대화상자 안)에 있으면 파일 고르기 결과가 돌아올 곳이
 * 없다(brief 표면 제약). 대화상자는 각자 창이라 셸 뒤로가기 7단과 겹치지 않는다.
 */
@Composable
fun EditorHost(
    state: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
) {
    val currentIntent by rememberUpdatedState(onIntent)
    // 받는 파일은 표시 이름 확장자로 판정한다 — `.md` 를 octet-stream · text/plain 으로 주는 제공자가 많다(LN-6 (b))
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            currentIntent(EditorIntent.FileChosen(uri?.toString()))
        }
    val seenPick = remember { intArrayOf(state.filePickRequestId) }
    LaunchedEffect(state.filePickRequestId) {
        if (state.filePickRequestId != seenPick[0]) {
            seenPick[0] = state.filePickRequestId
            launcher.launch(arrayOf(ANY_FILE))
        }
    }

    if (state.importDialog != ImportDialogUi.Closed) {
        ImportChooser(reading = state.importDialog == ImportDialogUi.Reading, onIntent = onIntent)
    }
    if (state.exitConfirm) {
        ExitConfirm(onIntent = onIntent)
    }
    state.deleteConfirmNoteId?.let {
        Dialog(
            onDismissRequest = { onIntent(EditorIntent.DeleteAnswered(confirm = false)) },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            ActionDialog(
                title = stringResource(R.string.editor_delete_title),
                body = stringResource(R.string.editor_delete_body),
                confirmLabel = stringResource(UiR.string.ui_action_delete),
                destructive = true,
                onDismiss = { onIntent(EditorIntent.DeleteAnswered(confirm = false)) },
                onConfirm = { onIntent(EditorIntent.DeleteAnswered(confirm = true)) },
            )
        }
    }
}

@Composable
private fun ImportChooser(
    reading: Boolean,
    onIntent: (EditorIntent) -> Unit,
) {
    SkeletonDialog(onDismiss = { onIntent(EditorIntent.DismissImport) }) {
        DialogText(stringResource(R.string.editor_import_title), stringResource(R.string.editor_import_body))
        if (reading) {
            Text(text = stringResource(R.string.editor_import_reading), color = MyNoteTheme.colors.text3)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            SecondaryButton(
                text = stringResource(R.string.editor_import_file),
                onClick = { onIntent(EditorIntent.ChooseFile) },
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = stringResource(R.string.editor_import_paste),
                onClick = { onIntent(EditorIntent.ChoosePaste) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ExitConfirm(onIntent: (EditorIntent) -> Unit) {
    SkeletonDialog(onDismiss = { onIntent(EditorIntent.ExitAnswered(discard = false)) }) {
        DialogText(stringResource(R.string.editor_exit_title), stringResource(R.string.editor_exit_body))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(
                text = stringResource(R.string.editor_exit_keep),
                onClick = { onIntent(EditorIntent.ExitAnswered(discard = false)) },
            )
            DestructiveButton(
                text = stringResource(R.string.editor_exit_discard),
                onClick = { onIntent(EditorIntent.ExitAnswered(discard = true)) },
            )
        }
    }
}

@Composable
private fun SkeletonDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MyNoteTheme.colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 400.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun DialogText(
    title: String,
    body: String,
) {
    val colors = MyNoteTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, style = MyNoteTheme.textStyles.sectionTitle, color = colors.text)
        Text(text = body, style = MyNoteTheme.textStyles.bodySmall, color = colors.bodyText)
    }
}

private const val ANY_FILE = "*/*"
