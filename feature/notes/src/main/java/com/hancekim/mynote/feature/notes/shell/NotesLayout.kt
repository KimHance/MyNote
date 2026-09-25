package com.hancekim.mynote.feature.notes.shell

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.window.AdaptiveTokens
import com.hancekim.mynote.core.designsystem.component.FloatingNavDefaults
import com.hancekim.mynote.core.designsystem.component.ScrollToTopDefaults
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Immutable
internal data class NotesLayout(
    val bottomInset: Dp,
    val startInset: Dp,
    val endInset: Dp,
    val headerOffset: Dp,
    val contentBottomPadding: Dp,
    val scrollToTopBottom: Dp,
)

@Composable
internal fun rememberNotesLayout(): NotesLayout {
    val adaptive = MyNoteTheme.adaptive
    val direction = LocalLayoutDirection.current
    val navInsets = WindowInsets.navigationBars.asPaddingValues()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = navInsets.calculateBottomPadding()
    val start = navInsets.calculateStartPadding(direction)
    val end = navInsets.calculateEndPadding(direction)

    return remember(adaptive, bottom, start, end, statusTop) {
        NotesLayout(
            bottomInset = bottom,
            startInset = start,
            endInset = end,
            headerOffset = if (adaptive.useModalDrawer) 0.dp else adaptive.headerHeight + statusTop,
            contentBottomPadding = if (adaptive.useFloatingRail) bottom else FLOATING_BAR_RESERVE + bottom,
            scrollToTopBottom = scrollToTopBottom(adaptive, bottom),
        )
    }
}

private fun scrollToTopBottom(
    adaptive: AdaptiveTokens,
    bottomInset: Dp,
): Dp =
    if (adaptive.useFloatingRail) {
        adaptive.padY + bottomInset
    } else {
        adaptive.barBottomMargin + bottomInset +
            (FloatingNavDefaults.BarHeight - ScrollToTopDefaults.Size) / 2
    }

private val FLOATING_BAR_RESERVE = 92.dp
