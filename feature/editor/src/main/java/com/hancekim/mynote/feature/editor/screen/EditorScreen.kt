package com.hancekim.mynote.feature.editor.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.token.resized
import com.hancekim.mynote.core.designsystem.block.NoteContent
import com.hancekim.mynote.core.designsystem.component.CategoryChip
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.model.DeviceNoteDraft
import com.hancekim.mynote.core.domain.model.DraftCheck
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteDates
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.SpanStyle
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.mapper.toContentBlocks
import com.hancekim.mynote.core.ui.screen.connect.ProgressBar
import com.hancekim.mynote.feature.editor.EditorIntent
import com.hancekim.mynote.feature.editor.EditorSession
import com.hancekim.mynote.feature.editor.EditorUiState
import com.hancekim.mynote.feature.editor.R
import com.hancekim.mynote.feature.editor.SaveBlock
import java.time.LocalDate
import com.hancekim.mynote.core.designsystem.R as DsR

/**
 * 편집기 목적지 화면(local-notes LN-3 · LN-4 · LN-9 · LN-10). [key] 와 짝인 세션만 그린다 — 전환 중 나가는 편집기는 빈 자리다.
 *
 * 자기 상단 바(닫기 · `새 노트`/`노트 고치기` · 주 `저장`) 아래 한 스크롤: 제목 칸 → 그룹 줄 → (고치기) 메타 줄 → `쓰기`/`미리보기` 전환
 * → 이유 줄 → 본문 칸 또는 미리보기. 키보드가 열리면 상단 바만 남고 나머지는 스크롤된다(디자인 C7). 닫기 · 뒤로의 나가기 확인은
 * 셸 뒤로가기 ④ 의 지킴과 같은 [EditorIntent.Close] 로 간다 — 화면 안에 `BackHandler` 를 두지 않는다.
 */
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
            navigationIcon = MyNoteIcons.Close,
            navigationLabel = stringResource(DsR.string.ds_action_close),
            onNavigation = { onIntent(EditorIntent.Close) },
            actionLabel = stringResource(DsR.string.ds_action_save),
            actionEnabled = session?.canSave == true,
            onAction = { onIntent(EditorIntent.Save) },
        )
        if (session != null) EditorBody(session, onIntent)
    }
}

@Composable
private fun EditorBody(
    session: EditorSession,
    onIntent: (EditorIntent) -> Unit,
) {
    val colors = MyNoteTheme.colors
    val styles = MyNoteTheme.textStyles
    val adaptive = MyNoteTheme.adaptive
    val reading = adaptive.readingMaxWidth
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val titleFocus = remember { FocusRequester() }
    // 새 노트 · 가져온 글은 제목 칸에서 시작한다(디자인 조작 표). 고치기는 키보드를 열지 않는다. 화면을 다시 만들어도 한 번만
    var focusedOnce by rememberSaveable(session.key) { mutableStateOf(false) }
    LaunchedEffect(session.key, session.loading) {
        if (!focusedOnce && !session.editing && !session.previewing) {
            focusedOnce = true
            titleFocus.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Column(
            modifier =
                (if (reading != null) Modifier.widthIn(max = reading) else Modifier)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = adaptive.page, end = adaptive.page, bottom = BOTTOM_SPACE),
        ) {
            PlainField(
                value = session.draft.title,
                hint = stringResource(R.string.editor_field_title),
                style = styles.screenTitle,
                singleLine = true,
                enabled = !session.loading,
                onChange = { onIntent(EditorIntent.TitleChanged(it)) },
                modifier =
                    Modifier
                        .underline(colors.border)
                        .padding(vertical = 12.dp)
                        .focusRequester(titleFocus),
            )
            GroupLine(session, onIntent)
            if (session.editing) CreatedLine(session.draft.date)
            ModeToggle(
                previewing = session.previewing,
                onSelect = { preview ->
                    if (preview) {
                        // 미리보기는 해석 한 번 + 키보드 닫음(디자인 조작 표). 나가기 확인 판정에 들지 않는다
                        focus.clearFocus()
                        keyboard?.hide()
                    }
                    onIntent(EditorIntent.SetPreview(preview))
                },
                modifier = Modifier.padding(top = TOGGLE_TOP),
            )
            session.saveBlock?.let { block ->
                ReasonLine(
                    text =
                        when (block) {
                            SaveBlock.Empty -> {
                                stringResource(R.string.editor_reason_empty)
                            }

                            SaveBlock.TooLarge -> {
                                stringResource(R.string.editor_reason_too_large, session.bodyKilobytes.toString())
                            }
                        },
                    warn = block == SaveBlock.TooLarge,
                    modifier = Modifier.padding(top = REASON_TOP),
                )
            }
            if (session.previewing) {
                EditorPreview(session)
            } else {
                PlainField(
                    value = session.draft.body,
                    hint = stringResource(R.string.editor_field_body),
                    style = styles.bodyCompact,
                    color = colors.bodyText,
                    singleLine = false,
                    enabled = !session.loading,
                    onChange = { onIntent(EditorIntent.BodyChanged(it)) },
                    modifier = Modifier.padding(top = 12.dp).heightIn(min = BODY_MIN_HEIGHT),
                )
            }
        }
    }
}

