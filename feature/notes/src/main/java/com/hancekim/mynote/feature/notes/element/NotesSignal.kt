package com.hancekim.mynote.feature.notes.element

import kotlinx.collections.immutable.PersistentMap

/** 화면이 한 번만 보여 주는 스낵바 신호. */
enum class NotesSignal {
    Error,
    Info,
    Notice,
    QuizUndo,
    ExitHint,
}

/** 아직 보여 주지 않은 신호면 [id], 이미 보여 줬으면 0. */
internal fun PersistentMap<NotesSignal, Int>.unseen(
    signal: NotesSignal,
    id: Int,
): Int = if (this[signal] == id) 0 else id
