package com.hancekim.mynote.feature.notes.element

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenEditTest {
    private val viewing = TokenEditState.Viewing
    private val editing = TokenEditState(editing = true)
    private val verifying = TokenEditState(editing = true, verifying = true)

    private fun step(
        state: TokenEditState,
        event: TokenEditEvent,
    ) = reduceTokenEdit(state, event)

    @Test
    fun `RC-18 토큰 편집 상태 전이표 D37 D39 포함`() {
        assertEquals(editing, step(viewing, TokenEditEvent.Start).next)

        val clear = step(viewing, TokenEditEvent.Clear(hasToken = true))
        assertEquals(TokenEditState(undoable = true), clear.next)
        assertEquals(listOf(TokenEditEffect.ClearStored), clear.effects)

        val save = step(editing, TokenEditEvent.Save(blank = false))
        assertEquals(verifying, save.next)
        assertEquals(listOf(TokenEditEffect.Verify), save.effects)

        val ok = step(verifying, TokenEditEvent.VerifySucceeded)
        assertEquals(TokenEditState.Viewing, ok.next)
        assertTrue(TokenEditEffect.Saved in ok.effects)

        val failed = step(verifying, TokenEditEvent.VerifyFailed(TokenFailureKind.Rejected))
        assertEquals(TokenEditState(editing = true, failure = TokenFailureKind.Rejected), failed.next)
        assertTrue(failed.effects.isEmpty())

        val back = step(verifying, TokenEditEvent.Dismiss)
        assertEquals(editing, back.next)
        assertEquals(listOf(TokenEditEffect.CancelRequest), back.effects)

        val blank = step(editing, TokenEditEvent.Save(blank = true))
        assertEquals(editing, blank.next)
        assertTrue(blank.effects.isEmpty())

        val cancel = step(editing, TokenEditEvent.Dismiss)
        assertEquals(viewing, cancel.next)
        assertEquals(listOf(TokenEditEffect.DiscardInput), cancel.effects)

        val enter = step(editing, TokenEditEvent.EnterSettings)
        assertEquals(viewing, enter.next)

        val undoable = TokenEditState(undoable = true)
        val undo = step(undoable, TokenEditEvent.Undo)
        assertEquals(viewing, undo.next)
        assertEquals(listOf(TokenEditEffect.RestoreStored), undo.effects)

        val saved =
            step(TokenEditState(editing = true, verifying = true, undoable = true), TokenEditEvent.VerifySucceeded)
        assertFalse(saved.next.undoable)
    }

    @Test
    fun `RC-18 토큰 편집 상태 전이표 D37 확인 중에는 저장소 바꾸기와 끊기가 잠긴다`() {
        assertTrue(verifying.locksRepoActions)
        assertEquals(listOf(TokenEditEffect.Blocked), step(verifying, TokenEditEvent.ChangeRepository).effects)
        assertEquals(listOf(TokenEditEffect.Blocked), step(verifying, TokenEditEvent.Disconnect).effects)
        assertTrue(step(editing, TokenEditEvent.ChangeRepository).effects.isEmpty())
        assertTrue(step(viewing, TokenEditEvent.Disconnect).effects.isEmpty())
        assertFalse(editing.locksRepoActions)
        assertEquals(verifying, step(verifying, TokenEditEvent.Disconnect).next)
    }

    @Test
    fun `RC-18 토큰 편집 상태 전이표 D39 확인 중 취소는 요청만 취소하고 입력을 유지한다`() {
        val cancelled = step(verifying, TokenEditEvent.Dismiss)

        assertTrue(cancelled.next.editing)
        assertFalse(cancelled.next.verifying)
        assertEquals(listOf(TokenEditEffect.CancelRequest), cancelled.effects)
        assertFalse(TokenEditEffect.DiscardInput in cancelled.effects)
    }

    @Test
    fun `RC-18 확인 성공에만 편집기가 닫힌다`() {
        assertEquals(viewing, step(verifying, TokenEditEvent.VerifySucceeded).next)
        assertTrue(step(verifying, TokenEditEvent.VerifyFailed(TokenFailureKind.NotVisible)).next.editing)
        assertTrue(step(verifying, TokenEditEvent.Dismiss).next.editing)
        assertEquals(editing, step(editing, TokenEditEvent.VerifySucceeded).next)
    }

    @Test
    fun `RC-18 새 토큰 확인에 실패하면 기존 토큰이 남는다`() {
        val failed = step(verifying, TokenEditEvent.VerifyFailed(TokenFailureKind.Rejected))

        assertFalse(TokenEditEffect.Saved in failed.effects)
        assertFalse(TokenEditEffect.ClearStored in failed.effects)
        assertFalse(TokenEditEffect.RestoreStored in failed.effects)
    }

    @Test
    fun `RC-18 지운 직후의 지우기는 저장소 값을 한 번만 지운다`() {
        val first = step(editing, TokenEditEvent.Clear(hasToken = true))
        assertTrue(TokenEditEffect.DiscardInput in first.effects)
        assertEquals(TokenEditState(undoable = true), first.next)

        val noToken = step(first.next, TokenEditEvent.Clear(hasToken = false))
        assertEquals(first.next, noToken.next)
        assertTrue(noToken.effects.isEmpty())
        assertEquals(verifying, step(verifying, TokenEditEvent.Clear(hasToken = true)).next)
    }
}
