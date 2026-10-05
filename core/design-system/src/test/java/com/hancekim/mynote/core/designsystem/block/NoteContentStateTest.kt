package com.hancekim.mynote.core.designsystem.block

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteContentStateTest {
    private fun state() = NoteContentState(emptySet(), emptySet())

    @Test
    fun `블록마다 펼침이 따로다`() {
        val state = state()

        state.toggle(2)

        assertTrue(state.isExpanded(2))
        assertFalse(state.isExpanded(5))
    }

    @Test
    fun `접으면 2단도 풀려 다시 펼칠 때 1단부터다`() {
        val state = state()
        state.toggle(2)
        state.showAll(2)

        state.toggle(2)
        state.toggle(2)

        assertTrue(state.isExpanded(2))
        assertFalse(state.isShowingAll(2))
    }

    @Test
    fun `저장했다 되살리면 펼침과 2단이 그대로다`() {
        val state = state()
        state.toggle(1)
        state.toggle(4)
        state.showAll(4)

        val saver = NoteContentState.Saver
        val saved = with(saver) { SaverScope { true }.save(state) }
        val restored = saver.restore(requireNotNull(saved))
        requireNotNull(restored)

        assertTrue(restored.isExpanded(1))
        assertTrue(restored.isExpanded(4))
        assertFalse(restored.isShowingAll(1))
        assertTrue(restored.isShowingAll(4))
    }
}
