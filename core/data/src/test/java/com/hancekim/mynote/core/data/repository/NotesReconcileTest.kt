package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.datasource.CachedNote
import com.hancekim.mynote.core.data.datasource.CachedNoteState
import com.hancekim.mynote.core.data.datasource.LocalNotesDataSource
import com.hancekim.mynote.core.data.datasource.NoteSourceAdapter
import com.hancekim.mynote.core.data.datasource.SourceEntry
import com.hancekim.mynote.core.data.datasource.SourceRecord
import com.hancekim.mynote.core.data.dto.NotesJson
import com.hancekim.mynote.core.data.format.PARSER_VERSION
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SourceKind
import com.hancekim.mynote.core.domain.model.SpanStyle
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections

class NotesReconcileTest {
    private var clock = 1_000L

    private fun repo(
        adapter: NoteSourceAdapter,
        local: LocalNotesDataSource,
    ) = NotesRepositoryImpl(setOf(adapter), local, NotesJson, {}) { clock++ }

    private suspend fun NotesRepositoryImpl.blocksOf(key: String): List<NoteBlock> =
        observeCategories()
            .first()
            .flatMap { it.notes }
            .first { it.id == key }
            .blocks

    @Test
    fun `로컬에 없으면 본문까지 받아 upsert 한다`() =
        runTest {
            val adapter = FakeAdapter(listOf(entry("a", "h1"), entry("b", "h1")))
            val local = FakeLocal()

            val result = repo(adapter, local).sync()

            assertEquals(2, result.getOrThrow().added)
            assertEquals(setOf("s0/a", "s0/b"), local.store.value.keys)
            assertEquals(setOf("a", "b"), adapter.fetched.toSet())
        }

    @Test
    fun `리비전이 같으면 본문을 다시 받지 않는다`() =
        runTest {
            val adapter = FakeAdapter(listOf(entry("a", "h1")))
            val local = FakeLocal(listOf(cached("a", "h1")))

            val outcome = repo(adapter, local).sync().getOrThrow()

            assertTrue(adapter.fetched.isEmpty())
            assertFalse(outcome.hasChanges)
            assertEquals(0, local.applyCalls)
        }

    @Test
    fun `리비전이 다르면 본문을 다시 받아 갱신한다`() =
        runTest {
            val adapter = FakeAdapter(listOf(entry("a", "NEW")))
            val local = FakeLocal(listOf(cached("a", "OLD")))

            val outcome = repo(adapter, local).sync().getOrThrow()

            assertEquals(listOf("a"), adapter.fetched)
            assertEquals(1, outcome.updated)
            assertEquals(
                "NEW",
                local.store.value
                    .getValue("s0/a")
                    .revision,
            )
        }

    @Test
    fun `index 에 없는 로컬 노트는 삭제한다`() =
        runTest {
            val adapter = FakeAdapter(listOf(entry("a", "h1")))
            val local = FakeLocal(listOf(cached("stale", "h1")))

            val outcome = repo(adapter, local).sync().getOrThrow()

            assertEquals(setOf("s0/a"), local.store.value.keys)
            assertEquals(listOf("s0/stale"), outcome.deleted)
        }

    @Test
    fun `모르는 span style 이 있어도 반영한다`() =
        runTest {
            val italic = """{"blocks":[{"type":"paragraph","spans":[{"text":"x","style":"italic"}]}]}"""
            val adapter = FakeAdapter(listOf(entry("a", "h1")), docs = mapOf("a" to italic))
            val local = FakeLocal()
            val repo = repo(adapter, local)

            assertTrue(repo.sync().isSuccess)
            val paragraph = repo.blocksOf("s0/a").single() as NoteBlock.Paragraph
            assertEquals("x", paragraph.spans.single().text)
            assertEquals(SpanStyle.Text, paragraph.spans.single().style)
        }

