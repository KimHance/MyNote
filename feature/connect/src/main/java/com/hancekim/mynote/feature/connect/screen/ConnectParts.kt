package com.hancekim.mynote.feature.connect.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.ui.screen.connect.FieldBox
import com.hancekim.mynote.core.ui.screen.connect.KeepWordsText
import com.hancekim.mynote.feature.connect.R

@Composable
internal fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    KeepWordsText(
        text = text,
        style =
            MyNoteTheme.textStyles.caption
                .copy(fontWeight = FontWeight.W600),
        color = MyNoteTheme.colors.text2,
        modifier = modifier,
    )
}

@Composable
internal fun LinkField(
    value: String,
    onValueChange: (String) -> Unit,
    locked: Boolean,
    warn: Boolean,
    onGo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    FieldBox(locked = locked, warn = warn, modifier = modifier) {
        Box(Modifier.weight(1f).padding(end = 8.dp)) {
            if (value.isEmpty()) {
                KeepWordsText(
                    text = stringResource(R.string.connect_link_hint),
                    style = MyNoteTheme.textStyles.mono,
                    color = colors.text3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = !locked,
                singleLine = true,
                textStyle = MyNoteTheme.textStyles.mono.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Go,
                    ),
                keyboardActions = KeyboardActions(onGo = { onGo() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
