package com.hancekim.mynote.feature.notes.screen.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hancekim.mynote.core.designsystem.component.GlassSurface
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.ConfirmKind
import com.hancekim.mynote.feature.notes.element.ConfirmUi
import com.hancekim.mynote.feature.notes.screen.PrimaryButton
import com.hancekim.mynote.feature.notes.screen.SecondaryButton
import com.hancekim.mynote.feature.notes.screen.SolidDestructiveButton
import com.hancekim.mynote.core.designsystem.R as DsR

internal val CONNECT_DIALOG_MAX_WIDTH: Dp = 420.dp
private val DIALOG_RADIUS: Dp = 26.dp
private val DIALOG_MARGIN: Dp = 24.dp

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
            title = R.string.notes_dialog_switch_title
            confirmLabel = R.string.notes_dialog_switch_confirm
        }

        ConfirmKind.Adopt -> {
            title = R.string.notes_dialog_adopt_title
            confirmLabel = R.string.notes_dialog_adopt_confirm
        }
    }
    val body =
        when (confirm.kind) {
            ConfirmKind.Switch -> {
                val base = stringResource(R.string.notes_dialog_switch_body, confirm.notes, confirm.bookmarks)
                if (confirm.dropsToken) base + " " + stringResource(R.string.notes_dialog_switch_token) else base
            }

            ConfirmKind.Adopt -> {
                stringResource(R.string.notes_dialog_adopt_body, confirm.notes)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionDialog(
    title: String,
    body: String,
    confirmLabel: String,
    destructive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(DIALOG_MARGIN),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            radius = DIALOG_RADIUS,
            backdrop = null,
            modifier = Modifier.widthIn(max = CONNECT_DIALOG_MAX_WIDTH).fillMaxWidth(),
            tint = colors.glassPanel,
        ) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)) {
                Column(
                    modifier =
                        Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    KeepWordsText(
                        text = title,
                        style = MyNoteTheme.textStyles.sectionTitle,
                        color = colors.text,
                        modifier = Modifier.semantics { heading() },
                    )
                    KeepWordsText(
                        text = body,
                        style = MyNoteTheme.textStyles.bodySmall,
                        color = colors.bodyText,
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SecondaryButton(text = stringResource(DsR.string.ds_action_cancel), onClick = onDismiss)
                    if (destructive) {
                        SolidDestructiveButton(text = confirmLabel, onClick = onConfirm)
                    } else {
                        PrimaryButton(text = confirmLabel, onClick = onConfirm)
                    }
                }
            }
        }
    }
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
