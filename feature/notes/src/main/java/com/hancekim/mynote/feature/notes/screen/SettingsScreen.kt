package com.hancekim.mynote.feature.notes.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale
import com.hancekim.mynote.core.designfoundation.token.MyNoteRadius
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.PressIndication
import com.hancekim.mynote.core.designsystem.component.riseIn
import com.hancekim.mynote.core.designsystem.motion.rememberReduceMotion
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.element.maskedTokenTail
import com.hancekim.mynote.feature.notes.element.modelCardUi
import com.hancekim.mynote.core.designsystem.R as DsR

@Composable
internal fun SettingsScreen(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
    onEnableNotifications: () -> Unit = {},
) {
    val colors = MyNoteTheme.colors
    val adaptive = MyNoteTheme.adaptive
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BackHandler(enabled = uiState.tokenEditing) { onIntent(NotesIntent.CancelTokenEdit) }

    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    if (confirmDelete) {
        ModelDeleteDialog(
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onIntent(NotesIntent.DeleteModel)
            },
        )
    }

    Surface(modifier = modifier.fillMaxSize(), color = colors.bg) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = CONTENT_MAX_WIDTH)
                        .fillMaxWidth()
                        .padding(horizontal = adaptive.page)
                        .padding(top = adaptive.padY + 4.dp, bottom = adaptive.padY + 32.dp + navBottom),
                verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
            ) {
                ReadingSection(uiState = uiState, onIntent = onIntent)
                StudyLogSection(enabled = uiState.studyLogEnabled, onIntent = onIntent)
                ModelSection(
                    card = modelCardUi(uiState.modelStatus),
                    notificationsEnabled = uiState.notificationsEnabled,
                    onIntent = { intent ->
                        if (intent == NotesIntent.DeleteModel) confirmDelete = true else onIntent(intent)
                    },
                    onEnableNotifications = onEnableNotifications,
                )
                GithubSection(uiState = uiState, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun ReadingSection(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
) {
    SettingsSection(label = stringResource(R.string.notes_settings_section_reading)) {
        SettingsCard {
            FontScaleRow(
                large = uiState.fontScale == MyNoteFontScale.Large,
                onChange = { large ->
                    onIntent(
                        NotesIntent.SetFontScale(
                            if (large) MyNoteFontScale.Large else MyNoteFontScale.Normal,
                        ),
                    )
                },
            )
            HorizontalDivider(
                color = MyNoteTheme.colors.border,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            PreviewBlock()
        }
    }
}

@Composable
private fun FontScaleRow(
    large: Boolean,
    onChange: (Boolean) -> Unit,
) {
    SettingsSwitchRow(
        title = stringResource(R.string.notes_settings_font_title),
        description = stringResource(R.string.notes_settings_font_desc),
        checked = large,
        onChange = onChange,
    )
}

@Composable
private fun StudyLogSection(
    enabled: Boolean,
    onIntent: (NotesIntent) -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    if (confirmClear) {
        StudyLogClearDialog(
            onDismiss = { confirmClear = false },
            onConfirm = {
                confirmClear = false
                onIntent(NotesIntent.ClearStudyLog)
            },
        )
    }
    SettingsSection(label = stringResource(R.string.notes_settings_section_studylog)) {
        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.notes_settings_studylog_title),
                description = stringResource(R.string.notes_settings_studylog_desc),
                checked = enabled,
                onChange = { onIntent(NotesIntent.SetStudyLogEnabled(it)) },
            )
            HorizontalDivider(
                color = MyNoteTheme.colors.border,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Box(Modifier.padding(16.dp)) {
                SecondaryButton(
                    text = stringResource(R.string.notes_settings_studylog_clear),
                    onClick = { confirmClear = true },
                )
            }
        }
        Text(
            text = stringResource(R.string.notes_settings_studylog_help),
            style = MyNoteTheme.textStyles.caption,
            color = MyNoteTheme.colors.text3,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = MyNoteTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MyNoteTheme.textStyles.cardTitle,
                color = colors.text,
            )
            Text(
                text = description,
                style = MyNoteTheme.textStyles.caption,
                color = colors.text3,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors =
                SwitchDefaults.colors(
                    checkedTrackColor = colors.accent,
                    checkedThumbColor = colors.bg,
                    checkedBorderColor = Color.Transparent,
                    uncheckedTrackColor = colors.surface2,
                    uncheckedThumbColor = colors.text3,
                    uncheckedBorderColor = colors.border,
                ),
        )
    }
}

@Composable
private fun PreviewBlock() {
    val colors = MyNoteTheme.colors
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.notes_settings_preview),
            style = MyNoteTheme.textStyles.overline,
            color = colors.text3,
        )
        Text(
            text = AnnotatedString.fromHtml(stringResource(R.string.notes_settings_preview_text)),
            style = MyNoteTheme.textStyles.body,
            color = colors.bodyText,
        )
    }
}

