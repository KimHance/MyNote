package com.hancekim.mynote.core.data.datasource

import com.hancekim.mynote.core.data.repository.FakeLocal
import com.hancekim.mynote.core.domain.ImportNoteUseCase
import com.hancekim.mynote.core.domain.model.DeviceNoteDraft
import com.hancekim.mynote.core.domain.model.FileText
import com.hancekim.mynote.core.domain.model.ImportOutcome
import com.hancekim.mynote.core.domain.model.ImportRejection
import com.hancekim.mynote.core.domain.repository.NoteFileReader
import com.hancekim.mynote.core.format.DeviceNoteMarkdown
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.LocalDate

private val TODAY: LocalDate = LocalDate.of(2026, 10, 14)

/** 주소 → (표시 이름, 바이트). 없는 주소는 읽지 못한다. */
private class BytesReader(
    private val files: Map<String, Pair<String?, ByteArray>>,
) : NoteFileReader {
    val reads = mutableListOf<Int>()

    override suspend fun displayName(uri: String): String? = (files[uri] ?: throw FileNotFoundException(uri)).first

    override suspend fun read(
        uri: String,
        maxBytes: Int,
    ): FileText {
        reads += maxBytes
        val bytes = (files[uri] ?: throw FileNotFoundException(uri)).second
        return readStrictUtf8(ByteArrayInputStream(bytes), maxBytes)
    }
}

/** 읽은 바이트 수를 센다 — 멈추는 자리를 본다. */
private class CountingStream(
    private val size: Int,
) : InputStream() {
    var served = 0

    override fun read(): Int = if (served < size) 'a'.code.also { served++ } else -1

    override fun read(
        b: ByteArray,
        off: Int,
        len: Int,
    ): Int {
        if (served >= size) return -1
        val n = minOf(len, size - served)
        b.fill('a'.code.toByte(), off, off + n)
        served += n
        return n
    }
}

private fun importer(
    reader: NoteFileReader =
        BytesReader(
            emptyMap(),
        ),
) = ImportNoteUseCase(DeviceNoteMarkdown(), reader) {
    TODAY
}

class NoteImportTest {
    @Test
    fun `LN-6 세 길이 같은 규칙으로 편집기를 채운다`() =
        runTest {
            val text = "---\r\ntitle: \"Flow: 기초\"\r\ndate: 2026.06.12\r\n---\r\n\r\n# 본문 제목\r\n문단\r\n"
            val reader = BytesReader(mapOf("content://flow" to ("flow.md" to text.toByteArray())))
            val importer = importer(reader)

            val shared = importer.text(text, subject = "공유 제목")
            val pasted = importer.text(text)
            val file = importer.file("content://flow")

            val expected = DeviceNoteDraft("Flow: 기초", "", "# 본문 제목\n문단\n", LocalDate.of(2026, 6, 12))
            assertEquals(ImportOutcome.Ready(expected), shared)
            assertEquals(ImportOutcome.Ready(expected), pasted)
            assertEquals(ImportOutcome.Ready(expected), file)
        }

    @Test
    fun `LN-6 이름 후보는 파일 이름에서 확장자를 빼고 공유 제목은 그대로다`() =
        runTest {
            val reader =
                BytesReader(
                    mapOf(
                        "content://a" to ("flow.md" to "본문".toByteArray()),
                        "content://b" to ("Guide.MARKDOWN" to "본문".toByteArray()),
                    ),
                )
            val importer = importer(reader)

            assertEquals("flow", (importer.file("content://a") as ImportOutcome.Ready).draft.title)
            assertEquals("Guide", (importer.file("content://b") as ImportOutcome.Ready).draft.title)
            assertEquals("공유.md", (importer.text("본문", "공유.md") as ImportOutcome.Ready).draft.title)
            assertEquals("", (importer.text("본문") as ImportOutcome.Ready).draft.title)
        }

