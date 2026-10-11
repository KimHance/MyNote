package com.hancekim.mynote.feature.notes

import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.feature.notes.element.CtaAction
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.frame.ShellDestinations
import com.hancekim.mynote.feature.notes.frame.sceneEdgeModifier
import com.hancekim.mynote.feature.notes.screen.HomeScreen
import com.hancekim.mynote.feature.notes.screen.NoteReaderScreen
import com.hancekim.mynote.feature.notes.screen.bookmarks.BookmarksScreen

/**
 * 노트 기능의 목적지 — 홈 · 북마크 · 리더. 노트 상태는 화면 안에서 읽는다(다른 기능 상태가 바뀌어도 다시 그리지 않는다).
 *
 * 리더 끝 CTA 는 퀴즈 · 모델 받기 쪽 값이라 셸이 묶어 넘긴다 — [modelStatus] 와 [onQuizAction](노트 id, 행동).
 */
internal fun notesDestinations(
    state: () -> NotesUiState,
    onIntent: (NotesIntent) -> Unit,
    modelStatus: () -> ModelStatus,
    onQuizAction: (String, CtaAction) -> Unit,
): ShellDestinations =
    { frame ->
        destination<NotesNavKey.Home> {
            val notes = state()
            HomeScreen(
                recentNotes = notes.recentNotes,
                body = notes.homeBody,
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
                    onQuizAction = { action -> onQuizAction(note.id, action) },
                    modelStatus = modelStatus(),
                )
            }
        }
    }