@Composable
private fun GithubSection(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
) {
    val colors = MyNoteTheme.colors
    SettingsSection(label = stringResource(R.string.notes_settings_section_github)) {
        SettingsCard(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconBox(icon = MyNoteIcons.Key)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.notes_settings_token_title),
                        style = MyNoteTheme.textStyles.cardTitle.copy(fontWeight = FontWeight.W500),
                        color = colors.text,
                    )
                    TokenStatusLine(uiState = uiState)
                }
            }

            if (uiState.tokenEditing) {
                TokenEditor(
                    onCancel = { onIntent(NotesIntent.CancelTokenEdit) },
                    onSave = { onIntent(NotesIntent.SaveToken(it)) },
                )
            } else {
                TokenActions(hasToken = uiState.hasToken, onIntent = onIntent)
            }
        }
        Text(
            text = stringResource(R.string.notes_settings_token_help),
            style = MyNoteTheme.textStyles.caption,
            color = colors.text3,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun TokenStatusLine(uiState: NotesUiState) {
    val colors = MyNoteTheme.colors
    val tail = maskedTokenTail(uiState.maskedToken)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (uiState.hasToken) colors.correct else colors.text3),
        )
        Text(
            text =
                stringResource(
                    if (uiState.hasToken) {
                        R.string.notes_settings_token_connected
                    } else {
                        R.string.notes_settings_token_none
                    },
                ),
            style = MyNoteTheme.textStyles.caption.copy(fontWeight = FontWeight.W500),
            color = colors.bodyText,
            maxLines = 1,
        )
        if (uiState.hasToken) {
            val masked = uiState.maskedToken.orEmpty()
            Text(
                text = masked,
                style = MyNoteTheme.textStyles.monoSmall,
                color = colors.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier.semantics {
                        if (tail != null) contentDescription = "끝자리 $tail"
                    },
            )
        } else {
            Text(
                text = stringResource(R.string.notes_settings_token_none_desc),
                style = MyNoteTheme.textStyles.caption,
                color = colors.text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TokenActions(
    hasToken: Boolean,
    onIntent: (NotesIntent) -> Unit,
) {
    Row(
        modifier = Modifier.padding(start = ACTION_INDENT, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (hasToken) {
            SecondaryButton(
                text = stringResource(R.string.notes_settings_token_change),
                onClick = { onIntent(NotesIntent.StartTokenEdit) },
            )
            DestructiveButton(
                text = stringResource(R.string.notes_settings_token_clear),
                onClick = { onIntent(NotesIntent.ClearToken) },
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.notes_settings_token_enter),
                onClick = { onIntent(NotesIntent.StartTokenEdit) },
            )
        }
    }
}

@Composable
private fun TokenEditor(
    onCancel: () -> Unit,
    onSave: (String) -> Unit,
) {
    val colors = MyNoteTheme.colors
    var draft by rememberSaveable { mutableStateOf("") }
    var revealed by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val fieldInView = remember { BringIntoViewRequester() }
    val reduceMotion = rememberReduceMotion()
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(imeBottom) { if (imeBottom > 0) fieldInView.bringIntoView() }

    Column(
        modifier =
            Modifier
                .then(if (reduceMotion) Modifier else Modifier.riseIn())
                .bringIntoViewRequester(fieldInView)
                .padding(start = ACTION_INDENT, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (draft.isEmpty()) {
                    Text(
                        text = stringResource(R.string.notes_settings_token_placeholder),
                        style = MyNoteTheme.textStyles.mono,
                        color = colors.text3,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = MyNoteTheme.textStyles.mono.copy(color = colors.text),
                    cursorBrush = SolidColor(colors.accent),
                    visualTransformation =
                        if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions = KeyboardActions(onDone = { if (draft.isNotBlank()) onSave(draft) }),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                )
            }
            QuietButton(
                text =
                    stringResource(
                        if (revealed) R.string.notes_settings_token_hide else R.string.notes_settings_token_show,
                    ),
                contentDescription =
                    stringResource(
                        if (revealed) {
                            R.string.notes_settings_token_hide_a11y
                        } else {
                            R.string.notes_settings_token_show_a11y
                        },
                    ),
                onClick = { revealed = !revealed },
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.notes_settings_token_local_only),
                style = MyNoteTheme.textStyles.caption,
                color = colors.text3,
                modifier = Modifier.weight(1f),
            )
            QuietButton(text = stringResource(DsR.string.ds_action_cancel), onClick = onCancel)
            PrimaryButton(
                text = stringResource(DsR.string.ds_action_save),
                enabled = draft.isNotBlank(),
                onClick = { onSave(draft) },
            )
        }
    }
}

@Composable
internal fun SettingsSection(
    label: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MyNoteTheme.textStyles.overline,
            color = MyNoteTheme.colors.text3,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
        )
        content()
    }
}

