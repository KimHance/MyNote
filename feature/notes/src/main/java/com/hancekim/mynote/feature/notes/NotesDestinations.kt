package com.hancekim.mynote.feature.notes

import androidx.compose.runtime.Composable
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.frame.ShellDestinations
import com.hancekim.mynote.core.ui.frame.sceneEdgeModifier
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.screen.HomeScreen
import com.hancekim.mynote.feature.notes.screen.NoteReaderScreen
import com.hancekim.mynote.feature.notes.screen.bookmarks.BookmarksScreen

/**
 * 노트 기능의 목적지 — 홈 · 북마크 · 리더. 노트 상태는 화면 안에서 읽는다(다른 기능 상태가 바뀌어도 다시 그리지 않는다).
 *
 * 리더 끝 CTA 는 퀴즈 기능의 화면이라 셸이 슬롯으로 넘긴다 — [readerEndCta](노트 id). 노트는 퀴즈 모듈을 모른다(FM-2).
 */
fun notesDestinations(
    state: () -> NotesUiState,
    onIntent: (NotesIntent) -> Unit,
    readerEndCta: @Composable (noteId: String) -> Unit,
): ShellDestinations =
    { frame ->
        destination<NotesNavKey.Home> {
            val notes = state()
            HomeScreen(
                recentNotes = notes.recentNotes,
                body = notes.homeBody,
                entries = notes.homeEntries,
                repoName = notes.heroRepoName,
                showConnectBand = notes.showConnectBand && MyNoteTheme.adaptive.useModalDrawer,
                onIntent = onIntent,
                contentBottomPadding = frame.contentBottomPadding,
                contentEndPadding = frame.contentEndPadding,
                contentTopPadding = frame.contentTopPadding,
                gridState = frame.scroll.homeGrid,
                bookmarkedIds = notes.bookmarkedIds,
            )
        }
        destination<NotesNavKey.Bookmarks> {
            val modifier = frame.sceneEdgeModifier()
            val notes = state()
            BookmarksScreen(
                bookmarks = notes.bookmarks,
                isLoading = notes.isLoading && notes.categories.isEmpty(),
                twoPane = frame.twoPane,
                onIntent = onIntent,
                modifier = modifier,
                contentBottomPadding = frame.contentBottomPadding,
                contentEndPadding = frame.contentEndPadding,
                contentTopPadding = frame.contentTopPadding,
                listState = frame.scroll.bookmarksList,
            )
        }
        destination<NotesNavKey.Reader> { key ->
            val notes = state()
            val note = notes.noteById(key.noteId)
            if (note != null) {
                val modifier = frame.sceneEdgeModifier()
                NoteReaderScreen(
                    note = note,
                    categoryName = notes.categoryNameOf(note.id).orEmpty(),
                    modifier = modifier,
                    contentBottomPadding = frame.contentBottomPadding,
                    contentTopPadding = frame.contentTopPadding,
                    contentEndPadding = frame.contentEndPadding,
                    scrollState = frame.scroll.reader,
                    contentState = frame.scroll.readerContent,
                    endCta = { readerEndCta(note.id) },
                )
            }
        }
    }
