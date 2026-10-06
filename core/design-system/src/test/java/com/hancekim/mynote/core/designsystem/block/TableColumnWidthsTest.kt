package com.hancekim.mynote.core.designsystem.block

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableColumnWidthsTest {
    private val em = 16
    private val target = em * 16
    private val floor = em * 6

    private fun widths(
        longestLine: IntArray,
        longestWord: IntArray,
        available: Int,
    ): IntArray = tableColumnWidths(longestLine, longestWord, available, target, floor)

    @Test
    fun `NF-5 다 들어가면 열마다 가장 긴 줄 폭이다`() {
        val result = widths(intArrayOf(70, 30, 50), intArrayOf(70, 30, 30), available = 277)

        assertArrayEquals(intArrayOf(70, 30, 50), result)
    }

    @Test
    fun `NF-5 목표 폭은 16em 에서 멈춘다`() {
        val result = widths(intArrayOf(900), intArrayOf(40), available = 2000)

        assertArrayEquals(intArrayOf(target), result)
    }

    @Test
    fun `NF-5 최소 폭만 들어가면 남는 폭을 목표와 최소의 차이 비율로 나눠 본문 폭을 채운다`() {
        val result = widths(intArrayOf(173, 400), intArrayOf(173, 60), available = 277)

        assertArrayEquals(intArrayOf(173, 104), result)
        assertEquals(277, result.sum())
    }

    @Test
    fun `NF-5 나눈 폭은 목표 폭을 넘지 않고 합이 본문 폭과 같다`() {
        val result = widths(intArrayOf(150, 151, 152), intArrayOf(10, 10, 10), available = 400)

        assertEquals(400, result.sum())
        assertTrue(result[0] <= 150 && result[1] <= 151 && result[2] <= 152)
        assertTrue(result.all { it >= floor })
    }

    @Test
    fun `NF-5 최소 폭도 넘치면 열마다 최소 폭으로 두고 가로로 스크롤한다`() {
        val result = widths(intArrayOf(200, 200, 200, 200, 200), intArrayOf(40, 40, 40, 40, 120), available = 277)

        assertArrayEquals(intArrayOf(floor, floor, floor, floor, 120), result)
    }

    @Test
    fun `NF-5 6em 보다 짧은 열은 줄을 바꾸지 않는다`() {
        val result = widths(intArrayOf(30, 900), intArrayOf(30, 80), available = 200)

        assertEquals(30, result[0])
    }

    @Test
    fun `NF-5 가장 긴 낱말이 최소 폭을 정한다`() {
        val result = widths(intArrayOf(300, 300), intArrayOf(250, 20), available = 100)

        assertArrayEquals(intArrayOf(250, floor), result)
    }
}
