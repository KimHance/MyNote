package com.hancekim.mynote.feature.connect.element

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectBackTest {
    @Test
    fun `RC-24 확인 중 뒤로는 요청을 취소하고 입력을 유지한다`() {
        assertEquals(ConnectBackAction.CancelProbe, connectBackAction(ConnectPlace.Root, ConnectStep.Probing))
        assertEquals(ConnectBackAction.CancelProbe, connectBackAction(ConnectPlace.Stacked, ConnectStep.Probing))
    }

    @Test
    fun `RC-24 첫 동기화 중 뒤로는 무시한다`() {
        assertEquals(ConnectBackAction.Ignore, connectBackAction(ConnectPlace.Root, ConnectStep.Syncing))
        assertEquals(ConnectBackAction.Ignore, connectBackAction(ConnectPlace.Stacked, ConnectStep.Syncing))
        assertEquals(ConnectBackAction.Ignore, connectBackAction(ConnectPlace.Stacked, ConnectStep.Done))
    }

    @Test
    fun `RC-1 첫 실행 루트의 뒤로는 종료 안내이고 쌓은 화면의 뒤로는 이전 화면이다`() {
        assertEquals(ConnectBackAction.ShowExitHint, connectBackAction(ConnectPlace.Root, ConnectStep.Editing))
        assertEquals(ConnectBackAction.PopScreen, connectBackAction(ConnectPlace.Stacked, ConnectStep.Editing))
    }
}
