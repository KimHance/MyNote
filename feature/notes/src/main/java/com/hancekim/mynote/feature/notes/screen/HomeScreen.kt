package com.hancekim.mynote.feature.notes.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.designsystem.component.NoteCard
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.NotesIntent
import com.hancekim.mynote.feature.notes.RecentNote
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 시작 화면(노트 미선택). hero → "최근 노트" 라벨 → 카드 그리드.
 * 그리드 열 수·간격·여백·최대폭은 모두 어댑티브 토큰을 따른다.
 */
@Composable
fun HomeScreen(
    recentNotes: ImmutableList<RecentNote>,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val adaptive = MyNoteTheme.adaptive
    val reading = adaptive.readingMaxWidth

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MyNoteTheme.colors.bg),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(adaptive.homeGridColumns),
            modifier = if (reading != null) Modifier.widthIn(max = reading) else Modifier,
            contentPadding = PaddingValues(adaptive.page),
            horizontalArrangement = Arrangement.spacedBy(adaptive.cardGap),
            verticalArrangement = Arrangement.spacedBy(adaptive.cardGap),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Hero(modalDrawer = adaptive.useModalDrawer)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "최근 노트",
                    style = MyNoteTheme.textStyles.overline,
                    color = MyNoteTheme.colors.text3,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(recentNotes, key = { it.note.id }) { recent ->
                NoteCard(
                    category = recent.categoryName,
                    date = recent.note.date,
                    title = recent.note.title,
                    onClick = { onIntent(NotesIntent.SelectNote(recent.note.id)) },
                )
            }
        }
    }
}

@Composable
private fun Hero(modalDrawer: Boolean) {
    val colors = MyNoteTheme.colors
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "N", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.W700)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "공부 기록",
            style = MyNoteTheme.textStyles.noteTitle.copy(fontSize = 26.sp),
            color = colors.text,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (modalDrawer) {
                "읽고 싶은 노트를 골라보세요. 왼쪽 위 ☰ 를 누르면 전체 목록이 열려요."
            } else {
                "왼쪽 목록에서 읽고 싶은 노트를 선택하세요."
            },
            style = MyNoteTheme.textStyles.body,
            color = colors.text2,
        )
    }
}

private val previewRecent = persistentListOf(
    RecentNote(Note("a", "Compose 상태 관리 정리", "2026.06.12", emptyList()), "Android"),
    RecentNote(Note("b", "코루틴 디스패처 메모", "2026.05.30", emptyList()), "Kotlin"),
    RecentNote(Note("c", "HTTP 캐싱 기초", "2026.04.22", emptyList()), "CS"),
)

@Preview(name = "Home Compact", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun HomeScreenCompactPreview() {
    MyNoteTheme {
        HomeScreen(recentNotes = previewRecent, onIntent = {})
    }
}

@Preview(name = "Home Medium", showBackground = true, widthDp = 760, heightDp = 640)
@Composable
private fun HomeScreenMediumPreview() {
    MyNoteTheme(windowClass = com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass.Medium) {
        HomeScreen(recentNotes = previewRecent, onIntent = {})
    }
}
