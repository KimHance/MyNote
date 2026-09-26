package com.hancekim.mynote.feature.notes.shell

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.hancekim.mynote.core.designsystem.component.FloatingNavBar
import com.hancekim.mynote.core.designsystem.component.FloatingNavItem
import com.hancekim.mynote.core.designsystem.component.FloatingNavRail
import com.hancekim.mynote.core.designsystem.component.FloatingNavScrollState
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.ScrollToTopButton
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.quiz.QuizOverlay
import com.hancekim.mynote.feature.notes.quiz.QuizUiState
import com.kyant.backdrop.Backdrop
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

internal const val NAV_HOME = "home"

@Composable
internal fun BoxScope.NotesOverlays(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
    layout: NotesLayout,
    scroll: NotesScrollState,
    navScroll: FloatingNavScrollState,
    backdrop: Backdrop,
    selectedNavItem: String,
    onSelectNavItem: (String) -> Unit,
    quizStartInset: Dp,
) {
    val adaptive = MyNoteTheme.adaptive
    val scope = rememberCoroutineScope()
    val quizOpen = uiState.quiz != QuizUiState.Idle

    FloatingNav(
        selectedId = selectedNavItem,
        onSelect = onSelectNavItem,
        hidden = navScroll.hidden || quizOpen,
        layout = layout,
        backdrop = backdrop,
    )

    ScrollToTopButton(
        visible = scroll.showScrollToTop && !quizOpen,
        onClick = {
            navScroll.reset()
            scope.launch { scroll.scrollToTop() }
        },
        backdrop = backdrop,
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = adaptive.railRightMargin + layout.endInset,
                    bottom = layout.scrollToTopBottom,
                ),
    )

    QuizOverlay(
        state = uiState.quiz,
        noteTitle = uiState.activeNote?.title.orEmpty(),
        onIntent = onIntent,
        startInset = quizStartInset,
        backdrop = backdrop,
    )
}

@Composable
private fun BoxScope.FloatingNav(
    selectedId: String,
    onSelect: (String) -> Unit,
    hidden: Boolean,
    layout: NotesLayout,
    backdrop: Backdrop,
) {
    val adaptive = MyNoteTheme.adaptive
    val navItems = rememberFloatingNavItems()
    if (adaptive.useFloatingRail) {
        FloatingNavRail(
            items = navItems,
            selectedId = selectedId,
            onSelect = onSelect,
            hidden = hidden,
            rightMargin = adaptive.railRightMargin,
            backdrop = backdrop,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = layout.headerOffset + adaptive.padY,
                        end = adaptive.railRightMargin + layout.endInset,
                    ),
        )
    } else {
        FloatingNavBar(
            items = navItems,
            selectedId = selectedId,
            onSelect = onSelect,
            hidden = hidden,
            backdrop = backdrop,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = layout.startInset,
                        end = layout.endInset,
                        bottom = adaptive.barBottomMargin + layout.bottomInset,
                    ),
        )
    }
}

@Composable
private fun rememberFloatingNavItems(): ImmutableList<FloatingNavItem> {
    val home = stringResource(R.string.notes_nav_home)
    val bookmark = stringResource(R.string.notes_nav_bookmark)
    val stats = stringResource(R.string.notes_nav_stats)
    return remember(home, bookmark, stats) {
        persistentListOf(
            FloatingNavItem(NAV_HOME, home, MyNoteIcons.Home),
            FloatingNavItem("bookmark", bookmark, MyNoteIcons.Bookmark),
            FloatingNavItem("stats", stats, MyNoteIcons.Stats),
        )
    }
}
