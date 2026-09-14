package com.hancekim.mynote.feature.notes.screen

import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.block.NoteContent
import com.hancekim.mynote.core.designsystem.block.NoteContentBlock
import com.hancekim.mynote.core.designsystem.block.NoteContentSpan
import com.hancekim.mynote.core.designsystem.block.NoteSpanStyle
import com.hancekim.mynote.core.designsystem.component.CategoryChip
import com.hancekim.mynote.core.designsystem.component.fadingEdges
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.navigation.rememberNoteSharedModifiers
import com.hancekim.mynote.feature.notes.screen.quiz.QuizCtaCard
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun NoteReaderScreen(
    note: NoteUi,
    categoryName: String,
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
    scrollState: ScrollState = rememberScrollState(),
    onOpenQuiz: (() -> Unit)? = null,
    quizAvailable: Boolean = true,
) {
    val shared = rememberNoteSharedModifiers(note.id)
    val adaptive = MyNoteTheme.adaptive
    val reading = adaptive.readingMaxWidth
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MyNoteTheme.colors.bg),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier =
                (if (reading != null) Modifier.widthIn(max = reading) else Modifier.fillMaxWidth())
                    .fadingEdges(top = adaptive.fadeHeight, bottom = adaptive.fadeHeight + 6.dp)
                    .verticalScroll(scrollState)
                    .padding(
                        start = adaptive.page,
                        end = adaptive.page,
                        top = adaptive.padY,
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
                modifier = shared.title,
            )
            NoteContent(blocks = note.blocks)
            if (onOpenQuiz != null) {
                QuizCtaCard(onClick = onOpenQuiz, available = quizAvailable)
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
                        persistentListOf(
                            NoteContentSpan("단순 UI 상태는 ", NoteSpanStyle.Normal),
                            NoteContentSpan("remember", NoteSpanStyle.Code),
                        ),
                        persistentListOf(
                            NoteContentSpan("복원이 필요하면 ", NoteSpanStyle.Normal),
                            NoteContentSpan("rememberSaveable", NoteSpanStyle.Code),
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
        NoteReaderScreen(note = previewNote, categoryName = "Android", onOpenQuiz = {})
    }
}

@Preview(name = "Reader · 모델 없음", showBackground = true, widthDp = 400, heightDp = 720)
@Composable
private fun NoteReaderScreenNoModelPreview() {
    MyNoteTheme {
        NoteReaderScreen(
            note = previewNote,
            categoryName = "Android",
            onOpenQuiz = {},
            quizAvailable = false,
        )
    }
}
