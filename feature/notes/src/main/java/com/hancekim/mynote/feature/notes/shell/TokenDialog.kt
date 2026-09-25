package com.hancekim.mynote.feature.notes.shell

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.core.designsystem.R as DsR

/**
 * GitHub 토큰 입력 다이얼로그. feature 전용 Composable — design-system 재사용 컴포넌트 아님.
 * PasswordVisualTransformation 으로 토큰 값을 가린다.
 */
@Composable
internal fun TokenDialog(
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
                text = stringResource(R.string.notes_token_title),
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
                        text = stringResource(R.string.notes_token_label),
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
                    text = stringResource(DsR.string.ds_action_save),
                    style = MyNoteTheme.textStyles.treeLabel,
                    color = colors.accent,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(DsR.string.ds_action_cancel),
                    style = MyNoteTheme.textStyles.treeLabel,
                    color = colors.text2,
                )
            }
        },
        containerColor = colors.surface,
    )
}
