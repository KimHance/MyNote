package com.hancekim.mynote.core.ui.element

import kotlinx.collections.immutable.persistentMapOf
import org.junit.Assert.assertEquals
import org.junit.Test

class NotesSignalTest {
    @Test
    fun `보여 준 신호는 다시 그리지 않는다`() {
        val shown = persistentMapOf(NotesSignal.Notice to 3)

        assertEquals(0, shown.unseen(NotesSignal.Notice, 3))
    }

    @Test
    fun `새 신호는 보여 준 적이 있어도 그린다`() {
        val shown = persistentMapOf(NotesSignal.Notice to 3)

        assertEquals(4, shown.unseen(NotesSignal.Notice, 4))
    }

    @Test
    fun `신호는 종류마다 따로 센다`() {
        val shown = persistentMapOf(NotesSignal.Error to 2)

        assertEquals(2, shown.unseen(NotesSignal.Info, 2))
        assertEquals(1, shown.unseen(NotesSignal.ExitHint, 1))
    }
}
