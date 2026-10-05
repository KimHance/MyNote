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
import androidx.compose.ui.platform.LocalWindowInfo
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
    val contentEndPadding: Dp,
    val scrollToTopBottom: Dp,
    val twoPane: Boolean,
)

@Composable
internal fun rememberNotesLayout(panelOpen: Boolean): NotesLayout {
    val adaptive = MyNoteTheme.adaptive
    val windowWidth = LocalWindowInfo.current.containerDpSize.width
    val direction = LocalLayoutDirection.current
    val navInsets = WindowInsets.navigationBars.asPaddingValues()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = navInsets.calculateBottomPadding()
    val start = navInsets.calculateStartPadding(direction)
    val end = navInsets.calculateEndPadding(direction)

    return remember(adaptive, bottom, start, end, statusTop, windowWidth, panelOpen) {
        NotesLayout(
            bottomInset = bottom,
            startInset = start,
            endInset = end,
            headerOffset = if (adaptive.useModalDrawer) 0.dp else adaptive.headerHeight + statusTop,
            contentBottomPadding = if (adaptive.useFloatingRail) bottom else FLOATING_BAR_RESERVE + bottom,
            contentEndPadding = if (adaptive.useFloatingRail) railReserve(adaptive, end) else 0.dp,
            scrollToTopBottom = scrollToTopBottom(adaptive, bottom),
            twoPane = contentTwoPane(windowWidth, adaptive, panelOpen, end),
        )
    }
}

internal fun contentTwoPane(
    windowWidth: Dp,
    adaptive: AdaptiveTokens,
    panelOpen: Boolean,
    endInset: Dp = 0.dp,
): Boolean {
    val panel = if (panelOpen && !adaptive.useModalDrawer) adaptive.paneWidth else 0.dp
    val endPadding = if (adaptive.useFloatingRail) railReserve(adaptive, endInset) else 0.dp
    return windowWidth - panel - adaptive.page - endPadding - adaptive.page >= TWO_PANE_MIN_CONTENT
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

private fun railReserve(
    adaptive: AdaptiveTokens,
    endInset: Dp,
): Dp =
    (adaptive.railRightMargin + endInset + FloatingNavDefaults.RailWidth + FLOATING_RAIL_GAP - adaptive.page)
        .coerceAtLeast(0.dp)

private val FLOATING_BAR_RESERVE = 92.dp
private val TWO_PANE_MIN_CONTENT = 640.dp
private val FLOATING_RAIL_GAP = 12.dp
