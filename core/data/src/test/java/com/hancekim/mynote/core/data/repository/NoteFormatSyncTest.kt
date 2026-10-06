package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.datasource.ConnectionIdGenerator
import com.hancekim.mynote.core.data.datasource.SourceEntry
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import com.hancekim.mynote.core.format.MarkdownConverter
import com.hancekim.mynote.core.format.NoteFormat
import com.hancekim.mynote.core.format.PARSER_VERSION
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections

private const val FAIL = "실패하는 본문"

private class FakeMarkdown : MarkdownConverter {
    val seen: MutableList<String> = Collections.synchronizedList(mutableListOf())

    override fun convert(raw: String): List<NoteBlock> {
        seen += raw
        check(raw != FAIL) { "변환 실패" }
        return listOf(NoteBlock.Paragraph(listOf(NoteSpan("md:$raw"))))
    }
}

private fun md(text: String) = listOf(NoteBlock.Paragraph(listOf(NoteSpan("md:$text"))))

private fun mdEntry(
    id: String,
    revision: String = "h1",
) = SourceEntry(id, "notes/android/$id.md", revision, "android", id, "2026.01.01")

private fun brokenNote(raw: String) =
    listOf(NoteBlock.Unsupported(UnsupportedReason.BrokenNote, type = null, text = null, raw = raw))

private fun limitNote(raw: String) =
    listOf(NoteBlock.Unsupported(UnsupportedReason.LimitExceeded, type = null, text = null, raw = raw))

private val DEEP_LIST = "- ".repeat(1_000) + "x"

class NoteFormatSyncTest {
    private val markdown = FakeMarkdown()
    private val format = NoteFormat(markdown)

    private fun repo(
        adapter: FakeAdapter,
        local: FakeLocal,
    ) = NotesRepositoryImpl(setOf(adapter), local, format, {}) { 1L }

    private suspend fun NotesRepositoryImpl.blocks(): Map<String, List<NoteBlock>> =
        observeCategories().first().flatMap { it.notes }.associate { it.id to it.blocks }

    @Test
    fun `NF-2 연결 흐름의 첫 동기화도 경로로 고른다`() =
        runTest {
            val local = FakeLocal(connected = false)
            val tokens = FakeTokenStore()
            val bookmarks = FakeBookmarkStore()
            val state = FakeStateStore()
            val adapter = FakeAdapter(listOf(mdEntry("a"), entry("b", "h1")), docs = mapOf("a" to "# a"))
            val connections =
                ConnectionRepositoryImpl(
                    setOf(adapter),
                    local,
                    tokens,
                    bookmarks,
                    state,
                    ConnectionIdGenerator { "c1" },
                    format,
                    ConnectionRecovery(local, tokens, bookmarks, state),
                    FakeAppliedToken(),
                ) { 1L }

            val pending = connections.begin(RepoConnection("o", "r", "main", "notes/index.json", isPublic = true), null)
            connections.syncPending(pending).getOrThrow()
            connections.confirm(pending)

            val blocks = repo(adapter, local).blocks()
            assertEquals(md("# a"), blocks.getValue("c1/a"))
            assertEquals(listOf(NoteBlock.Paragraph(listOf(NoteSpan("b")))), blocks.getValue("c1/b"))
            assertEquals(listOf("# a"), markdown.seen)
        }

    @Test
    fun `NF-2 캐시가 깨진 마크다운 행은 원문을 마크다운으로 다시 읽는다`() =
        runTest {
            val local =
                FakeLocal(
                    listOf(
                        cached("a", "h1", raw = "# a", blocksJson = "{깨짐").copy(path = "notes/android/a.md"),
                        cached("b", "h1", blocksJson = "{깨짐").copy(path = "notes/android/b.json"),
                    ),
                )

            val blocks = repo(FakeAdapter(emptyList()), local).blocks()

            assertEquals(md("# a"), blocks.getValue("s0/a"))
            assertEquals(listOf(NoteBlock.Paragraph(listOf(NoteSpan("b")))), blocks.getValue("s0/b"))
        }

    @Test
    fun `NF-8 1,000단 목록 노트도 동기화와 캐시 디코드를 지나 한도 넘음 상자가 된다`() =
        runTest {
            val adapter =
                FakeAdapter(
                    listOf(mdEntry("deep"), mdEntry("a"), entry("b", "h1")),
                    docs = mapOf("deep" to DEEP_LIST, "a" to "a"),
                )
            val local = FakeLocal()
            val repo = repo(adapter, local)

            val outcome = repo.sync().getOrThrow()

            assertEquals(3, outcome.added)
            val deep = local.store.value.getValue("s0/deep")
            assertEquals(DEEP_LIST, deep.raw)
            assertTrue(deep.blocksJson.contains("\"reason\":\"limit_exceeded\""))
            val blocks = repo.blocks()
            assertEquals(limitNote(DEEP_LIST), blocks.getValue("s0/deep"))
            assertEquals(md("a"), blocks.getValue("s0/a"))
            assertEquals(listOf(NoteBlock.Paragraph(listOf(NoteSpan("b")))), blocks.getValue("s0/b"))
            assertEquals(listOf("a"), markdown.seen)
        }

