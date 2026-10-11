package com.hancekim.mynote.feature.notes.screen.connect

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.ui.screen.connect.ActionDialog
import com.hancekim.mynote.feature.notes.R

@Composable
internal fun DisconnectDialog(
    notes: Int,
    bookmarks: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        DisconnectDialogContent(notes = notes, bookmarks = bookmarks, onDismiss = onDismiss, onConfirm = onConfirm)
    }
}

@Composable
internal fun DisconnectDialogContent(
    notes: Int,
    bookmarks: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ActionDialog(
        title = stringResource(R.string.notes_dialog_disconnect_title),
        body =
            if (notes == 0 && bookmarks == 0) {
                stringResource(R.string.notes_dialog_disconnect_empty)
            } else {
                stringResource(R.string.notes_dialog_disconnect_body, notes, bookmarks)
            },
        confirmLabel = stringResource(R.string.notes_dialog_disconnect_confirm),
        destructive = true,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Preview(name = "대화상자 · 끊기", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun DisconnectDialogPreview() {
    MyNoteTheme {
        DisconnectDialogContent(notes = 24, bookmarks = 3, onDismiss = {}, onConfirm = {})
    }
}

@Preview(name = "대화상자 · 끊기 · 개수 0", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun DisconnectDialogEmptyPreview() {
    MyNoteTheme {
        DisconnectDialogContent(notes = 0, bookmarks = 0, onDismiss = {}, onConfirm = {})
    }
}
