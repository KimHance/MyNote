package com.hancekim.mynote.core.designsystem.block

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RawSliceTest {
    private fun numbered(count: Int): String = (1..count).joinToString("\n") { "line $it" }

    @Test
    fun `300줄 원문은 1단에서 40줄 2단에서 200줄까지 보이고 남는다`() {
        val raw = numbered(300)

        val first = sliceRaw(raw, maxLines = 40)
        val second = sliceRaw(raw, maxLines = 200)

        assertEquals(40, first.lines.size)
        assertTrue(first.hasMore)
        assertEquals(200, second.lines.size)
        assertTrue(second.hasMore)
        assertEquals("line 200", second.lines.last())
    }

    @Test
    fun `71줄 원문은 2단에서 다 보여 31줄을 더 보인다`() {
        val raw = numbered(71)

        val first = sliceRaw(raw, maxLines = 40)
        val second = sliceRaw(raw, maxLines = 200)

        assertTrue(first.hasMore)
        assertEquals(71, second.lines.size)
        assertFalse(second.hasMore)
        assertEquals(31, second.lines.size - first.lines.size)
    }

    @Test
    fun `400자 줄은 자르지 않는다`() {
        val long = "a".repeat(400)

        val slice = sliceRaw("{\n$long\n}", maxLines = 40)

        assertEquals(listOf("{", long, "}"), slice.lines)
        assertFalse(slice.hasMore)
    }

    @Test
    fun `혼자 10000자를 넘는 줄은 그 줄만 10000자에서 자르고 멈춘다`() {
        val raw = "b".repeat(12_000) + "\nnext"

        val slice = sliceRaw(raw, maxLines = 200)

        assertEquals(1, slice.lines.size)
        assertEquals(10_000, slice.lines.single().length)
        assertTrue(slice.hasMore)
    }

    @Test
    fun `10000자에 닿는 줄은 줄 중간이 아니라 통째로 빠진다`() {
        val raw = listOf("c".repeat(6_000), "d".repeat(6_000)).joinToString("\n")

        val slice = sliceRaw(raw, maxLines = 200)

        assertEquals(listOf("c".repeat(6_000)), slice.lines)
        assertTrue(slice.hasMore)
    }

    @Test
    fun `1단에서 이미 10000자에 닿으면 2단도 더 보이는 줄이 없다`() {
        val raw = (1..60).joinToString("\n") { "e".repeat(300) }

        val first = sliceRaw(raw, maxLines = 40)
        val second = sliceRaw(raw, maxLines = 200)

        assertEquals(first.lines.size, second.lines.size)
        assertTrue(second.hasMore)
    }

    @Test
    fun `빈 원문은 줄이 없고 남은 것도 없다`() {
        val slice = sliceRaw("", maxLines = 40)

        assertTrue(slice.lines.isEmpty())
        assertFalse(slice.hasMore)
    }

    @Test
    fun `1줄 원문은 그대로다`() {
        val slice = sliceRaw("{\"type\":\"timeline\"}", maxLines = 40)

        assertEquals(listOf("{\"type\":\"timeline\"}"), slice.lines)
        assertFalse(slice.hasMore)
    }

    @Test
    fun `CRLF 를 나누고 끝 개행 뒤 빈 줄은 버린다`() {
        val slice = sliceRaw("{\r\n  \"a\": 1\r\n}\r\n", maxLines = 40)

        assertEquals(listOf("{", "  \"a\": 1", "}"), slice.lines)
        assertFalse(slice.hasMore)
    }

    @Test
    fun `앞 공백은 한 칸 탭은 두 칸으로 센다`() {
        assertEquals(0, leadingCells("{"))
        assertEquals(4, leadingCells("    \"type\": \"table\""))
        assertEquals(5, leadingCells("\t\t \"x\""))
        assertEquals(3, leadingCells("   "))
    }
}
