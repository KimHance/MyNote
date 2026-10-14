package com.hancekim.mynote.feature.notes.element

/**
 * 홈 본문 상태 넷. 연결 없음 · 노트 0 은 첫 실행 빈 상태(LN-1)이고, 연결 있음의 두 빈 상태(받는 중 · 노트 없음)는 RC-21 이다.
 */
enum class HomeBody { Notes, Receiving, NoNotes, FirstRun }

fun homeBodyOf(
    connected: Boolean,
    noteCount: Int,
    syncing: Boolean,
): HomeBody =
    when {
        noteCount > 0 -> HomeBody.Notes
        !connected -> HomeBody.FirstRun
        syncing -> HomeBody.Receiving
        else -> HomeBody.NoNotes
    }