    @Test
    fun `NF-8 목록은 원문을 읽지 않고 노트 전체 상자만 원문을 따로 읽는다`() =
        runTest {
            val adapter =
                FakeAdapter(
                    listOf(mdEntry("deep"), mdEntry("bad"), mdEntry("a"), entry("b", "h1")),
                    docs = mapOf("deep" to DEEP_LIST, "bad" to FAIL, "a" to "a"),
                )
            val local = FakeLocal()
            val repo = repo(adapter, local)
            repo.sync().getOrThrow()

            val blocks = repo.blocks()

            assertEquals(limitNote(DEEP_LIST), blocks.getValue("s0/deep"))
            assertEquals(brokenNote(FAIL), blocks.getValue("s0/bad"))
            assertEquals(setOf("s0/deep", "s0/bad"), local.rawReads.toSet())
            assertEquals(
                """[{"type":"unsupported","reason":"limit_exceeded","raw":""}]""",
                local.store.value
                    .getValue("s0/deep")
                    .blocksJson,
            )
        }

    @Test
    fun `NF-8 해석 실패는 그 노트만 상자다`() =
        runTest {
            val adapter =
                FakeAdapter(
                    listOf(mdEntry("bad"), mdEntry("a"), entry("b", "h1")),
                    docs = mapOf("bad" to FAIL, "a" to "a"),
                )
            val repo = repo(adapter, FakeLocal())

            assertEquals(3, repo.sync().getOrThrow().added)

            val blocks = repo.blocks()
            assertEquals(brokenNote(FAIL), blocks.getValue("s0/bad"))
            assertEquals(md("a"), blocks.getValue("s0/a"))
            assertEquals(listOf(NoteBlock.Paragraph(listOf(NoteSpan("b")))), blocks.getValue("s0/b"))
        }

    @Test
    fun `NF-9 JSON 행은 재변환 뒤에도 같다`() =
        runTest {
            val cache = """[{"type":"paragraph","spans":[{"text":"a"}]}]"""
            val local =
                FakeLocal(
                    listOf(
                        cached("a", "h1", parserVersion = 0, blocksJson = cache).copy(path = "notes/android/a.json"),
                        cached("b", "h1", raw = body("a"), parserVersion = 0, blocksJson = cache),
                    ),
                )
            val adapter = FakeAdapter(emptyList(), listError = IllegalStateException("offline"))
            val repo = repo(adapter, local)
            val before = repo.blocks()

            assertTrue(repo.sync().isFailure)

            listOf("s0/a", "s0/b").forEach { key ->
                val row = local.store.value.getValue(key)
                assertEquals(key, PARSER_VERSION, row.parserVersion)
                assertEquals(key, cache, row.blocksJson)
            }
            assertEquals(before, repo.blocks())
            assertTrue(markdown.seen.isEmpty())
            assertTrue(adapter.fetched.isEmpty())
        }

    @Test
    fun `NF-9 재변환 중 해석이 실패한 행은 상자가 되고 동기화는 성공한다`() =
        runTest {
            val stale =
                listOf("bad" to FAIL, "deep" to DEEP_LIST, "ok" to "ok").map { (id, raw) ->
                    cached(
                        id,
                        "h1",
                        raw = raw,
                        parserVersion = 0,
                        blocksJson = "[]",
                    ).copy(path = "notes/android/$id.md")
                }
            val adapter =
                FakeAdapter(listOf(mdEntry("bad"), mdEntry("deep"), mdEntry("ok"), entry("new", "h1")))
            val local = FakeLocal(stale)
            val repo = repo(adapter, local)

            val outcome = repo.sync().getOrThrow()

            assertEquals(1, outcome.added)
            assertEquals(0, outcome.updated)
            assertTrue(outcome.deleted.isEmpty())
            assertEquals(listOf("new"), adapter.fetched)
            listOf("bad", "deep", "ok").forEach { id ->
                assertEquals(
                    id,
                    PARSER_VERSION,
                    local.store.value
                        .getValue("s0/$id")
                        .parserVersion,
                )
            }
            val blocks = repo.blocks()
            assertEquals(brokenNote(FAIL), blocks.getValue("s0/bad"))
            assertEquals(limitNote(DEEP_LIST), blocks.getValue("s0/deep"))
            assertEquals(md("ok"), blocks.getValue("s0/ok"))
            assertEquals(listOf(NoteBlock.Paragraph(listOf(NoteSpan("new")))), blocks.getValue("s0/new"))
        }
}
