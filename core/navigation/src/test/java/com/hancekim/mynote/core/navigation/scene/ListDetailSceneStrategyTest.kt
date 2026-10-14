package com.hancekim.mynote.core.navigation.scene

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategyScope
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListDetailSceneStrategyTest {
    private val strategy =
        ListDetailSceneStrategy<MyNoteNavKey>(
            sceneKey = SCENE_KEY,
            panel = {},
            header = {},
            panelExpanded = { true },
        )

    private fun entry(key: MyNoteNavKey) = NavEntry(key) {}

    private fun sceneOf(vararg keys: MyNoteNavKey): Scene<MyNoteNavKey> {
        val scope = SceneStrategyScope<MyNoteNavKey>()
        return with(strategy) { scope.calculateScene(keys.map(::entry)) }!!
    }

    @Test
    fun `상세가 바뀌어도 씬 키는 그대로다`() {
        // 키가 같아야 NavDisplay 가 씬 전체를 슬라이드시키지 않고, 씬이 본문만 페이드 스루로 바꾼다
        val home = sceneOf(NotesNavKey.Home)
        val reader = sceneOf(NotesNavKey.Home, NotesNavKey.Reader("a"))
        val otherReader = sceneOf(NotesNavKey.Home, NotesNavKey.Reader("b"))

        assertEquals(home.key, reader.key)
        assertEquals(reader.key, otherReader.key)
    }

    @Test
    fun `씬은 맨 위 엔트리 하나만 그린다`() {
        val scene = sceneOf(NotesNavKey.Home, NotesNavKey.Reader("a"))
        assertEquals(1, scene.entries.size)
        assertEquals(entry(NotesNavKey.Reader("a")).contentKey, scene.entries.single().contentKey)
    }

    @Test
    fun `노트를 보는 중이면 뒤로 갈 곳이 있다`() {
        val scene = sceneOf(NotesNavKey.Home, NotesNavKey.Reader("a"))
        assertEquals(1, scene.previousEntries.size)
        assertEquals(entry(NotesNavKey.Home).contentKey, scene.previousEntries.single().contentKey)
    }

    @Test
    fun `홈만 있으면 뒤로 갈 곳이 없다`() {
        // previousEntries 가 비면 NavDisplay 가 뒤로가기를 가져가지 않고 앱 종료 핸들러로 넘어간다
        val scene = sceneOf(NotesNavKey.Home)
        assertTrue(scene.previousEntries.isEmpty())
    }

    @Test
    fun `RC-14 연결 화면은 넓은 창에서도 한 칸이다`() {
        val scope = SceneStrategyScope<MyNoteNavKey>()

        val stacked =
            with(strategy) {
                scope.calculateScene(
                    listOf(entry(NotesNavKey.Home), entry(NotesNavKey.Settings), entry(NotesNavKey.Connect)),
                )
            }
        val root = with(strategy) { scope.calculateScene(listOf(entry(NotesNavKey.Connect))) }

        assertNull(stacked)
        assertNull(root)
    }

    @Test
    fun `RC-14 연결 화면이 맨 위가 아니면 장면은 그대로다`() {
        val scene = sceneOf(NotesNavKey.Connect, NotesNavKey.Home)
        assertEquals(entry(NotesNavKey.Home).contentKey, scene.entries.single().contentKey)
    }

    @Test
    fun `LN-3 편집기와 붙여넣기는 넓은 창에서 상세 칸이다`() {
        // 디자인 C8 — 목록 패널은 남고 편집기 · 붙여넣기가 상세 칸에 든다(헤더는 셸 슬롯이 비운다)
        val editor = sceneOf(NotesNavKey.Home, NotesNavKey.Reader("a"), NotesNavKey.Editor(noteId = "a"))
        val paste = sceneOf(NotesNavKey.Home, NotesNavKey.Paste)
        val home = sceneOf(NotesNavKey.Home)

        assertEquals(entry(NotesNavKey.Editor(noteId = "a")).contentKey, editor.entries.single().contentKey)
        assertEquals(entry(NotesNavKey.Paste).contentKey, paste.entries.single().contentKey)
        assertEquals(home.key, editor.key)
        assertEquals(2, editor.previousEntries.size)
    }

    private companion object {
        const val SCENE_KEY = "list-detail"
    }
}
