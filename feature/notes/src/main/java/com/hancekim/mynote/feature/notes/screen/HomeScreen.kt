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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.component.NoteCard
import com.hancekim.mynote.core.designsystem.component.fadingEdges
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.RecentNoteUi
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.navigation.rememberNoteSharedModifiers
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun HomeScreen(
    recentNotes: ImmutableList<RecentNoteUi>,
    isLoading: Boolean,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
    contentTopPadding: Dp = 0.dp,
    contentEndPadding: Dp = 0.dp,
    gridState: LazyGridState = rememberLazyGridState(),
    bookmarkedIds: ImmutableSet<String> = persistentSetOf(),
) {
    val adaptive = MyNoteTheme.adaptive
    val reading = adaptive.readingMaxWidth
    val colors = MyNoteTheme.colors

    Surface(modifier = modifier.fillMaxSize(), color = colors.bg) {
        Box(contentAlignment = Alignment.TopCenter) {
            if (recentNotes.isEmpty()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = colors.accent,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.notes_home_empty),
                        style = MyNoteTheme.textStyles.body,
                        color = colors.text2,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = adaptive.cardMinWidth),
                    modifier =
                        (if (reading != null) Modifier.widthIn(max = reading) else Modifier)
                            .fadingEdges(top = adaptive.fadeHeight, bottom = adaptive.fadeHeight + 6.dp),
                    contentPadding =
                        PaddingValues(
                            start = adaptive.page,
                            end = adaptive.page + contentEndPadding,
                            top = adaptive.padY + contentTopPadding,
                            bottom = adaptive.page + contentBottomPadding,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(adaptive.cardGap),
                    verticalArrangement = Arrangement.spacedBy(adaptive.cardGap),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Hero(modalDrawer = adaptive.useModalDrawer)
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.notes_home_recent),
                            style = MyNoteTheme.textStyles.overline,
                            color = colors.text3,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(recentNotes, key = { it.note.id }) { recent ->
                        val shared = rememberNoteSharedModifiers(recent.note.id)
                        NoteCard(
                            category = recent.categoryName,
                            date = recent.note.date,
                            title = recent.note.title,
                            onClick = { onIntent(NotesIntent.SelectNote(recent.note.id)) },
                            bookmarked = recent.note.id in bookmarkedIds,
                            chipModifier = shared.chip,
                            dateModifier = shared.date,
                            titleModifier = shared.title,
                        )
                    }
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
    val adaptive = MyNoteTheme.adaptive
    val logoSize = adaptive.heroLogoSize

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        if (logoSize != null) {
            Box(
                modifier =
                    Modifier
                        .size(logoSize)
                        .clip(RoundedCornerShape(logoSize * 0.29f))
                        .background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "N",
                    color = Color.White,
                    fontSize = adaptive.heroTitleSize,
                    fontWeight = FontWeight.W700,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        Text(
            text = stringResource(R.string.notes_app_title),
            style = MyNoteTheme.textStyles.noteTitle.copy(fontSize = adaptive.heroTitleSize),
            color = colors.text,
        )
        if (logoSize != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text =
                    if (modalDrawer) {
                        stringResource(R.string.notes_home_hint_compact)
                    } else {
                        stringResource(R.string.notes_home_hint_wide)
                    },
                style = MyNoteTheme.textStyles.body,
                color = colors.text2,
            )
        }
    }
}

private val previewRecent =
    persistentListOf(
        RecentNoteUi(NoteUi("a", "Compose 상태 관리 정리", "2026.06.12"), "Android"),
        RecentNoteUi(NoteUi("b", "코루틴 디스패처 메모", "2026.05.30"), "Kotlin"),
        RecentNoteUi(NoteUi("c", "HTTP 캐싱 기초", "2026.04.22"), "CS"),
    )

@Preview(name = "Home · Compact×Medium", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun HomeScreenCompactPreview() {
    MyNoteTheme {
        HomeScreen(recentNotes = previewRecent, isLoading = false, onIntent = {})
    }
}

@Preview(name = "Home · 북마크 리본", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun HomeScreenBookmarkedPreview() {
    MyNoteTheme {
        HomeScreen(
            recentNotes = previewRecent,
            isLoading = false,
            onIntent = {},
            bookmarkedIds = persistentSetOf("a", "c"),
        )
    }
}

@Preview(name = "Home · Medium×Compact", showBackground = true, widthDp = 760, heightDp = 420)
@Composable
private fun HomeScreenShortPreview() {
    MyNoteTheme(
        windowClass = MyNoteWindowClass.Medium,
        heightClass = MyNoteHeightClass.Compact,
    ) {
        HomeScreen(recentNotes = previewRecent, isLoading = false, onIntent = {})
    }
}

@Preview(name = "Home · Expanded", showBackground = true, widthDp = 900, heightDp = 700)
@Composable
private fun HomeScreenExpandedPreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Expanded) {
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