    @Test
    fun `형식 오류 노트는 격리하고 나머지는 반영한다`() =
        runTest {
            val adapter =
                FakeAdapter(
                    listOf(entry("ok", "h1"), entry("broken", "h1"), entry("bare", "h1")),
                    docs =
                        mapOf(
                            "broken" to """{"blocks":[{"type":"heading","spans":[]},{"type":"code","code":"c"}]}""",
                            "bare" to """{"blocks":[{"type":"code","code":"envelope 없음"}]}""",
                        ),
                )
            val local = FakeLocal()
            val repo = repo(adapter, local)

            val outcome = repo.sync().getOrThrow()

            assertEquals(3, outcome.added)
            val broken = repo.blocksOf("s0/broken")
            assertEquals(UnsupportedReason.BrokenBlock, (broken[0] as NoteBlock.Unsupported).reason)
            assertEquals(NoteBlock.Code("c"), broken[1])
            assertEquals(listOf(NoteBlock.Code("envelope 없음")), repo.blocksOf("s0/bare"))
        }

    @Test
    fun `JSON 이 아닌 노트도 목록에 남고 본문은 BrokenNote 다`() =
        runTest {
            val adapter = FakeAdapter(listOf(entry("a", "h1"), entry("b", "h1")), docs = mapOf("a" to "not json"))
            val repo = repo(adapter, FakeLocal())

            assertTrue(repo.sync().isSuccess)
            val block = repo.blocksOf("s0/a").single() as NoteBlock.Unsupported
            assertEquals(UnsupportedReason.BrokenNote, block.reason)
            assertEquals("not json", block.raw)
        }

    @Test
    fun `원문과 parserVersion 을 캐시에 남긴다`() =
        runTest {
            val raw = """{"blocks":[{"type":"timeline","events":[1]}]}"""
            val adapter = FakeAdapter(listOf(entry("a", "h1")), docs = mapOf("a" to raw))
            val local = FakeLocal()

            repo(adapter, local).sync()

            val row = local.store.value.getValue("s0/a")
            assertEquals(raw, row.raw)
            assertEquals(PARSER_VERSION, row.parserVersion)
            assertTrue(row.blocksJson.contains("timeline"))
        }

    @Test
    fun `본문 전송이 하나라도 실패하면 아무것도 반영하지 않는다`() =
        runTest {
            val adapter = FakeAdapter(listOf(entry("a", "h1"), entry("b", "h1")), failOn = "b")
            val local = FakeLocal(listOf(cached("stale", "h1")))

            val result = repo(adapter, local).sync()

            assertTrue(result.isFailure)
            assertEquals(setOf("s0/stale"), local.store.value.keys)
        }

    @Test
    fun `index 조회가 실패하면 Result 실패다`() =
        runTest {
            val adapter = FakeAdapter(emptyList(), listError = IllegalStateException("network"))

            val result = repo(adapter, FakeLocal()).sync()

            assertTrue(result.isFailure)
            assertTrue(adapter.fetched.isEmpty())
        }

    @Test
    fun `index 를 해석하지 못하면 아무것도 반영하지 않는다`() =
        runTest {
            val adapter = FakeAdapter(emptyList(), listError = kotlinx.serialization.SerializationException("index"))
            val local = FakeLocal(listOf(cached("a", "h1")))

            val result = repo(adapter, local).sync()

            assertTrue(result.isFailure)
            assertEquals(setOf("s0/a"), local.store.value.keys)
            assertEquals(0, local.applyCalls)
        }

    @Test
    fun `로컬 쓰기가 실패해도 예외를 던지지 않고 Result 실패다`() =
        runTest {
            val adapter = FakeAdapter(listOf(entry("a", "h1")))
            val local = FakeLocal(writeError = IllegalStateException("disk"))

            val result = repo(adapter, local).sync()

            assertTrue(result.isFailure)
        }

