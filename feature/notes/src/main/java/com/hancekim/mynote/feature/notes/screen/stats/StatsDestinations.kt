package com.hancekim.mynote.feature.notes.screen.stats

import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.frame.ShellDestinations
import com.hancekim.mynote.core.ui.frame.sceneEdgeModifier

/** 통계 기능의 목적지 — 통계 · 지난 요약. 상태는 화면이 자기 ViewModel(Activity owner)에서 받는다. */
internal fun statsDestinations(): ShellDestinations =
    { frame ->
        destination<NotesNavKey.Stats> {
            val modifier = frame.sceneEdgeModifier()
            StatsRoute(
                twoPane = frame.twoPane,
                listState = frame.scroll.statsList,
                sideState = frame.scroll.statsSide,
                modifier = modifier,
                contentBottomPadding = frame.contentBottomPadding,
                contentTopPadding = frame.contentTopPadding,
                contentEndPadding = frame.contentEndPadding,
            )
        }
        destination<NotesNavKey.StatsHistory> {
            val modifier = frame.sceneEdgeModifier()
            StatsHistoryRoute(
                listState = frame.scroll.statsHistory,
                modifier = modifier,
                contentBottomPadding = frame.contentBottomPadding,
                contentTopPadding = frame.contentTopPadding,
                contentEndPadding = frame.contentEndPadding,
            )
        }
    }
