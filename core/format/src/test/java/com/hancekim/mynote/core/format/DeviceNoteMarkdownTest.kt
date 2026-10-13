package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.model.DeviceNoteDraft
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteLimits
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

private val TODAY: LocalDate = LocalDate.of(2030, 1, 1)

private fun frontMatterBundle(): File =
    generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .map { File(it, "notes-tests/fixtures/front-matter") }
        .first { File(it, "cases.json").isFile }

private fun draft(
    title: String,
    body: String,
    date: LocalDate = TODAY,
) = DeviceNoteDraft(title = title, body = body, date = date)

class DeviceNoteMarkdownTest {
    private val rules = DeviceNoteMarkdown()

    @Test
    fun `LN-6 가져오기 규칙 표`() {
        val oct1 = LocalDate.of(2026, 10, 1)
        val cases: List<Triple<String, String?, DeviceNoteDraft>> =
            listOf(
                Triple("---\ntitle: 제목\ndate: 2026.10.01\n---\n본문", null, draft("제목", "본문", oct1)),
                Triple("---\ntitle: \"Flow: 기초\"\n---\n\n본문", "flow", draft("Flow: 기초", "본문")),
                Triple("# 제목\n본문 첫 줄", "flow", draft("제목", "본문 첫 줄")),
                Triple("# 제목\n\n본문", null, draft("제목", "본문")),
                Triple("\n\n# 제목\n본문", null, draft("제목", "본문")),
                Triple("## 둘째\n본문", "flow", draft("flow", "## 둘째\n본문")),
                Triple("제목\n===\n본문", null, draft("", "제목\n===\n본문")),
                Triple("# 가 **나** #\n본문", null, draft("가 **나**", "본문")),
                Triple("# 제목#\n본문", null, draft("제목#", "본문")),
                Triple("#제목\n본문", "h", draft("h", "#제목\n본문")),
                Triple("# #\n본문", "h", draft("h", "# #\n본문")),
                Triple("본문만", "flow", draft("flow", "본문만")),
                Triple("본문", "공유 제목", draft("공유 제목", "본문")),
                Triple("본문", null, draft("", "본문")),
                Triple("---\ntitle: x\n# 본문", "h", draft("h", "---\ntitle: x\n# 본문")),
                Triple("---\ntitle: t\ndate: 2026-10-14\n---\nb", null, draft("t", "b")),
                Triple("---\ntitle: t\ndate: 2026.02.30\n---\nb", null, draft("t", "b")),
                Triple("---\ndate: 2026.10.01\n---\n# 머리\nb", null, draft("머리", "b", oct1)),
                Triple("---\ntitle: 앞\n---\n# 본문 제목\n글", null, draft("앞", "# 본문 제목\n글")),
                Triple("---\ntitle:   \n---\n# 머리\nb", null, draft("머리", "b")),
                Triple("---\n---\n", "빈 앞머리", draft("빈 앞머리", "")),
                Triple("# " + "가".repeat(300) + "\nb", null, draft("가".repeat(200), "b")),
            )

        assertTrue(cases.size >= 10)
        cases.forEach { (text, hint, expected) ->
            assertEquals(text, expected, rules.imported(text, hint, TODAY))
        }
    }

    @Test
    fun `LN-6 CRLF 와 BOM 원문은 LF 원문과 같다`() {
        val lf = "---\ntitle: 제목\ndate: 2026.10.01\n---\n\n# 둘째 제목\n본문\n\n- 항목\n"

        val expected = rules.imported(lf, "h", TODAY)

        assertEquals(expected, rules.imported(lf.replace("\n", "\r\n"), "h", TODAY))
        assertEquals(expected, rules.imported("﻿" + lf.replace("\n", "\r\n"), "h", TODAY))
        assertEquals(expected, rules.imported(lf.replace("\n", "\r"), "h", TODAY))
    }

