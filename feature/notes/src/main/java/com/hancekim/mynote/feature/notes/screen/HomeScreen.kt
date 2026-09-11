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
import androidx.compose.material3.CircularProgressIndicator
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
import com.hancekim.mynote.core.designsystem.component.NoteCard
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.NotesIntent
import com.hancekim.mynote.feature.notes.RecentNoteUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun HomeScreen(
    recentNotes: ImmutableList<RecentNoteUi>,
    isLoading: Boolean,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val adaptive = MyNoteTheme.adaptive
    val reading = adaptive.readingMaxWidth
    val colors = MyNoteTheme.colors

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.bg),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (recentNotes.isEmpty()) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.accent,
                )
            } else {
                Text(
                    text = "표시할 노트가 없어요",
                    style = MyNoteTheme.textStyles.body,
                    color = colors.text2,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        } else {
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
                        color = colors.text3,
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
}

@Composable
private fun Hero(
    modalDrawer: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "N",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.W700,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "공부 기록",
            style = MyNoteTheme.textStyles.noteTitle.copy(fontSize = 26.sp),
            color = colors.text,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text =
                if (modalDrawer) {
                    "읽고 싶은 노트를 골라보세요. 왼쪽 위 ☰ 를 누르면 전체 목록이 열려요."
                } else {
                    "왼쪽 목록에서 읽고 싶은 노트를 선택하세요."
                },
            style = MyNoteTheme.textStyles.body,
            color = colors.text2,
        )
    }
}

private val previewRecent =
    persistentListOf(
        RecentNoteUi(NoteUi("a", "Compose 상태 관리 정리", "2026.06.12"), "Android"),
        RecentNoteUi(NoteUi("b", "코루틴 디스패처 메모", "2026.05.30"), "Kotlin"),
        RecentNoteUi(NoteUi("c", "HTTP 캐싱 기초", "2026.04.22"), "CS"),
    )

@Preview(name = "Home Compact", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun HomeScreenCompactPreview() {
    MyNoteTheme {
        HomeScreen(recentNotes = previewRecent, isLoading = false, onIntent = {})
    }
}

@Preview(name = "Home Medium", showBackground = true, widthDp = 760, heightDp = 640)
@Composable
private fun HomeScreenMediumPreview() {
    MyNoteTheme(windowClass = com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass.Medium) {
        HomeScreen(recentNotes = previewRecent, isLoading = false, onIntent = {})
    }
}

@Preview(name = "Home · 로딩 중", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun HomeScreenLoadingPreview() {
    MyNoteTheme {
        HomeScreen(recentNotes = persistentListOf(), isLoading = true, onIntent = {})
    }
}

@Preview(name = "Home · 빈 상태", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun HomeScreenEmptyPreview() {
    MyNoteTheme {
        HomeScreen(recentNotes = persistentListOf(), isLoading = false, onIntent = {})
    }
}