    @Test
    fun `변경된 노트를 동시에 받는다`() =
        runTest {
            val ids = listOf("a", "b", "c")
            val adapter = FakeAdapter(ids.map { entry(it, "h1") }, gate = ids.size)

            val result = repo(adapter, FakeLocal()).sync()

            assertTrue(result.isSuccess)
            assertEquals(ids.size, adapter.fetched.size)
        }

    @Test
    fun `다른 소스의 노트는 지우지 않는다`() =
        runTest {
            val adapter = FakeAdapter(listOf(entry("a", "h1")))
            val local =
                FakeLocal(
                    initial = listOf(cached("a", "h1"), cached("clip", "r1", source = LINK)),
                    sources = listOf(SourceRecord(LINK, SourceKind.Link, null)),
                )

            val outcome = repo(adapter, local).sync().getOrThrow()

            assertEquals(setOf("s0/a", "s1/clip"), local.store.value.keys)
            assertTrue(outcome.deleted.isEmpty())
        }

    @Test
    fun `어댑터가 없는 kind 의 소스는 동기화도 삭제도 하지 않는다`() =
        runTest {
            val adapter = FakeAdapter(listOf(entry("a", "h1")))
            val local =
                FakeLocal(
                    initial = listOf(cached("clip", "r1", source = LINK)),
                    sources = listOf(SourceRecord(LINK, SourceKind.Link, null)),
                )

            repo(adapter, local).sync().getOrThrow()

            assertEquals(1, adapter.lists)
            assertTrue("s1/clip" in local.store.value.keys)
        }

    @Test
    fun `소스 index 가 비었는데 그 소스 노트가 남아 있으면 지우지 않고 실패한다`() =
        runTest {
            val local = FakeLocal(listOf(cached("a", "h1")))

            val result = repo(FakeAdapter(emptyList()), local).sync()

            assertTrue(result.isFailure)
            assertEquals(setOf("s0/a"), local.store.value.keys)
        }

    @Test
    fun `소스 index 가 비어도 다른 소스 노트만 있으면 성공이다`() =
        runTest {
            val local =
                FakeLocal(
                    initial = listOf(cached("clip", "r1", source = LINK)),
                    sources = listOf(SourceRecord(LINK, SourceKind.Link, null)),
                )

            val result = repo(FakeAdapter(emptyList()), local).sync()

            assertTrue(result.isSuccess)
            assertEquals(setOf("s1/clip"), local.store.value.keys)
        }

    @Test
    fun `원격 index 가 비었고 로컬도 비었으면 성공이다`() =
        runTest {
            val result = repo(FakeAdapter(emptyList()), FakeLocal()).sync()

            assertTrue(result.isSuccess)
        }

    @Test
    fun `parserVersion 이 낮은 행은 네트워크 없이 원문에서 다시 만든다`() =
        runTest {
            val raw = """{"blocks":[{"type":"timeline","events":[1]}]}"""
            val stale = cached("a", "h1", raw = raw, parserVersion = 0, blocksJson = """[{"type":"unknown"}]""")
            val adapter = FakeAdapter(emptyList(), listError = IllegalStateException("offline"))
            val local = FakeLocal(listOf(stale))
            val repo = repo(adapter, local)

            assertTrue(repo.sync().isFailure)

            assertTrue(adapter.fetched.isEmpty())
            val row = local.store.value.getValue("s0/a")
            assertEquals(PARSER_VERSION, row.parserVersion)
            val block = repo.blocksOf("s0/a").single() as NoteBlock.Unsupported
            assertEquals("timeline", block.type)
        }

    @Test
    fun `원문이 없는 옛 행은 리비전이 같아도 한 번 다시 받고 갱신으로 세지 않는다`() =
        runTest {
            val adapter = FakeAdapter(listOf(entry("a", "h1")))
            val local = FakeLocal(listOf(cached("a", "h1", raw = null, parserVersion = 0)))
            val repo = repo(adapter, local)

            val first = repo.sync().getOrThrow()
            val second = repo.sync().getOrThrow()

            assertEquals(listOf("a"), adapter.fetched)
            assertFalse(first.hasChanges)
            assertFalse(second.hasChanges)
            assertEquals(
                body("a"),
                local.store.value
                    .getValue("s0/a")
                    .raw,
            )
        }

