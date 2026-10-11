package com.hancekim.mynote.core.format

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val MARKERS = listOf("* ", "+ ", "1. ", "10) ")

private fun nestedLines(
    depth: Int,
    step: String,
    marker: String = "- ",
): String = (0 until depth).joinToString("\n") { step.repeat(it) + marker + "x" }

private fun quotedList(
    depth: Int,
    marker: String,
): String {
    val quotes = "> ".repeat(depth - 16)
    return (0 until 16).joinToString("\n") { quotes + "  ".repeat(it) + marker + "x" }
}

private fun mixedLines(depth: Int): String =
    (0 until depth).joinToString("\n") { level ->
        buildString {
            repeat(level) { append(if (it % 2 == 0) "> " else "  ") }
            append(if (level % 2 == 0) "> x" else "- x")
        }
    }

class MarkdownLimitsTest {
    private fun assertReads(
        name: String,
        raw: String,
    ) = assertFalse(name, exceedsMarkdownLimits(raw))

    private fun assertExceeds(
        name: String,
        raw: String,
    ) = assertTrue(name, exceedsMarkdownLimits(raw))

    @Test
    fun `NF-8 32단은 읽고 33단은 한도 넘음이다`() {
        val cases =
            mapOf<String, (Int) -> String>(
                "> 한 줄" to { "> ".repeat(it) + "x" },
                ">> 한 줄" to { ">".repeat(it) + " x" },
                "- 한 줄" to { "- ".repeat(it) + "x" },
                "* + 1. 1) 섞은 한 줄" to { n -> (0 until n).joinToString("") { MARKERS[it % MARKERS.size] } + "x" },
                "> - 섞은 한 줄" to { n -> (0 until n).joinToString("") { if (it % 2 == 0) "> " else "- " } + "x" },
                "두 칸 들여쓰기" to { nestedLines(it, "  ") },
                "네 칸 들여쓰기" to { nestedLines(it, "    ") },
                "탭 들여쓰기" to { nestedLines(it, "\t") },
                "번호 목록 들여쓰기" to { nestedLines(it, "   ", marker = "1. ") },
                "> 줄마다 한 단" to { n -> (1..n).joinToString("\n") { ">".repeat(it) + " x" } },
                "> 와 목록을 섞은 들여쓰기" to { mixedLines(it) },
                "표시 뒤 공백 다섯" to { nestedLines(it, "  ", marker = "-     ") },
                "표시 뒤 탭 둘" to { nestedLines(it, "  ", marker = "-\t\t") },
                "인용과 섞은 표시 뒤 공백 다섯 목록" to { quotedList(it, "-     ") },
                "인용과 섞은 표시 뒤 탭 둘 목록" to { quotedList(it, "-\t\t") },
            )

        cases.forEach { (name, build) ->
            assertReads("$name 32", build(32))
            assertExceeds("$name 33", build(33))
        }
    }

    @Test
    fun `NF-8 512KB 를 넘으면 한도 넘음이다`() {
        assertReads("ASCII 524,288", "a".repeat(524_288))
        assertExceeds("ASCII 524,289", "a".repeat(524_289))

        val hangul = "가".repeat(524_288 / 3)
        assertReads("한글 524,286", hangul + "ab")
        assertExceeds("한글 524,289", hangul + "abc")

        val emoji = "\uD83D\uDE00".repeat(524_288 / 4)
        assertReads("평면 밖 524,288", emoji)
        assertExceeds("평면 밖 524,289", emoji + "a")
    }

    @Test
    fun `NF-8 줄 끝 CR 과 BOM 도 같은 깊이로 센다`() {
        val lf = nestedLines(33, "  ")

        assertExceeds("CRLF", lf.replace("\n", "\r\n"))
        assertExceeds("CR", lf.replace("\n", "\r"))
        assertExceeds("BOM", "\uFEFF" + "> ".repeat(33) + "x")
        assertReads("CRLF 32", nestedLines(32, "  ").replace("\n", "\r\n"))
    }

    @Test
    fun `NF-8 컨테이너가 아닌 줄 앞 글자는 세지 않는다`() {
        val plain =
            listOf(
                "-x",
                "1.5 배",
                "2026.10.07",
                "***",
                "-- 주석",
                "    - 들여쓴 코드",
                "|a|b|",
            ).joinToString("\n")
        assertReads("표시가 아닌 줄", (plain + "\n").repeat(2_000))

        val siblings = (1..5_000).joinToString("\n") { "- 항목 $it" }
        assertReads("형제 항목 5,000개", siblings)
        assertReads("빈 줄로 끝난 인용", ("> a\n\n").repeat(1_000))
    }

    @Test
    fun `NF-8 공백 낀 구분선은 목록 표시로 세지 않는다`() {
        assertReads("- 40개", "- ".repeat(40).trim())
        assertReads("* 40개", "* ".repeat(40).trim())
        assertReads("인용 안 - 40개", "> " + "-  ".repeat(40))
        assertReads("목록 안 * 40개", "- " + "* ".repeat(40).trim())
        assertExceeds("글이 붙으면 목록", "- ".repeat(33) + "x")
        assertExceeds("섞인 글자는 구분선이 아니다", "- ".repeat(33) + "* -")
    }

    @Test
    fun `NF-8 줄 앞부분이 품을 수 있는 컨테이너가 64 를 넘으면 한도 넘음이다`() {
        assertReads("들여쓰기 124칸", " ".repeat(124) + "- x")
        assertExceeds("들여쓰기 130칸", " ".repeat(130) + "- x")

        val lazy =
            (0 until 80).joinToString("\n") { level ->
                "  ".repeat(level) + "- x\n2) 이어지는 글"
            }
        assertExceeds("이어지는 글로 끊어 보인 들여쓰기 중첩", lazy)
    }

    @Test(timeout = 5_000)
    fun `NF-8 512KB 안의 표시 한 줄도 줄 길이에 비례한 시간으로 거른다`() {
        assertExceeds("표시 26만 개 뒤 글", "- ".repeat(262_143) + "x")
        assertReads("표시 26만 개짜리 구분선", "- ".repeat(262_143).trim())
        assertExceeds("인용 26만 개 뒤 글", "> ".repeat(262_143) + "x")
    }
}
