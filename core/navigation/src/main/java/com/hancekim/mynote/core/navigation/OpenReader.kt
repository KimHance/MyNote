package com.hancekim.mynote.core.navigation

import com.hancekim.mynote.core.navigation.NavCommand
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.key.NotesNavKey

/** 노트 리더를 여는 명령. 설정이 맨 위면 설정을 걷고 그 자리에 리더를 둔다. */
fun List<MyNoteNavKey>.openReaderCommand(noteId: String): NavCommand {
    val reader = NotesNavKey.Reader(noteId)
    if (lastOrNull() !is NotesNavKey.Settings) return NavCommand.Push(reader)
    val below = dropLast(1)
    return NavCommand.ReplaceAll(if (below.lastOrNull() == reader) below else below + reader)
}