    @Test
    fun `노트 키에 소스 접두를 단다`() =
        runTest {
            val local = FakeLocal()
            val repo = repo(FakeAdapter(listOf(entry("graph", "h1"))), local)

            repo.sync()

            val row = local.store.value.getValue("s0/graph")
            assertEquals("s0", row.sourceId)
            assertEquals("graph", row.docId)
            assertEquals(
                "s0/graph",
                repo
                    .observeCategories()
                    .first()
                    .single()
                    .notes
                    .single()
                    .id,
            )
        }

    @Test
    fun `처음 본 시각은 갱신해도 바뀌지 않는다`() =
        runTest {
            val local = FakeLocal(listOf(cached("a", "OLD", firstSeenAt = 7L)))

            repo(FakeAdapter(listOf(entry("a", "NEW"))), local).sync()

            assertEquals(
                7L,
                local.store.value
                    .getValue("s0/a")
                    .firstSeenAt,
            )
        }

    @Test
    fun `같은 동기화에서 처음 본 노트는 처음 본 시각이 같다`() =
        runTest {
            val local = FakeLocal()

            repo(FakeAdapter(listOf(entry("a", "h1"), entry("b", "h1"), entry("c", "h1"))), local).sync()

            assertEquals(
                1,
                local.store.value.values
                    .map { it.firstSeenAt }
                    .toSet()
                    .size,
            )
        }

    @Test
    fun `index 날짜를 epochDay 로 저장하고 못 읽으면 null 이다`() =
        runTest {
            val local = FakeLocal()
            val adapter =
                FakeAdapter(
                    listOf(
                        entry("dot", "h1", date = "2026.06.29"),
                        entry("dash", "h1", date = "2026-06-29"),
                        entry("bad", "h1", date = "어제"),
                    ),
                )

            repo(adapter, local).sync()

            assertEquals(
                20633L,
                local.store.value
                    .getValue("s0/dot")
                    .epochDay,
            )
            assertEquals(
                20633L,
                local.store.value
                    .getValue("s0/dash")
                    .epochDay,
            )
            assertNull(
                local.store.value
                    .getValue("s0/bad")
                    .epochDay,
            )
            assertEquals(
                "2026.06.29",
                local.store.value
                    .getValue("s0/dot")
                    .date,
            )
        }

    @Test
    fun `excerpt 를 첫 문장으로 채운다`() =
        runTest {
            val raw = """{"blocks":[{"type":"paragraph","spans":[{"text":"첫 문장입니다. 둘째 문장."}]}]}"""
            val local = FakeLocal()

            repo(FakeAdapter(listOf(entry("a", "h1")), docs = mapOf("a" to raw)), local).sync()

            assertEquals(
                "첫 문장입니다.",
                local.store.value
                    .getValue("s0/a")
                    .excerpt,
            )
        }

    @Test
    fun `blocks_json 이 깨지면 원문을 다시 읽고 원문도 없으면 BrokenNote 로 남긴다`() =
        runTest {
            val local =
                FakeLocal(
                    listOf(
                        cached("withRaw", "h1", blocksJson = "{깨짐"),
                        cached("noRaw", "h1", raw = null, blocksJson = "{깨짐"),
                    ),
                )
            val repo = repo(FakeAdapter(emptyList()), local)

            assertEquals(1, repo.blocksOf("s0/withRaw").size)
            assertTrue(repo.blocksOf("s0/withRaw").single() is NoteBlock.Paragraph)
            val broken = repo.blocksOf("s0/noRaw").single() as NoteBlock.Unsupported
            assertEquals(UnsupportedReason.BrokenNote, broken.reason)
            assertEquals("{깨짐", broken.raw)
        }
}
