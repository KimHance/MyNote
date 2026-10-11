package com.hancekim.mynote.feature.notes.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale
import com.hancekim.mynote.core.designfoundation.token.MyNoteRadius
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.ui.element.LastSync
import com.hancekim.mynote.core.ui.element.NotesDrafts
import com.hancekim.mynote.core.ui.element.modelCardUi
import com.hancekim.mynote.core.ui.screen.CONTENT_MAX_WIDTH
import com.hancekim.mynote.core.ui.screen.SecondaryButton
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.settings.SettingsIntent
import com.hancekim.mynote.feature.notes.settings.SettingsUiState

@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    drafts: NotesDrafts,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onEnableNotifications: () -> Unit = {},
) {
    val colors = MyNoteTheme.colors
    val adaptive = MyNoteTheme.adaptive
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    if (confirmDelete) {
        ModelDeleteDialog(
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onIntent(SettingsIntent.DeleteModel)
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
                        if (intent == SettingsIntent.DeleteModel) confirmDelete = true else onIntent(intent)
                    },
                    onEnableNotifications = onEnableNotifications,
                )
                RepoSection(
                    uiState = uiState,
                    onIntent = onIntent,
                    drafts = drafts,
                    snackbarHostState = snackbarHostState,
                )
            }
        }
    }
}

@Composable
private fun ReadingSection(
    uiState: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
) {
    SettingsSection(label = stringResource(R.string.notes_settings_section_reading)) {
        SettingsCard {
            FontScaleRow(
                large = uiState.fontScale == MyNoteFontScale.Large,
                onChange = { large ->
                    onIntent(
                        SettingsIntent.SetFontScale(
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
    onIntent: (SettingsIntent) -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    if (confirmClear) {
        StudyLogClearDialog(
            onDismiss = { confirmClear = false },
            onConfirm = {
                confirmClear = false
                onIntent(SettingsIntent.ClearStudyLog)
            },
        )
    }
    SettingsSection(label = stringResource(R.string.notes_settings_section_studylog)) {
        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.notes_settings_studylog_title),
                description = stringResource(R.string.notes_settings_studylog_desc),
                checked = enabled,
                onChange = { onIntent(SettingsIntent.SetStudyLogEnabled(it)) },
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

private val SECTION_GAP: Dp = 28.dp

@Preview(name = "설정 · 공개 저장소", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 400, heightDp = 1100)
@Composable
private fun SettingsScreenPreview() {
    MyNoteTheme {
        SettingsScreen(
            uiState =
                SettingsUiState(
                    connection = previewConnection,
                    lastSync = LastSync.Succeeded,
                ),
            onIntent = {},
            drafts = NotesDrafts(),
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(name = "설정 · 토큰 사용", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 400, heightDp = 1100)
@Composable
private fun SettingsScreenTokenPreview() {
    MyNoteTheme {
        SettingsScreen(
            uiState =
                SettingsUiState(
                    connection = previewConnection,
                    hasToken = true,
                    maskedToken = "github_pat_••••••••a1b2",
                    lastSync = LastSync.Succeeded,
                ),
            onIntent = {},
            drafts = NotesDrafts(),
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(name = "설정 · 연결 없음", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 400, heightDp = 860)
@Composable
private fun SettingsScreenNoConnectionPreview() {
    MyNoteTheme {
        SettingsScreen(
            uiState = SettingsUiState(connection = ConnectionState.None),
            onIntent = {},
            drafts = NotesDrafts(),
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

private val previewConnection =
    ConnectionState.Connected(
        sourceId = SourceId("s1"),
        connection = RepoConnection("octo-learner", "android-notes", "main", "notes/index.json", isPublic = true),
    )
