package com.hancekim.mynote.feature.editor.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.model.DeviceNoteDraft
import com.hancekim.mynote.core.domain.model.DraftCheck
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.screen.PrimaryButton
import com.hancekim.mynote.feature.editor.EditorIntent
import com.hancekim.mynote.feature.editor.EditorSession
import com.hancekim.mynote.feature.editor.EditorUiState
import com.hancekim.mynote.feature.editor.R
import com.hancekim.mynote.core.designsystem.R as DsR

// 골격 — 목적지 · 배선을 세우는 자리다. 모양(칸 모양 · 쓰기/미리보기 전환 · 이유 줄 · 키보드 규칙)은 편집기 화면 단위가 채운다.

/** 편집기 목적지 화면. [key] 와 짝인 세션만 그린다 — 전환 중 나가는 편집기는 빈 자리다. */
@Composable
internal fun EditorScreen(
    key: NotesNavKey.Editor,
    state: EditorUiState,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val session = state.session?.takeIf { it.key == key }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.bg)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        EditorTopBar(
            title = stringResource(if (key.noteId != null) R.string.editor_title_edit else R.string.editor_title_new),
            onClose = { onIntent(EditorIntent.Close) },
            actionLabel = stringResource(DsR.string.ds_action_save),
            actionEnabled = session?.canSave == true,
            onAction = { onIntent(EditorIntent.Save) },
        )
        if (session != null) EditorFields(session, onIntent)
    }
}

@Composable
private fun EditorFields(
    session: EditorSession,
    onIntent: (EditorIntent) -> Unit,
) {
    val styles = MyNoteTheme.textStyles
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
    ) {
        PlainField(
            value = session.draft.title,
            hint = stringResource(R.string.editor_field_title),
            style = styles.title,
            singleLine = true,
            onChange = { onIntent(EditorIntent.TitleChanged(it)) },
        )
        PlainField(
            value = session.draft.group,
            hint = stringResource(R.string.editor_field_group),
            style = styles.bodySmall,
            singleLine = true,
            onChange = { onIntent(EditorIntent.GroupChanged(it)) },
        )
        PlainField(
            value = session.draft.body,
            hint = stringResource(R.string.editor_field_body),
            style = styles.bodyCompact,
            singleLine = false,
            onChange = { onIntent(EditorIntent.BodyChanged(it)) },
        )
    }
}

@Composable
internal fun PlainField(
    value: String,
    hint: String,
    style: TextStyle,
    singleLine: Boolean,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = singleLine,
        textStyle = style.copy(color = colors.text),
        cursorBrush = SolidColor(colors.accent),
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(text = hint, style = style, color = colors.text3)
                inner()
            }
        },
    )
}

/** 편집기 · 붙여넣기가 같이 쓰는 자기 상단 바 — 닫기 · 제목 · 주 동작 하나. */
@Composable
internal fun EditorTopBar(
    title: String,
    onClose: () -> Unit,
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(MyNoteTheme.adaptive.headerHeight)
                .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = MyNoteIcons.Close,
                contentDescription = stringResource(DsR.string.ds_action_close),
                tint = colors.text,
            )
        }
        Text(
            text = title,
            style = MyNoteTheme.textStyles.sectionTitle,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        PrimaryButton(text = actionLabel, onClick = onAction, enabled = actionEnabled)
    }
}

@Preview(name = "편집기 · 골격", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun EditorScreenPreview() {
    val key = NotesNavKey.Editor(draftId = 1)
    val draft = DeviceNoteDraft(title = "가져온 글", body = "# 본문\n\n내용")
    MyNoteTheme {
        EditorScreen(
            key = key,
            state =
                EditorUiState(
                    session =
                        EditorSession(
                            key,
                            draft,
                            draft,
                            DraftCheck(titleBlank = false, bodyBlank = false, bodyTooLarge = false),
                        ),
                ),
            onIntent = {},
        )
    }
}