    @Test
    fun `LN-6 앞머리 경계는 NF-1 경계 묶음과 같다`() {
        val bundle = frontMatterBundle()
        val cases = Json.parseToJsonElement(File(bundle, "cases.json").readText()).jsonArray.map { it.jsonObject }

        assertTrue(cases.isNotEmpty())
        cases.forEach { case ->
            val file = case.getValue("file").jsonPrimitive.content
            val raw = File(bundle, file).readBytes().decodeToString()
            val body = case.getValue("body").jsonPrimitive.content
            val source = markdownSource(raw)
            val imported = rules.imported(raw, "이름", TODAY)
            val fromFrontMatter = source.head.any { it.startsWith("title:") }

            when {
                fromFrontMatter -> {
                    assertEquals(file, "경계", imported.title)
                    assertEquals(file, body, imported.body)
                    assertEquals(file, LocalDate.of(2026, 10, 7), imported.date)
                }

                body.startsWith("# 본문\n") -> {
                    assertEquals(file, "본문", imported.title)
                    assertEquals(file, body.removePrefix("# 본문\n").trimStart('\n'), imported.body)
                }

                else -> {
                    assertEquals(file, "이름", imported.title)
                    assertEquals(file, body, imported.body)
                }
            }
            assertFalse(file, source.frontMatter && imported.body.startsWith("---\n"))
        }
    }

    @Test
    fun `LN-3 크기 경계는 저장 원문 기준으로 리더 한도와 같다`() {
        val max = NoteLimits.Default.maxBytes - EMPTY_FRONT_MATTER_BYTES
        val bodies =
            listOf(
                "a".repeat(max) to true,
                "a".repeat(max + 1) to false,
                "a".repeat(NoteLimits.Default.maxBytes + 1) to false,
                "가".repeat(max / 3) to true,
                "가".repeat(max / 3 - 1) + "aaa" to true,
                "가".repeat(max / 3) + "a" to false,
            )

        assertEquals(524_280, max)
        assertEquals(EMPTY_FRONT_MATTER_BYTES, deviceNoteRaw("").toByteArray().size)
        bodies.forEach { (body, fits) ->
            val label = "${body.toByteArray().size} 바이트"
            assertEquals(label, fits, rules.fits(body))
            assertEquals(label, !fits, exceedsMarkdownLimits(deviceNoteRaw(body)))
        }
        val largest = NoteFormat().parse(deviceNotePath("n"), deviceNoteRaw("a".repeat(max)))
        assertFalse(largest.any { it is NoteBlock.Unsupported })
    }

    @Test
    fun `LN-7 가져올 원문의 크기 경계는 512KB 다`() {
        assertTrue(rules.rawFits("a".repeat(524_288)))
        assertFalse(rules.rawFits("a".repeat(524_289)))
        assertFalse(rules.rawFits("가".repeat(174_763)))
    }

    @Test
    fun `LN-3 본문 첫 줄 구분선은 앞머리로 먹히지 않는다`() {
        val body = "---\ntitle: x\n---\n가"

        val blocks = NoteFormat().parse(deviceNotePath("n"), deviceNoteRaw(body))

        assertEquals(NoteBlock.Divider, blocks.first())
        assertTrue(blocks.toString(), blocks.drop(1).any { it.toString().contains("title: x") })
        assertEquals(body, deviceNoteBody(deviceNoteRaw(body)))
    }

    @Test
    fun `LN-3 본문은 저장 원문에서 그대로 되돌아온다`() {
        listOf("", "가", "\n---\n", "---\n---\n두 번", "﻿본문", "끝 줄바꿈\n\n").forEach { body ->
            assertEquals(body, deviceNoteBody(deviceNoteRaw(body)))
        }
        assertTrue(deviceNotePath("abc").endsWith(".md"))
    }

    @Test
    fun `LN-3 다른 한도를 넘는 본문은 상자로 저장된다`() {
        val body = "> ".repeat(33) + "깊음"

        val box = NoteFormat().parse(deviceNotePath("n"), deviceNoteRaw(body)).single() as NoteBlock.Unsupported

        assertTrue(rules.fits(body))
        assertEquals(UnsupportedReason.LimitExceeded, box.reason)
    }
}
