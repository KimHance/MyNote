package com.hancekim.mynote.core.data.format

import com.hancekim.mynote.core.data.dto.NoteBlockDto
import com.hancekim.mynote.core.data.dto.NotesJson
import com.hancekim.mynote.core.data.dto.toDomain
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.firstSentence
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

private val ALL_BLOCKS =
    """
    {"id":"n","title":"t","date":"2026.01.01","blocks":[
      {"type":"heading","level":2,"spans":[{"text":"제목"}]},
      {"type":"paragraph","spans":[{"text":"본문 "},{"text":"굵게","style":"bold"},{"text":" 와 ","style":"text"},{"text":"code","style":"code"},{"text":". 둘째."}]},
      {"type":"code","code":"val x = 1"},
      {"type":"quote","spans":[{"text":"인용"}]},
      {"type":"bulletList","items":[{"spans":[{"text":"a"}]},{"spans":[{"text":"b","style":"bold"}]}]},
      {"type":"orderedList","items":[{"spans":[{"text":"1"}]}]},
      {"type":"divider"},
      {"type":"spacer"}
    ]}
    """.trimIndent()

class ReparseEquivalenceTest {
    private val format = MyNoteJsonFormat(NotesJson)

    private fun oldCachePath(raw: String): Note {
        val blocks =
            NotesJson.decodeFromJsonElement(
                ListSerializer(NoteBlockDto.serializer()),
                NotesJson.parseToJsonElement(raw).jsonObject.getValue("blocks"),
            )
        val cached = NotesJson.encodeToString(blocks)
        return Note("k", "t", "d", format.decode(cached).map { it.toDomain() })
    }

    private fun newPath(raw: String): Note =
        Note(
            "k",
            "t",
            "d",
            format.decode(format.derive(raw).blocksJson).map {
                it.toDomain()
            },
        )

    private fun assertSame(
        name: String,
        raw: String,
    ) {
        val old = oldCachePath(raw)
        val new = newPath(raw)
        assertEquals(name, old, new)
        assertEquals(name, old.firstSentence(), new.firstSentence())
        assertEquals(name, old.firstSentence(), format.derive(raw).excerpt)
    }

    @Test
    fun `원문 재변환 결과가 옛 캐시 경로와 같다`() {
        assertSame("all-blocks", ALL_BLOCKS)
    }

    @Test
    fun `레포의 실제 노트는 옛 캐시 경로와 새 경로가 같다`() {
        val notes =
            generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
                .map { File(it, "notes") }
                .firstOrNull { File(it, "index.json").isFile }
        assumeTrue(notes != null)
        val files =
            notes!!
                .walkTopDown()
                .filter { it.isFile && it.extension == "json" && it.name != "index.json" }
                .toList()
        assumeTrue(files.isNotEmpty())

        files.forEach { assertSame(it.name, it.readText()) }
    }
}
