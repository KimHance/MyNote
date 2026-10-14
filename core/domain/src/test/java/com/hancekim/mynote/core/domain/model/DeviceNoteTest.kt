package com.hancekim.mynote.core.domain.model

import com.hancekim.mynote.core.domain.repository.DeviceNoteText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** 본문 바이트만 센다 — 크기 판정은 `:core:format` 시험이 본다. */
private class ByteRules(
    private val maxBody: Int,
) : DeviceNoteText {
    override fun fits(body: String): Boolean = body.toByteArray().size <= maxBody

    override fun rawFits(text: String): Boolean = true

    override fun imported(
        text: String,
        nameHint: String?,
        today: LocalDate,
    ): DeviceNoteDraft = DeviceNoteDraft()
}

private fun note(id: String) = Note(id, id, "2026.10.14", emptyList())

class DeviceNoteTest {
    @Test
    fun `LN-3 제목과 그룹은 한 줄 200자다`() {
        assertEquals("가 나 다 라", DeviceNoteFields.singleLine("가\n나\r\n다\r라"))
        assertEquals("a".repeat(200), DeviceNoteFields.singleLine("a".repeat(201)))
        assertEquals("😀".repeat(200), DeviceNoteFields.singleLine("😀".repeat(250)))
        assertEquals(200, DeviceNoteFields.singleLine("\n".repeat(500)).length)
        assertEquals("제목", DeviceNoteFields.title("  제목\n"))
        assertEquals("b".repeat(200), DeviceNoteFields.group("b".repeat(10_000), "기본"))
    }

    @Test
    fun `LN-3 제목 본문 공백은 저장할 수 없다`() {
        val rules = ByteRules(maxBody = 10)

        assertEquals(DraftCheck(true, false, false), DeviceNoteDraft(" \n", "", "본문").check(rules))
        assertEquals(DraftCheck(false, true, false), DeviceNoteDraft("제목", "", "\t \n").check(rules))
        assertEquals(DraftCheck(false, false, true), DeviceNoteDraft("제목", "", "a".repeat(11)).check(rules))
        assertTrue(DeviceNoteDraft("제목", "", "a".repeat(10)).check(rules).canSave)
        assertFalse(DeviceNoteDraft().check(rules).canSave)
    }

    @Test
    fun `LN-4 그룹 칸을 비우면 기본 그룹이다`() {
        assertEquals("기본", DeviceNoteFields.group("", "기본"))
        assertEquals("기본", DeviceNoteFields.group(" \n ", "기본"))
    }

    @Test
    fun `LN-4 그룹 이름은 공백만 다듬고 대소문자를 가린다`() {
        assertEquals("kotlin", DeviceNoteFields.group(" kotlin ", "기본"))
        assertEquals("Kotlin", DeviceNoteFields.group("Kotlin", "기본"))
        assertEquals("안드로이드 앱", DeviceNoteFields.group("안드로이드\n앱", "기본"))
    }

    @Test
    fun `LN-4 날짜는 저장소 노트와 같은 모양이다`() {
        assertEquals("2026.10.04", NoteDates.format(LocalDate.of(2026, 10, 4)))
        assertEquals(LocalDate.of(2026, 10, 4), NoteDates.parse("2026.10.04"))
        assertNull(NoteDates.parse("2026-10-04"))
        assertNull(NoteDates.parse("2026.02.30"))
        assertNull(NoteDates.parse("2026.1.4"))
    }

    @Test
    fun `LN-5 출처 표시는 기기 노트에만 있다`() {
        assertEquals(NoteOrigin.Device, note("local/abc").origin)
        assertEquals(NoteOrigin.Repository, note("s0/graph").origin)
        assertEquals(NoteOrigin.Repository, note("2f1c/android/flow").origin)
        assertEquals(NoteOrigin.Repository, note("localx/abc").origin)
        assertTrue(NoteOrigin.Device.editable)
        assertFalse(NoteOrigin.Repository.editable)
    }

    @Test
    fun `RC-1 띠가 세는 노트는 연결 정보 없는 저장소 노트뿐이다`() {
        val categories =
            listOf(
                Category("a", listOf(note("s0/x"), note("local/1"))),
                Category("b", listOf(note("local/2"), note("s0/y"))),
            )
        val deviceOnly = listOf(Category("a", listOf(note("local/1"))))

        assertEquals(2, categories.connectBandNoteCount(ConnectionState.None))
        assertEquals(0, deviceOnly.connectBandNoteCount(ConnectionState.None))
        assertEquals(0, categories.connectBandNoteCount(null))
        assertEquals(
            0,
            categories.connectBandNoteCount(
                ConnectionState.Connected(SourceId.Legacy, RepoConnection("o", "r", "", "notes/index.json", true)),
            ),
        )
    }

    @Test
    fun `LN-7 받는 파일은 md 와 markdown 이고 이름 후보는 확장자를 뺀다`() {
        assertTrue(ImportFileTypes.accepts("flow.md"))
        assertTrue(ImportFileTypes.accepts("FLOW.MarkDown"))
        assertFalse(ImportFileTypes.accepts("flow.txt"))
        assertFalse(ImportFileTypes.accepts("flow"))
        assertFalse(ImportFileTypes.accepts(null))
        assertEquals("flow", ImportFileTypes.stem("flow.md"))
        assertEquals("a.b", ImportFileTypes.stem("a.b.Markdown"))
    }
}
