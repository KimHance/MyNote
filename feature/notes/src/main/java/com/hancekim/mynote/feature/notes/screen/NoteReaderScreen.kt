package com.hancekim.mynote.feature.notes.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.block.NoteContent
import com.hancekim.mynote.core.designsystem.block.NoteContentBlock
import com.hancekim.mynote.core.designsystem.block.NoteContentListItem
import com.hancekim.mynote.core.designsystem.block.NoteContentSpan
import com.hancekim.mynote.core.designsystem.block.NoteContentState
import com.hancekim.mynote.core.designsystem.block.NoteSpanStyle
import com.hancekim.mynote.core.designsystem.block.UnsupportedKind
import com.hancekim.mynote.core.designsystem.block.rememberNoteContentState
import com.hancekim.mynote.core.designsystem.component.CategoryChip
import com.hancekim.mynote.core.designsystem.component.fadingEdges
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.model.NoteLimits
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.element.CtaAction
import com.hancekim.mynote.feature.notes.navigation.rememberNoteSharedModifiers
import com.hancekim.mynote.feature.notes.quiz.component.QuizCtaCard
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun NoteReaderScreen(
    note: NoteUi,
    categoryName: String,
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
    contentTopPadding: Dp = 0.dp,
    scrollState: ScrollState = rememberScrollState(),
    contentState: NoteContentState = rememberNoteContentState(),
    onQuizAction: ((CtaAction) -> Unit)? = null,
    modelStatus: ModelStatus = ModelStatus.Ready,
) {
    val shared = rememberNoteSharedModifiers(note.id)
    val adaptive = MyNoteTheme.adaptive
    val reading = adaptive.readingMaxWidth
    Surface(modifier = modifier.fillMaxSize(), color = MyNoteTheme.colors.bg) {
        Box(contentAlignment = Alignment.TopCenter) {
            Column(
                modifier =
                    (if (reading != null) Modifier.widthIn(max = reading) else Modifier.fillMaxWidth())
                        .fadingEdges(top = adaptive.fadeHeight, bottom = adaptive.fadeHeight + 6.dp)
                        .verticalScroll(scrollState)
                        .padding(
                            start = adaptive.page,
                            end = adaptive.page,
                            top = adaptive.padY + contentTopPadding,
                            bottom = 24.dp + contentBottomPadding,
                        ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryChip(categoryName, modifier = shared.chip)
                    Text(
                        text = note.date,
                        style = MyNoteTheme.textStyles.caption,
                        color = MyNoteTheme.colors.text3,
                        modifier = shared.date,
                    )
                }
                Text(
                    text = note.title,
                    style = MyNoteTheme.textStyles.noteTitle.copy(fontSize = adaptive.noteTitleSize),
                    color = MyNoteTheme.colors.text,
                    modifier = shared.title.semantics { heading() },
                )
                NoteContent(blocks = note.blocks, state = contentState)
                if (onQuizAction != null && note.readable) {
                    QuizCtaCard(status = modelStatus, onAction = onQuizAction)
                }
            }
        }
    }
}

private val previewNote =
    NoteUi(
        id = "compose-state",
        title = "Compose 상태 관리 정리",
        date = "2026.06.12",
        blocks =
            persistentListOf(
                NoteContentBlock.Heading(2, persistentListOf(NoteContentSpan("상태 호이스팅", NoteSpanStyle.Normal))),
                NoteContentBlock.Paragraph(
                    persistentListOf(
                        NoteContentSpan("Composable 은 ", NoteSpanStyle.Normal),
                        NoteContentSpan("상태를 직접 들지 않고", NoteSpanStyle.Bold),
                        NoteContentSpan(" 상위로 끌어올린다.", NoteSpanStyle.Normal),
                    ),
                ),
                NoteContentBlock.Quote(
                    persistentListOf(NoteContentSpan("상태는 단방향으로 흐른다.", NoteSpanStyle.Normal)),
                ),
                NoteContentBlock.BulletList(
                    persistentListOf(
                        NoteContentListItem(
                            persistentListOf(
                                NoteContentSpan("단순 UI 상태는 ", NoteSpanStyle.Normal),
                                NoteContentSpan("remember", NoteSpanStyle.Code),
                            ),
                        ),
                        NoteContentListItem(
                            persistentListOf(
                                NoteContentSpan("복원이 필요하면 ", NoteSpanStyle.Normal),
                                NoteContentSpan("rememberSaveable", NoteSpanStyle.Code),
                            ),
                        ),
                    ),
                ),
                NoteContentBlock.Code("var text by rememberSaveable { mutableStateOf(\"\") }"),
            ),
    )