    @Test
    fun `LN-7 가져오기 거절 이유 5종이 다르다`() =
        runTest {
            val reader =
                BytesReader(
                    mapOf(
                        "content://empty" to ("empty.md" to " \n\t".toByteArray()),
                        "content://latin" to ("latin.md" to byteArrayOf(0x63, 0x61, 0x66, 0xE9.toByte())),
                        "content://big" to ("big.md" to ByteArray(524_289) { 'a'.code.toByte() }),
                        "content://txt" to ("note.txt" to "본문".toByteArray()),
                        "content://noname" to (null to "본문".toByteArray()),
                    ),
                )
            val importer = importer(reader)

            val outcomes =
                listOf(
                    importer.file("content://empty"),
                    importer.file("content://latin"),
                    importer.file("content://big"),
                    importer.file("content://gone"),
                    importer.file("content://txt"),
                )

            assertEquals(
                ImportRejection.entries.map { ImportOutcome.Rejected(it) },
                outcomes,
            )
            assertEquals(ImportOutcome.Rejected(ImportRejection.UnsupportedType), importer.file("content://noname"))
            assertEquals(ImportOutcome.Rejected(ImportRejection.Empty), importer.text("  \n"))
            assertEquals(ImportOutcome.Rejected(ImportRejection.TooLarge), importer.text("가".repeat(174_763)))
        }

    @Test
    fun `LN-7 크기 경계는 512KB 다`() =
        runTest {
            val reader =
                BytesReader(
                    mapOf(
                        "content://edge" to ("edge.md" to ByteArray(524_288) { 'a'.code.toByte() }),
                        "content://over" to ("over.md" to ByteArray(524_289) { 'a'.code.toByte() }),
                    ),
                )
            val importer = importer(reader)

            assertTrue(importer.file("content://edge") is ImportOutcome.Ready)
            assertEquals(ImportOutcome.Rejected(ImportRejection.TooLarge), importer.file("content://over"))
            assertTrue(importer.text("a".repeat(524_288)) is ImportOutcome.Ready)
            assertEquals(ImportOutcome.Rejected(ImportRejection.TooLarge), importer.text("a".repeat(524_289)))
            assertEquals(listOf(524_288, 524_288), reader.reads)
        }

    @Test
    fun `LN-7 파일은 524,289 바이트에서 멈춘다`() {
        val stream = CountingStream(10_000_000)

        assertEquals(FileText.TooLarge, readStrictUtf8(stream, 524_288))
        assertEquals(524_289, stream.served)
    }

    @Test
    fun `LN-7 UTF-8 이 아니면 대체 문자 없이 가른다`() {
        val utf16 = "﻿본문".toByteArray(Charsets.UTF_16LE)
        val truncated = "가".toByteArray().copyOf(2)

        assertEquals(FileText.NotUtf8, readStrictUtf8(ByteArrayInputStream(utf16), 524_288))
        assertEquals(FileText.NotUtf8, readStrictUtf8(ByteArrayInputStream(truncated), 524_288))
        assertEquals(FileText.Read("﻿가"), readStrictUtf8(ByteArrayInputStream("﻿가".toByteArray()), 524_288))
    }

    @Test
    fun `LN-6 저장하기 전에는 노트가 생기지 않는다`() =
        runTest {
            val local = FakeLocal(connected = false)
            val reader = BytesReader(mapOf("content://a" to ("a.md" to "# 제목\n본문".toByteArray())))
            val importer = importer(reader)

            importer.text("# 제목\n본문", "공유")
            importer.text("붙여 넣은 글")
            importer.file("content://a")

            assertTrue(local.store.value.isEmpty())
            assertTrue(local.sourceRows.isEmpty())
        }

    @Test
    fun `LN-7 고르지 않고 돌아오면 아무 일도 없다`() =
        runTest {
            val reader = BytesReader(emptyMap())

            assertEquals(ImportOutcome.Dismissed, importer(reader).file(null))
            assertTrue(reader.reads.isEmpty())
        }
}
