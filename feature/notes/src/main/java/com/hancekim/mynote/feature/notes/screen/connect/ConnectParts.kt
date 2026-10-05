package com.hancekim.mynote.feature.notes.screen.connect

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.motion.rememberReduceMotion
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.TokenDraft
import com.hancekim.mynote.feature.notes.screen.QuietButton
import com.hancekim.mynote.feature.notes.screen.SecondaryButton

internal val FIELD_HEIGHT: Dp = 48.dp
private val FIELD_RADIUS: Dp = 12.dp
private const val FIELD_LOCKED_ALPHA = 0.55f
private const val WARN_BORDER_ALPHA = 0.5f
private const val WARN_FILL_ALPHA = 0.08f
private const val WARN_LINE_ALPHA = 0.2f

@Composable
internal fun monoAnnotated(
    @StringRes res: Int,
    vararg args: String,
): AnnotatedString {
    val text = keepWordsResource(res, *args)
    val mono = SpanStyle(fontFamily = MyNoteTheme.textStyles.mono.fontFamily, color = MyNoteTheme.colors.bodyText)
    return remember(text, mono) {
        buildAnnotatedString {
            append(text)
            var from = 0
            args.forEach { arg ->
                val index = text.indexOf(arg, from)
                if (index >= 0) {
                    addStyle(mono, index, index + arg.length)
                    from = index + arg.length
                }
            }
        }
    }
}

@Composable
internal fun ProgressRing(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    val colors = MyNoteTheme.colors
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) {
        CircularProgressIndicator(
            progress = { RING_STILL },
            modifier = modifier.size(size),
            color = colors.accent,
            trackColor = colors.border,
            strokeWidth = 2.dp,
        )
    } else {
        CircularProgressIndicator(
            modifier = modifier.size(size),
            color = colors.accent,
            trackColor = colors.border,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
internal fun ProgressBar(modifier: Modifier = Modifier) {
    val colors = MyNoteTheme.colors
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) {
        LinearProgressIndicator(
            progress = { BAR_STILL },
            modifier = modifier,
            color = colors.accent,
            trackColor = colors.border,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier,
            color = colors.accent,
            trackColor = colors.border,
        )
    }
}

private const val RING_STILL = 0.3f
private const val BAR_STILL = 0.35f

@Composable
internal fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style =
            MyNoteTheme.textStyles.caption
                .copy(fontWeight = FontWeight.W600),
        color = MyNoteTheme.colors.text2,
        modifier = modifier,
    )
}

@Composable
internal fun FieldBox(
    locked: Boolean,
    warn: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(FIELD_RADIUS)
    val border =
        if (warn) {
            colors.warn.copy(
                alpha = WARN_BORDER_ALPHA,
            )
        } else {
            colors.accent.copy(alpha = WARN_BORDER_ALPHA)
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(FIELD_HEIGHT)
                .alpha(if (locked) FIELD_LOCKED_ALPHA else 1f)
                .clip(shape)
                .background(colors.surface2)
                .border(1.dp, if (locked) colors.border else border, shape)
                .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
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
                Text(
                    text = keepWordsResource(R.string.notes_connect_link_hint),
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

@Composable
internal fun TokenField(
    draft: TokenDraft,
    locked: Boolean,
    warn: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val colors = MyNoteTheme.colors
    FieldBox(locked = locked, warn = warn, modifier = modifier) {
        Box(Modifier.weight(1f)) {
            if (draft.text.isEmpty()) {
                Text(
                    text = keepWordsResource(R.string.notes_settings_token_placeholder),
                    style = MyNoteTheme.textStyles.mono,
                    color = colors.text3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = draft.text,
                onValueChange = { draft.text = it },
                enabled = !locked,
                singleLine = true,
                textStyle = MyNoteTheme.textStyles.mono.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                visualTransformation =
                    if (draft.revealed) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions = KeyboardActions(onDone = { if (!draft.isBlank) onDone() }),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            )
        }
        QuietButton(
            text =
                keepWordsResource(
                    if (draft.revealed) R.string.notes_settings_token_hide else R.string.notes_settings_token_show,
                ),
            contentDescription =
                keepWordsResource(
                    if (draft.revealed) {
                        R.string.notes_settings_token_hide_a11y
                    } else {
                        R.string.notes_settings_token_show_a11y
                    },
                ),
            enabled = !locked,
            onClick = { draft.revealed = !draft.revealed },
        )
    }
}

@Composable
internal fun TokenHowTo(modifier: Modifier = Modifier) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(FIELD_RADIUS)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface2)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HowToLine(R.string.notes_token_howto_kind_label, R.string.notes_token_howto_kind)
        HowToLine(R.string.notes_token_howto_scope_label, R.string.notes_token_howto_scope)
        HowToLine(R.string.notes_token_howto_perm_label, R.string.notes_token_howto_perm)
    }
}

@Composable
private fun HowToLine(
    @StringRes label: Int,
    @StringRes value: Int,
) {
    val colors = MyNoteTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = keepWordsResource(label),
            style = MyNoteTheme.textStyles.bodySmall,
            color = colors.text2,
        )
        Text(
            text = "·",
            style = MyNoteTheme.textStyles.bodySmall,
            color = colors.text3,
        )
        Text(
            text = keepWordsResource(value),
            style =
                MyNoteTheme.textStyles.bodySmall
                    .copy(fontWeight = FontWeight.W600),
            color = colors.text,
        )
    }
}

@Composable
internal fun TokenCreateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SecondaryButton(
        text = keepWordsResource(R.string.notes_token_create),
        onClick = onClick,
        trailingIcon = ConnectIcons.External,
        modifier = modifier,
    )
}

@Composable
internal fun NoticeBox(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(FIELD_RADIUS)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.warn.copy(alpha = WARN_FILL_ALPHA))
                .border(1.dp, colors.warn.copy(alpha = WARN_LINE_ALPHA), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = ConnectIcons.Alert,
            contentDescription = null,
            tint = colors.warn,
            modifier = Modifier.padding(top = 2.dp).size(18.dp),
        )
        Text(
            text = text,
            style = MyNoteTheme.textStyles.bodySmall,
            color = colors.warnText,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun VerifyingLine(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProgressRing()
        Text(
            text = text,
            style =
                MyNoteTheme.textStyles.caption
                    .copy(fontWeight = FontWeight.W500),
            color = MyNoteTheme.colors.text2,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}
