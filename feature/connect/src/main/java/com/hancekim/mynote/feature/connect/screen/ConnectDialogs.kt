package com.hancekim.mynote.feature.connect.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.ui.screen.connect.ActionDialog
import com.hancekim.mynote.feature.connect.R
import com.hancekim.mynote.feature.connect.element.ConfirmKind
import com.hancekim.mynote.feature.connect.element.ConfirmUi

@Composable
internal fun ConnectConfirmDialog(
    confirm: ConfirmUi,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ConnectConfirmContent(confirm = confirm, onDismiss = onDismiss, onConfirm = onConfirm)
    }
}

@Composable
internal fun ConnectConfirmContent(
    confirm: ConfirmUi,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title: Int
    val confirmLabel: Int
    when (confirm.kind) {
        ConfirmKind.Switch -> {
            title = R.string.connect_dialog_switch_title
            confirmLabel = R.string.connect_dialog_switch_confirm
        }

        ConfirmKind.Adopt -> {
            title = R.string.connect_dialog_adopt_title
            confirmLabel = R.string.connect_dialog_adopt_confirm
        }
    }
    val body =
        when (confirm.kind) {
            ConfirmKind.Switch -> {
                val base = stringResource(R.string.connect_dialog_switch_body, confirm.notes, confirm.bookmarks)
                if (confirm.dropsToken) base + " " + stringResource(R.string.connect_dialog_switch_token) else base
            }

            ConfirmKind.Adopt -> {
                stringResource(R.string.connect_dialog_adopt_body, confirm.notes)
            }
        }
    ActionDialog(
        title = stringResource(title),
        body = body,
        confirmLabel = stringResource(confirmLabel),
        destructive = confirm.kind == ConfirmKind.Switch,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Preview(name = "대화상자 · 바꾸기", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun SwitchDialogPreview() {
    MyNoteTheme {
        ConnectConfirmContent(
            confirm = ConfirmUi(ConfirmKind.Switch, notes = 24, bookmarks = 3, dropsToken = false),
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview(name = "대화상자 · 바꾸기 · 토큰", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun SwitchDialogTokenPreview() {
    MyNoteTheme {
        ConnectConfirmContent(
            confirm = ConfirmUi(ConfirmKind.Switch, notes = 24, bookmarks = 3, dropsToken = true),
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview(name = "대화상자 · 바꾸기 · 852", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 852, heightDp = 800)
@Composable
private fun SwitchDialogWidePreview() {
    MyNoteTheme(windowClass = com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass.Medium) {
        ConnectConfirmContent(
            confirm = ConfirmUi(ConfirmKind.Switch, notes = 24, bookmarks = 3, dropsToken = false),
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview(
    name = "대화상자 · 바꾸기 · 가로 · ×1.3",
    showBackground = true,
    backgroundColor = 0xFF0E1217,
    widthDp = 800,
    heightDp = 360,
    fontScale = 1.3f,
)
@Composable
private fun SwitchDialogLandscapeLargePreview() {
    MyNoteTheme(
        windowClass = com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass.Medium,
        heightClass = com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass.Compact,
    ) {
        ConnectConfirmContent(
            confirm = ConfirmUi(ConfirmKind.Switch, notes = 24, bookmarks = 3, dropsToken = false),
            onDismiss = {},
            onConfirm = {},
        )
    }
}
