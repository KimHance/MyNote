package com.hancekim.mynote.feature.notes.shell

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.hancekim.mynote.core.designsystem.component.FloatingNavBar
import com.hancekim.mynote.core.designsystem.component.FloatingNavItem
import com.hancekim.mynote.core.designsystem.component.FloatingNavRail
import com.hancekim.mynote.core.designsystem.component.FloatingNavScrollState
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.ScrollToTopButton
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.navigation.NotesTab
import com.hancekim.mynote.core.ui.shell.NotesScrollState
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.quiz.QuizUiState
import com.kyant.backdrop.Backdrop
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

internal const val NAV_HOME = "home"
internal const val NAV_BOOKMARK = "bookmark"
internal const val NAV_STATS = "stats"

@Composable
internal fun BoxScope.NotesOverlays(
    states: FeatureStates,
    intents: FeatureIntents,
    layout: NotesLayout,
    scroll: NotesScrollState,
    navScroll: FloatingNavScrollState,
    backdrop: Backdrop,
    pillVisible: Boolean,
) {
    val adaptive = MyNoteTheme.adaptive
    val scope = rememberCoroutineScope()
    val shell = states.shell
    val quizOpen = states.quiz.quiz != QuizUiState.Idle

    ModelPill(
        status = shell.modelStatus,
        visible = pillVisible,
        hidden = navScroll.hidden || quizOpen,
        topOffset = layout.headerOffset,
        backdrop = backdrop,
        onClick = { intents.shell(ShellIntent.OpenSettings) },
    )

    FloatingNav(
        selectedId =
            when (shell.selectedTab) {
                NotesTab.Home -> NAV_HOME
                NotesTab.Bookmarks -> NAV_BOOKMARK
                NotesTab.Stats -> NAV_STATS
            },
        onSelect = { id ->
            when (id) {
                NAV_HOME -> intents.shell(ShellIntent.SwitchTab(NotesTab.Home))
                NAV_BOOKMARK -> intents.shell(ShellIntent.SwitchTab(NotesTab.Bookmarks))
                NAV_STATS -> intents.shell(ShellIntent.SwitchTab(NotesTab.Stats))
            }
        },
        hidden = navScroll.hidden || quizOpen || shell.isSettingsOpen || shell.isConnectOpen,
        layout = layout,
        backdrop = backdrop,
    )

    ScrollToTopButton(
        visible =
            scrollToTopVisible(
                scrolled = scroll.showScrollToTop,
                quizOpen = quizOpen,
                connectOpen = shell.isConnectOpen,
                tokenEditing = states.settings.tokenEditing,
            ),
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
            FloatingNavItem(NAV_BOOKMARK, bookmark, MyNoteIcons.Bookmark),
            FloatingNavItem(NAV_STATS, stats, MyNoteIcons.Stats),
        )
    }
}
