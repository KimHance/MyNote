package com.hancekim.mynote.feature.notes.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hancekim.mynote.core.designsystem.component.GlassSurface
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.ui.screen.DestructiveButton
import com.hancekim.mynote.core.ui.screen.PrimaryButton
import com.hancekim.mynote.core.ui.screen.SecondaryButton
import com.hancekim.mynote.feature.notes.R
import com.kyant.backdrop.Backdrop
import com.hancekim.mynote.core.designsystem.R as DsR
import com.hancekim.mynote.core.ui.R as UiR

@Composable
fun ModelConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    backdrop: Backdrop? = null,
) {
    ModelDialog(
        title = stringResource(R.string.notes_model_confirm_title),
        body = stringResource(R.string.notes_model_confirm_body),
        dismissLabel = stringResource(R.string.notes_model_confirm_later),
        confirmLabel = stringResource(UiR.string.ui_model_download),
        destructive = false,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        backdrop = backdrop,
    )
}

@Composable
internal fun ModelDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    backdrop: Backdrop? = null,
) {
    ModelDialog(
        title = stringResource(R.string.notes_model_delete_title),
        body = stringResource(R.string.notes_model_delete_body),
        dismissLabel = stringResource(DsR.string.ds_action_cancel),
        confirmLabel = stringResource(R.string.notes_model_delete),
        destructive = true,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        backdrop = backdrop,
    )
}

@Composable
internal fun StudyLogClearDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    backdrop: Backdrop? = null,
) {
    ModelDialog(
        title = stringResource(R.string.notes_settings_studylog_clear_title),
        body = stringResource(R.string.notes_settings_studylog_clear_body),
        dismissLabel = stringResource(DsR.string.ds_action_cancel),
        confirmLabel = stringResource(R.string.notes_settings_studylog_clear_confirm),
        destructive = true,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        backdrop = backdrop,
    )
}

@Composable
private fun ModelDialog(
    title: String,
    body: String,
    dismissLabel: String,
    confirmLabel: String,
    destructive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    backdrop: Backdrop?,
) {
    val colors = MyNoteTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            radius = DIALOG_RADIUS,
            backdrop = backdrop,
            modifier = Modifier.widthIn(max = DIALOG_MAX_WIDTH),
            tint = colors.glassPanel,
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.material3.Text(
                    text = title,
                    style = MyNoteTheme.textStyles.sectionTitle,
                    color = colors.text,
                )
                androidx.compose.material3.Text(
                    text = body,
                    style = MyNoteTheme.textStyles.bodySmall,
                    color = colors.bodyText,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SecondaryButton(text = dismissLabel, onClick = onDismiss)
                    if (destructive) {
                        DestructiveButton(text = confirmLabel, onClick = onConfirm)
                    } else {
                        PrimaryButton(text = confirmLabel, onClick = onConfirm)
                    }
                }
            }
        }
    }
}

private val DIALOG_RADIUS = 26.dp
private val DIALOG_MAX_WIDTH = 360.dp

@Preview(name = "모델 받기 확인", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 400)
@Composable
private fun ModelConfirmDialogPreview() {
    MyNoteTheme {
        ModelConfirmDialog(onDismiss = {}, onConfirm = {})
    }
}