@Composable
internal fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(MyNoteRadius.card)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.border, shape),
    ) {
        content()
    }
}

@Composable
internal fun IconBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color? = null,
) {
    val colors = MyNoteTheme.colors
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface2),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint ?: colors.text2,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
internal fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val indication =
        remember(colors, shape) { PressIndication(shape, colors.accent, colors.accentPressed, scale = 0.98f) }
    Box(
        modifier =
            Modifier
                .height(BUTTON_HEIGHT)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clip(shape)
                .background(colors.accent)
                .clickable(
                    enabled = enabled,
                    interactionSource = null,
                    indication = indication,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        ButtonLabel(text = text, color = colors.bg, weight = FontWeight.W700)
    }
}

@Composable
internal fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val indication = remember(colors, shape) { PressIndication(shape, pressColor = colors.border) }
    Box(
        modifier =
            Modifier
                .height(BUTTON_HEIGHT)
                .clip(shape)
                .background(colors.surface2)
                .border(1.dp, colors.border, shape)
                .clickable(interactionSource = null, indication = indication, role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        ButtonLabel(text = text, color = colors.text, weight = FontWeight.W500)
    }
}

@Composable
internal fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val indication =
        remember(colors, shape) { PressIndication(shape, pressColor = colors.wrong.copy(alpha = 0.10f)) }
    Box(
        modifier =
            Modifier
                .height(BUTTON_HEIGHT)
                .clip(shape)
                .clickable(interactionSource = null, indication = indication, role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        ButtonLabel(text = text, color = colors.wrong, weight = FontWeight.W500)
    }
}

@Composable
private fun QuietButton(
    text: String,
    onClick: () -> Unit,
    contentDescription: String? = null,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(10.dp)
    val indication = remember(colors, shape) { PressIndication(shape, pressColor = colors.border) }
    Box(
        modifier =
            Modifier
                .height(36.dp)
                .clip(shape)
                .clickable(interactionSource = null, indication = indication, role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp)
                .then(
                    if (contentDescription != null) {
                        Modifier.semantics { this.contentDescription = contentDescription }
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        ButtonLabel(text = text, color = colors.text2, weight = FontWeight.W500)
    }
}

@Composable
private fun ButtonLabel(
    text: String,
    color: Color,
    weight: FontWeight,
) {
    Text(
        text = text,
        style = MyNoteTheme.textStyles.button.copy(fontWeight = weight),
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
    )
}

private val CONTENT_MAX_WIDTH: Dp = 560.dp
private val SECTION_GAP: Dp = 28.dp
private val BUTTON_HEIGHT: Dp = 40.dp
private val ACTION_INDENT: Dp = 70.dp
private const val DISABLED_ALPHA = 0.4f

@Preview(name = "설정 · 토큰 있음", showBackground = true, widthDp = 400, heightDp = 860)
@Composable
private fun SettingsScreenPreview() {
    MyNoteTheme {
        SettingsScreen(
            uiState =
                NotesUiState(
                    hasToken = true,
                    maskedToken = "ghp_••••••••a3F9",
                    isLoading = false,
                ),
            onIntent = {},
        )
    }
}

@Preview(name = "설정 · 토큰 없음", showBackground = true, widthDp = 400, heightDp = 860)
@Composable
private fun SettingsScreenNoTokenPreview() {
    MyNoteTheme {
        SettingsScreen(uiState = NotesUiState(isLoading = false), onIntent = {})
    }
}

@Preview(name = "설정 · 편집 중 · 글자 크게", showBackground = true, widthDp = 400, heightDp = 860)
@Composable
private fun SettingsScreenEditingPreview() {
    MyNoteTheme(fontScale = MyNoteFontScale.Large) {
        SettingsScreen(
            uiState =
                NotesUiState(
                    hasToken = true,
                    maskedToken = "ghp_••••••••a3F9",
                    tokenEditing = true,
                    fontScale = MyNoteFontScale.Large,
                    isLoading = false,
                ),
            onIntent = {},
        )
    }
}
