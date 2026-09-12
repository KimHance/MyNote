package com.hancekim.mynote.core.navigation.key

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesNavKeyTest {
    @Test
    fun `백스택은 activeNoteId 하나로 결정된다`() {
        assertEquals(listOf(NotesNavKey.Home), notesBackStackOf(null))
        assertEquals(
            listOf(NotesNavKey.Home, NotesNavKey.Reader("compose-state")),
            notesBackStackOf("compose-state"),
        )
    }

    @Test
    fun `노트 키는 앱 공통 키 계약을 따른다`() {
        // :feature:root 가 하나의 백스택으로 여러 feature 를 태울 수 있으려면 이 상속이 유지돼야 한다
        val keys: List<MyNoteNavKey> = notesBackStackOf("a")
        assertEquals(2, keys.size)
    }
}
