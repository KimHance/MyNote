package com.hancekim.mynote.feature.notes.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.data.model.Category
import com.hancekim.mynote.core.data.model.Note
import com.hancekim.mynote.core.designsystem.component.CategoryRow
import com.hancekim.mynote.core.designsystem.component.NoteRow
import com.hancekim.mynote.core.designsystem.component.SearchField
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.NotesIntent
import com.hancekim.mynote.feature.notes.NotesUiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/**
 * 카테고리/노트 트리 + 검색. Compact 에서는 모달 드로어, Medium+ 에서는 고정 list pane 으로 쓰인다.
 */
@Composable
fun ListPanelScreen(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            text = "노트 ${uiState.totalCount}개",
            style = MyNoteTheme.textStyles.caption,
            color = colors.text3,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        SearchField(
            query = uiState.query,
            onQueryChange = { onIntent(NotesIntent.QueryChanged(it)) },
        )
        Spacer(Modifier.height(12.dp))

        if (uiState.query.isNotBlank() && uiState.visibleCategories.isEmpty()) {
            Text(
                text = "검색 결과가 없어요",
                style = MyNoteTheme.textStyles.body,
                color = colors.text3,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        } else {
            LazyColumn {
                uiState.visibleCategories.forEach { category ->
                    item(key = "cat-${category.name}") {
                        CategoryRow(
                            name = category.name,
                            count = category.notes.size,
                            expanded = uiState.isExpanded(category.name),
                            onClick = { onIntent(NotesIntent.ToggleCategory(category.name)) },
                        )
                    }
                    if (uiState.isExpanded(category.name)) {
                        items(category.notes, key = { "note-${it.id}" }) { note ->
                            NoteRow(
                                title = note.title,
                                active = note.id == uiState.activeNoteId,
                                onClick = { onIntent(NotesIntent.SelectNote(note.id)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private val previewState = NotesUiState(
    categories = persistentListOf(
        Category("Android", listOf(Note("a", "Compose 상태 관리 정리", "2026.06.12", emptyList()))),
        Category("Kotlin", listOf(Note("b", "코루틴 디스패처 메모", "2026.05.30", emptyList()))),
    ),
    visibleCategories = persistentListOf(
        Category("Android", listOf(Note("a", "Compose 상태 관리 정리", "2026.06.12", emptyList()))),
        Category("Kotlin", listOf(Note("b", "코루틴 디스패처 메모", "2026.05.30", emptyList()))),
    ),
    activeNoteId = "a",
    expanded = persistentMapOf("Android" to true),
    isLoading = false,
)

@Preview(name = "ListPanel", showBackground = true, backgroundColor = 0xFF161C24, widthDp = 300, heightDp = 520)
@Composable
private fun ListPanelScreenPreview() {
    MyNoteTheme {
        ListPanelScreen(uiState = previewState, onIntent = {})
    }
}
