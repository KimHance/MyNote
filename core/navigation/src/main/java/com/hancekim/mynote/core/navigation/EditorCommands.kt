package com.hancekim.mynote.core.navigation

import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.key.NotesNavKey

// 기기 노트 편집기 · 붙여넣기 · 지우기의 스택 계산(local-notes). 셋 다 바뀔 스택을 한 번에 계산해
// 명령 하나로 돌려준다 — 둘로 나눠 보내면 앞 명령이 쓰로틀 창을 열어 뒤 명령이 버려진다.

/** 맨 위가 편집기인가. */
fun List<MyNoteNavKey>.isEditorTop(): Boolean = lastOrNull() is NotesNavKey.Editor

/** 스택에 있는 편집기. 한 번에 하나다(LN-8 ③). */
fun List<MyNoteNavKey>.editorKey(): NotesNavKey.Editor? = lastOrNull { it is NotesNavKey.Editor } as NotesNavKey.Editor?

/**
 * 편집기를 연다. 스택에 편집기가 이미 있으면 그것을 빼고 맨 위에 새 편집기를 둔다(갈아 끼움 — LN-8 ③).
 * 붙여넣기가 맨 위면 그 자리를 갈아 끼운다(붙여넣기 `다음` — 뒤로 = 붙여넣기를 연 화면).
 */
fun List<MyNoteNavKey>.openEditorCommand(editor: NotesNavKey.Editor): NavCommand {
    val below =
        filterNot { it is NotesNavKey.Editor }.let {
            if (it.lastOrNull() ==
                NotesNavKey.Paste
            ) {
                it.dropLast(1)
            } else {
                it
            }
        }
    if (below.size == size) return NavCommand.Push(editor)
    return NavCommand.ReplaceAll(below.ifEmpty { listOf(NotesNavKey.Home) } + editor)
}

/**
 * 저장한 뒤 — 편집기 자리를 그 노트의 리더가 갈아 끼운다(LN-3 ⑥). 편집기 아래가 이미 그 노트 리더면(고치기) 편집기만 뺀다.
 * 뒤로 = 편집기를 연 화면이다.
 */
fun List<MyNoteNavKey>.savedNoteCommand(noteId: String): NavCommand {
    val reader = NotesNavKey.Reader(noteId)
    val below = filterNot { it is NotesNavKey.Editor }.ifEmpty { listOf(NotesNavKey.Home) }
    return NavCommand.ReplaceAll(if (below.last() == reader) below else below + reader)
}

/**
 * 노트를 지운 뒤 — 스택에서 그 노트의 리더와 그 노트를 고치던 편집기를 뺀다(LN-11). 빼고 나서 이웃이 같은 키가 되면 하나로 합치고,
 * 비면 홈이다. 바뀔 것이 없으면 지금 스택 그대로라 `Navigator` 가 무시한다.
 */
fun List<MyNoteNavKey>.withoutNoteCommand(noteId: String): NavCommand {
    val left =
        filterNot { it == NotesNavKey.Reader(noteId) || (it is NotesNavKey.Editor && it.noteId == noteId) }
            .fold(emptyList<MyNoteNavKey>()) { kept, key -> if (kept.lastOrNull() == key) kept else kept + key }
    return NavCommand.ReplaceAll(left.ifEmpty { listOf(NotesNavKey.Home) })
}