/** 그룹 줄 — 라벨 `그룹`(폭 40) + 자유 입력 칸(LN-4). 비우면 기본 그룹 `그룹 없음` 에 든다. */
@Composable
private fun GroupLine(
    session: EditorSession,
    onIntent: (EditorIntent) -> Unit,
) {
    val colors = MyNoteTheme.colors
    val styles = MyNoteTheme.textStyles
    Row(
        modifier = Modifier.fillMaxWidth().height(GROUP_HEIGHT).underline(colors.border),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.editor_field_group),
            style = styles.caption,
            color = colors.text3,
            maxLines = 1,
            modifier = Modifier.width(GROUP_LABEL_WIDTH),
        )
        PlainField(
            value = session.draft.group,
            hint = stringResource(R.string.editor_group_default),
            style = styles.bodySmall,
            color = colors.bodyText,
            singleLine = true,
            enabled = !session.loading,
            onChange = { onIntent(EditorIntent.GroupChanged(it)) },
        )
    }
}

/** 고치기 편집기의 메타 줄 — `이 기기 · 2026.10.14에 만듦`(22B · 지우기 버튼 없음). 날짜는 만든 날이고 고쳐도 그대로다(LN-4). */
@Composable
private fun CreatedLine(date: LocalDate?) {
    if (date == null) return
    val colors = MyNoteTheme.colors
    Row(
        modifier = Modifier.padding(top = 12.dp).height(META_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = MyNoteIcons.Device,
            contentDescription = null,
            tint = colors.text3,
            modifier = Modifier.size(GLYPH),
        )
        Text(
            text = stringResource(R.string.editor_meta_created, NoteDates.format(date)),
            style = MyNoteTheme.textStyles.caption,
            color = colors.text3,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 미리보기 — 리더와 같은 머리(칩 · 날짜 · 이 기기 → 노트 제목)와 리더 렌더러 `NoteContent`(한도 상자 포함 — 디자인 C5).
 * 블록은 저장할 본문을 저장 때와 같은 경로로 해석한 결과다(LN-3 ②). 해석이 오기 전엔 진행 막대(디자인 C10).
 */
@Composable
private fun EditorPreview(session: EditorSession) {
    val blocks = session.previewBlocks
    if (blocks == null) {
        ProgressBar(modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
        return
    }
    val colors = MyNoteTheme.colors
    val styles = MyNoteTheme.textStyles
    val content = remember(blocks) { blocks.toContentBlocks() }
    val today = remember { LocalDate.now() }
    val group =
        session.draft.group
            .trim()
            .ifEmpty { stringResource(R.string.editor_group_default) }
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryChip(group, modifier = Modifier.widthIn(max = CHIP_MAX_WIDTH))
            Text(
                text = NoteDates.format(session.draft.date ?: today),
                style = styles.caption,
                color = colors.text3,
                maxLines = 1,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = MyNoteIcons.Device,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(GLYPH),
                )
                Text(
                    text = stringResource(R.string.editor_origin_device),
                    style = styles.caption,
                    color = colors.text3,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = session.draft.title.trim(),
            style = styles.noteTitle.resized(MyNoteTheme.adaptive.noteTitleSize),
            color = colors.text,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(20.dp))
        NoteContent(blocks = content)
    }
}

// 전환은 터치 48 안에 트랙 36 을 그린다 — 위 · 아래 6 이 트랙 밖이라 디자인 간격(그룹 줄 → 12 → 전환 → 8 → 이유 줄)에서 그만큼 뺀다
private val TOGGLE_TOP: Dp = 6.dp
private val REASON_TOP: Dp = 2.dp
private val GROUP_HEIGHT: Dp = 44.dp
private val GROUP_LABEL_WIDTH: Dp = 40.dp
private val META_HEIGHT: Dp = 36.dp
private val GLYPH: Dp = 12.dp
private val CHIP_MAX_WIDTH: Dp = 120.dp

// 본문 칸은 남은 높이를 쓰는 대신 최소 높이를 둔다 — 빈 칸 아래를 눌러도 본문에 닿게(한 스크롤 안이라 weight 를 못 쓴다)
private val BODY_MIN_HEIGHT: Dp = 240.dp
private val BOTTOM_SPACE: Dp = 24.dp

private val previewDraft =
    DeviceNoteDraft(
        title = "코루틴 취소와 예외 처리 정리",
        group = "Kotlin",
        body = "취소는 한 번 요청하면 되돌릴 수 없다.\n\n## 취소는 협력적이다\n- isActive 를 확인한다\n- ensureActive() 로 바로 멈춘다",
        date = LocalDate.of(2026, 10, 14),
    )

private fun previewState(
    key: NotesNavKey.Editor,
    draft: DeviceNoteDraft,
    check: DraftCheck = DraftCheck(titleBlank = false, bodyBlank = false, bodyTooLarge = false),
    previewing: Boolean = false,
    bodyKilobytes: Int = 0,
) = EditorUiState(
    session =
        EditorSession(
            key = key,
            draft = draft,
            original = draft,
            check = check,
            previewing = previewing,
            bodyKilobytes = bodyKilobytes,
            previewBlocks =
                if (previewing) {
                    listOf(
                        NoteBlock.Paragraph(listOf(NoteSpan("취소는 한 번 요청하면 되돌릴 수 없다."))),
                        NoteBlock.Heading(2, listOf(NoteSpan("취소는 협력적이다"))),
                        NoteBlock.Paragraph(listOf(NoteSpan("ensureActive()", SpanStyle.Code))),
                    )
                } else {
                    null
                },
        ),
)

@Preview(name = "편집기 · 빈 · 저장 꺼짐", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun EditorEmptyPreview() {
    val key = NotesNavKey.Editor()
    MyNoteTheme {
        EditorScreen(
            key = key,
            state =
                previewState(
                    key,
                    DeviceNoteDraft(),
                    DraftCheck(titleBlank = true, bodyBlank = true, bodyTooLarge = false),
                ),
            onIntent = {},
        )
    }
}

@Preview(name = "편집기 · 크기 넘음", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun EditorTooLargePreview() {
    val key = NotesNavKey.Editor()
    MyNoteTheme {
        EditorScreen(
            key = key,
            state =
                previewState(
                    key,
                    previewDraft,
                    DraftCheck(titleBlank = false, bodyBlank = false, bodyTooLarge = true),
                    bodyKilobytes = 530,
                ),
            onIntent = {},
        )
    }
}

@Preview(name = "편집기 · 고치기", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun EditorEditPreview() {
    val key = NotesNavKey.Editor(noteId = "local/1")
    MyNoteTheme { EditorScreen(key = key, state = previewState(key, previewDraft), onIntent = {}) }
}

@Preview(name = "편집기 · 미리보기", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun EditorPreviewPreview() {
    val key = NotesNavKey.Editor()
    MyNoteTheme { EditorScreen(key = key, state = previewState(key, previewDraft, previewing = true), onIntent = {}) }
}
