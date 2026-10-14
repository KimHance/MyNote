package com.hancekim.mynote.core.navigation

import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val home = NotesNavKey.Home
private val settings = NotesNavKey.Settings
private val paste = NotesNavKey.Paste
private val readerA = NotesNavKey.Reader("a")
private val readerX = NotesNavKey.Reader("x")
private val newNote = NotesNavKey.Editor()
private val editX = NotesNavKey.Editor(noteId = "x")
private val shared1 = NotesNavKey.Editor(draftId = 1)
private val shared2 = NotesNavKey.Editor(draftId = 2)

/** 명령을 적용한 스택 — Navigator 의 reduce 와 같은 규칙(ReplaceAll 은 그대로 · Push 는 덧붙임). */
private fun List<MyNoteNavKey>.after(command: NavCommand): List<MyNoteNavKey> =
    when (command) {
        is NavCommand.Push -> this + command.key
        NavCommand.Pop -> dropLast(1)
        is NavCommand.ReplaceAll -> command.keys
    }

class EditorCommandsTest {
    @Test
    fun `LN-8 공유 진입 스택은 꺼져 있으면 홈과 편집기다`() {
        val cold = listOf<MyNoteNavKey>(home)

        assertEquals(listOf(home, shared1), cold.after(cold.openEditorCommand(shared1)))
    }

    @Test
    fun `LN-8 떠 있으면 지금 스택 위에 편집기다`() {
        val warm = listOf(home, readerA)

        assertEquals(NavCommand.Push(shared1), warm.openEditorCommand(shared1))
        assertEquals(listOf(home, readerA, shared1), warm.after(warm.openEditorCommand(shared1)))
    }

    @Test
    fun `LN-8 공유 위 공유는 편집기를 갈아 끼운다`() {
        val stack = listOf(home, readerA, shared1)

        assertEquals(listOf(home, readerA, shared2), stack.after(stack.openEditorCommand(shared2)))
        // 알림이 설정을 편집기 위에 쌓았어도 편집기는 하나다
        val deep = listOf(home, shared1, settings)
        assertEquals(listOf(home, settings, shared2), deep.after(deep.openEditorCommand(shared2)))
    }

    @Test
    fun `LN-6 붙여넣기 다음은 붙여넣기 자리를 편집기가 갈아 끼운다`() {
        val stack = listOf(home, paste)

        assertEquals(listOf(home, shared1), stack.after(stack.openEditorCommand(shared1)))
    }

    @Test
    fun `LN-3 저장 뒤 스택은 편집기 자리에 리더다`() {
        val stack = listOf(home, newNote)

        assertEquals(listOf(home, NotesNavKey.Reader("n1")), stack.after(stack.savedNoteCommand("n1")))
    }

    @Test
    fun `LN-9 고쳐 저장하면 원래 리더로 돌아간다`() {
        val stack = listOf(home, readerX, editX)

        assertEquals(listOf(home, readerX), stack.after(stack.savedNoteCommand("x")))
    }

    @Test
    fun `LN-11 지우면 그 노트의 리더와 편집기가 스택에서 빠진다`() {
        assertEquals(listOf(home), listOf(home, readerX).after(listOf(home, readerX).withoutNoteCommand("x")))
        assertEquals(
            listOf(home),
            listOf(home, readerX, editX).after(listOf(home, readerX, editX).withoutNoteCommand("x")),
        )
        // 빼고 나서 이웃이 같은 리더가 되면 하나로 합친다
        val sandwich = listOf(home, readerA, readerX, readerA)
        assertEquals(listOf(home, readerA), sandwich.after(sandwich.withoutNoteCommand("x")))
        // 다른 노트의 리더 · 편집기는 그대로다
        val other = listOf(home, readerA, NotesNavKey.Editor(noteId = "a"))
        assertEquals(other, other.after(other.withoutNoteCommand("x")))
    }

    @Test
    fun `편집기 판정은 맨 위와 스택 안을 가른다`() {
        assertTrue(listOf(home, newNote).isEditorTop())
        assertFalse(listOf(home, newNote, settings).isEditorTop())
        assertEquals(newNote, listOf(home, newNote, settings).editorKey())
        assertEquals(null, listOf<MyNoteNavKey>(home, readerA).editorKey())
    }
}
