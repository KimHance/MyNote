package com.hancekim.mynote.core.navigation.registry

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.SceneStrategyScope
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.navigation.scene.ListDetailSceneStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationsTest {
    private val connect: DestinationRegistration<Unit> = { destination<NotesNavKey.Connect> {} }
    private val home: DestinationRegistration<Unit> = { destination<NotesNavKey.Home> {} }

    @Test
    fun `FM-4 등록 틀은 연결 화면의 기본 내용 키를 바꾸지 않는다`() {
        val provider = destinationProvider(Unit, listOf(home, connect))

        assertEquals(NavEntry(NotesNavKey.Connect) {}.contentKey, provider(NotesNavKey.Connect).contentKey)
    }

    @Test
    fun `FM-4 등록한 연결 화면은 넓은 창에서도 한 칸이다`() {
        // 넓은 창 판정(RC-14)이 기본 내용 키로 연결 화면을 알아본다 — 등록 틀을 거쳐도 그대로여야 한다
        val provider = destinationProvider(Unit, listOf(home, connect))
        val strategy =
            ListDetailSceneStrategy<MyNoteNavKey>(sceneKey = "list-detail", panel = {}, header = {}) { true }

        val scene =
            with(strategy) {
                SceneStrategyScope<MyNoteNavKey>().calculateScene(
                    listOf(provider(NotesNavKey.Home), provider(NotesNavKey.Connect)),
                )
            }

        assertNull(scene)
    }

    @Test
    fun `FM-4 같은 목적지를 두 번 등록하면 묶음을 만들 때 멈춘다`() {
        val again: DestinationRegistration<Unit> = { destination<NotesNavKey.Home> {} }

        val failure = runCatching { destinationProvider(Unit, listOf(home, again)) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `FM-4 등록하지 않은 목적지는 묶음이 모른다`() {
        val provider = destinationProvider(Unit, listOf(home))

        val failure = runCatching { provider(NotesNavKey.Settings) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
    }

    @Test
    fun `FM-4 등록 묶음은 셸이 준 틀 값을 등록에 넘긴다`() {
        var seen: String? = null
        val registration: DestinationRegistration<String> = { frame -> seen = frame }

        destinationProvider("틀", listOf(registration))

        assertEquals("틀", seen)
    }
}
