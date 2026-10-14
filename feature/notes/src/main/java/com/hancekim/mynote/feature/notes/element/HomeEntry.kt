package com.hancekim.mynote.feature.notes.element

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** 홈에서 노트를 만드는 입구와 저장소 입구(LN-1 · LN-2). */
enum class HomeAction { NewNote, Import, Connect }

/** 버튼 위계 — 강조(주) · 보조 · 조용한(DESIGN.md 버튼 위계). */
enum class EntryWeight { Primary, Secondary, Quiet }

@Immutable
data class HomeEntry(
    val action: HomeAction,
    val weight: EntryWeight,
)

/**
 * 홈 상태별 입구 표. 첫 실행 빈 상태는 `새 노트` 하나만 강조하고 `가져오기` 는 보조, `저장소 연결` 은 조용한 버튼이다(LN-1).
 * 그 밖의 상태(노트 있음 · 받는 중 · 노트 없음)는 히어로 힌트 아래 · 빈 상태 묶음 아래의 보조 버튼 둘이다(LN-2 · 디자인 21A).
 */
fun homeEntriesOf(body: HomeBody): ImmutableList<HomeEntry> =
    when (body) {
        HomeBody.FirstRun -> {
            persistentListOf(
                HomeEntry(HomeAction.NewNote, EntryWeight.Primary),
                HomeEntry(HomeAction.Import, EntryWeight.Secondary),
                HomeEntry(HomeAction.Connect, EntryWeight.Quiet),
            )
        }

        HomeBody.Notes, HomeBody.Receiving, HomeBody.NoNotes -> {
            persistentListOf(
                HomeEntry(HomeAction.NewNote, EntryWeight.Secondary),
                HomeEntry(HomeAction.Import, EntryWeight.Secondary),
            )
        }
    }
