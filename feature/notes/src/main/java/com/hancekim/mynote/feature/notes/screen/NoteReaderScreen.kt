package com.hancekim.mynote.feature.notes.screen

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.SpanStyle
import com.hancekim.mynote.core.designsystem.block.NoteContent
import com.hancekim.mynote.core.designsystem.component.CategoryChip
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.toContentBlocks

/**
 * 노트 상세(블로그 글처럼 읽기). 카테고리 칩 + 날짜 → 제목 → 서버드리븐 블록 본문.
 * 본문 폭은 `reading` 토큰 안에서 가운데 정렬.
 */
@Composable
fun NoteReaderScreen(
    note: Note,
    categoryName: String,
    modifier: Modifier = Modifier,
) {
    val adaptive = MyNoteTheme.adaptive
    val reading = adaptive.readingMaxWidth
    val blocks = remember(note.blocks) { note.blocks.toContentBlocks() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MyNoteTheme.colors.bg),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = (if (reading != null) Modifier.widthIn(max = reading) else Modifier.fillMaxWidth())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = adaptive.page, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryChip(categoryName)
                Text(
                    text = note.date,
                    style = MyNoteTheme.textStyles.caption,
                    color = MyNoteTheme.colors.text3,
                )
            }
            Text(
                text = note.title,
                style = MyNoteTheme.textStyles.noteTitle,
                color = MyNoteTheme.colors.text,
            )
            NoteContent(blocks = blocks)
        }
    }
}

private val previewNote = Note(
    id = "compose-state",
    title = "Compose 상태 관리 정리",
    date = "2026.06.12",
    blocks = listOf(
        NoteBlock.Heading(2, listOf(NoteSpan("상태 호이스팅"))),
        NoteBlock.Paragraph(
            listOf(
                NoteSpan("Composable 은 "),
                NoteSpan("상태를 직접 들지 않고", SpanStyle.Bold),
                NoteSpan(" 상위로 끌어올린다."),
            ),
        ),
        NoteBlock.Quote(listOf(NoteSpan("상태는 단방향으로 흐른다."))),
        NoteBlock.BulletList(
            listOf(
                NoteListItem(listOf(NoteSpan("단순 UI 상태는 "), NoteSpan("remember", SpanStyle.Code))),
                NoteListItem(listOf(NoteSpan("복원이 필요하면 "), NoteSpan("rememberSaveable", SpanStyle.Code))),
            ),
        ),
        NoteBlock.Code("var text by rememberSaveable { mutableStateOf(\"\") }"),
    ),
)

@Preview(name = "Reader", showBackground = true, widthDp = 400, heightDp = 720)
@Composable
private fun NoteReaderScreenPreview() {
    MyNoteTheme {
        NoteReaderScreen(note = previewNote, categoryName = "Android")
    }
}
