package com.hancekim.mynote.core.designsystem.component

import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.VectorPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyNoteIconsTest {
    @Test
    fun `LN-3 기기 노트 아이콘 여섯은 핸드오프 path 를 선 하나로 읽는다`() {
        val icons =
            mapOf(
                "note_new" to MyNoteIcons.NewNote,
                "import" to MyNoteIcons.Import,
                "edit" to MyNoteIcons.Edit,
                "delete" to MyNoteIcons.Delete,
                "device" to MyNoteIcons.Device,
                "paste" to MyNoteIcons.Paste,
            )
        icons.forEach { (name, icon) ->
            assertEquals(name, icon.name)
            assertEquals(24f, icon.viewportWidth)
            val path = icon.root.single() as VectorPath
            assertEquals(1.7f, path.strokeLineWidth)
            assertTrue("$name 에 면이 없다", path.fill == null)
            assertTrue("$name 는 이동으로 시작한다", path.pathData.first() is PathNode.MoveTo)
        }
        // 이동 · 선 · 호가 모두 읽혔는지 — 휴지통은 이동 다섯 · 호 넷이다
        val delete = (MyNoteIcons.Delete.root.single() as VectorPath).pathData
        assertEquals(5, delete.count { it is PathNode.MoveTo || it is PathNode.RelativeMoveTo })
        assertEquals(4, delete.count { it is PathNode.RelativeArcTo })
    }
}
