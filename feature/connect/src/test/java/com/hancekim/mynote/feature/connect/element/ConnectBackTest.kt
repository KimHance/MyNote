package com.hancekim.mynote.feature.connect.element

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectBackTest {
    @Test
    fun `RC-24 확인 중 뒤로는 요청을 취소하고 입력을 유지한다`() {
        assertEquals(ConnectBackAction.CancelProbe, connectBackAction(ConnectStep.Probing))
    }

    @Test
    fun `RC-24 첫 동기화 중 뒤로는 무시한다`() {
        assertEquals(ConnectBackAction.Ignore, connectBackAction(ConnectStep.Syncing))
        assertEquals(ConnectBackAction.Ignore, connectBackAction(ConnectStep.Done))
    }

    @Test
    fun `RC-1 쌓은 연결 화면의 뒤로는 이전 화면이다`() {
        assertEquals(ConnectBackAction.PopScreen, connectBackAction(ConnectStep.Editing))
    }
}