@Preview(name = "Reader", showBackground = true, widthDp = 400, heightDp = 720)
@Composable
private fun NoteReaderScreenPreview() {
    MyNoteTheme {
        NoteReaderScreen(note = previewNote, categoryName = "Android", onQuizAction = {})
    }
}

@Preview(name = "Reader · 모델 없음", showBackground = true, widthDp = 400, heightDp = 720)
@Composable
private fun NoteReaderScreenNoModelPreview() {
    MyNoteTheme {
        NoteReaderScreen(
            note = previewNote,
            categoryName = "Android",
            onQuizAction = {},
            modelStatus = ModelStatus.Missing,
        )
    }
}

private val previewUnsupportedNote =
    previewNote.copy(
        blocks =
            persistentListOf(
                NoteContentBlock.Paragraph(
                    persistentListOf(NoteContentSpan("TCP 는 연결을 맺을 때 세 번 주고받는다.", NoteSpanStyle.Normal)),
                ),
                NoteContentBlock.Unsupported(
                    kind = UnsupportedKind.UnknownType,
                    salvaged = "1. 클라이언트 → 서버: SYN\n2. 서버 → 클라이언트: SYN+ACK\n3. 클라이언트 → 서버: ACK",
                    raw = "{\n  \"type\": \"table\",\n  \"rows\": []\n}",
                ),
                NoteContentBlock.Code("socket.connect(address)"),
                NoteContentBlock.Unsupported(
                    kind = UnsupportedKind.BrokenBlock,
                    salvaged = null,
                    raw = "{\n  \"type\": \"heading\"\n}",
                ),
            ),
    )

private val previewBrokenNote =
    previewNote.copy(
        blocks =
            persistentListOf(
                NoteContentBlock.Unsupported(
                    kind = UnsupportedKind.BrokenNote,
                    salvaged = null,
                    raw = "{\n  \"id\": \"compose-state\",\n  \"blocks\": \"깨짐\"\n",
                ),
            ),
        readable = false,
    )

@Preview(name = "Reader · 모르는 블록", showBackground = true, widthDp = 400, heightDp = 720)
@Composable
private fun NoteReaderScreenUnsupportedPreview() {
    MyNoteTheme {
        NoteReaderScreen(note = previewUnsupportedNote, categoryName = "Network", onQuizAction = {})
    }
}

@Preview(name = "Reader · 읽지 못한 노트", showBackground = true, widthDp = 400, heightDp = 720)
@Composable
private fun NoteReaderScreenBrokenNotePreview() {
    MyNoteTheme {
        NoteReaderScreen(note = previewBrokenNote, categoryName = "Android", onQuizAction = {})
    }
}

private val previewLimitNote =
    previewNote.copy(
        blocks =
            persistentListOf(
                NoteContentBlock.Unsupported(
                    kind = UnsupportedKind.LimitExceeded(NoteLimits.Default.maxDepth, NoteLimits.Default.maxKilobytes),
                    salvaged = null,
                    raw = "- 상태\n  - 호이스팅\n    - 단방향\n",
                ),
            ),
        readable = false,
    )

@Preview(name = "Reader · 한도 넘음", showBackground = true, widthDp = 400, heightDp = 720)
@Composable
private fun NoteReaderScreenLimitPreview() {
    MyNoteTheme {
        NoteReaderScreen(note = previewLimitNote, categoryName = "Android", onQuizAction = {})
    }
}
