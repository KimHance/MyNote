package com.hancekim.mynote.shell

enum class ConnectionKnown { Unknown, None, Connected }

enum class NotesKnown { Unknown, Empty, Some }

/** 시작 결과 — 읽는 중이면 아무 화면도 그리지 않고, 아니면 셸(홈)이다. 연결 화면은 시작 결과가 아니다(RC-1 · local-notes 결정 4). */
enum class StartDestination { Reading, Shell }

/**
 * 시작 판정(RC-1). 연결 · 노트 중 하나라도 아직 모르거나, 연결이 없고 시드가 아직 소비 전이면 읽는 중이다.
 * 그 밖은 모두 셸이다 — 노트 0 개면 홈 첫 실행 빈 상태(LN-1), 연결 안내 띠는 홈이 정한다.
 */
fun startDestination(
    connection: ConnectionKnown,
    notes: NotesKnown,
    seedPending: Boolean,
): StartDestination =
    when {
        connection == ConnectionKnown.Unknown || notes == NotesKnown.Unknown -> StartDestination.Reading
        connection == ConnectionKnown.None && seedPending -> StartDestination.Reading
        else -> StartDestination.Shell
    }
