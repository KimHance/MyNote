package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.data.datasource.SourceEntry
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import com.hancekim.mynote.core.format.NoteFormat
import com.hancekim.mynote.core.format.PARSER_VERSION
import com.hancekim.mynote.core.format.ParityNotes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

private const val TIME_LIMIT_MILLIS = 10_000L

private fun mdEntry(
    id: String,
    revision: String = "h1",
) = SourceEntry(id, "notes/android/$id.md", revision, "android", id, "2026.01.01")

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
        "%02x".format(it)
    }

private fun List<NoteBlock>.hasBox(): Boolean =
    any { block ->
        when (block) {
            is NoteBlock.Unsupported -> true
            is NoteBlock.BulletList -> block.items.any { it.children.hasBox() }
            is NoteBlock.OrderedList -> block.items.any { it.children.hasBox() }
            else -> false
        }
    }

class MarkdownSyncTest {
    private val format = NoteFormat()

    private fun repo(
        adapter: FakeAdapter,
        local: FakeLocal,
    ) = NotesRepositoryImpl(setOf(adapter), local, format, {}) { 1L }

    private suspend fun NotesRepositoryImpl.blocks(): Map<String, List<NoteBlock>> =
        observeCategories().first().flatMap { it.notes }.associate { it.id to it.blocks }

    @Test
    fun `NF-8 한도 안의 큰 입력도 동기화를 멈추지 않는다`() =
        runTest {
            val deepList = (0 until 1_000).joinToString("\n") { "  ".repeat(it) + "- x" }
            val docs =
                mapOf(
                    "brackets" to "[".repeat(5_000),
                    "emphasis" to "*".repeat(20_000) + "a" + "*".repeat(20_000),
                    "deep" to deepList,
                    "a" to "# 제목\n\n- 항목",
                )
            val adapter = FakeAdapter(docs.keys.map { mdEntry(it) } + entry("b", "h1"), docs = docs)
            val local = FakeLocal()
            val repo = repo(adapter, local)

            val started = System.nanoTime()
            val outcome = repo.sync().getOrThrow()
            val elapsed = (System.nanoTime() - started) / 1_000_000

            assertTrue("$elapsed ms", elapsed < TIME_LIMIT_MILLIS)
            assertEquals(5, outcome.added)
            val blocks = repo.blocks()
            assertTrue(blocks.getValue("s0/brackets").isNotEmpty())
            assertTrue(blocks.getValue("s0/emphasis").isNotEmpty())
            listOf(
                "s0/brackets",
                "s0/emphasis",
                "s0/a",
                "s0/b",
            ).forEach { assertTrue(it, !blocks.getValue(it).hasBox()) }
            assertEquals(
                listOf(
                    NoteBlock.Unsupported(UnsupportedReason.LimitExceeded, type = null, text = null, raw = deepList),
                ),
                blocks.getValue("s0/deep"),
            )
            assertEquals(
                listOf(
                    NoteBlock.Heading(1, listOf(NoteSpan("제목"))),
                    NoteBlock.BulletList(listOf(NoteListItem(listOf(NoteSpan("항목"))))),
                ),
                blocks.getValue("s0/a"),
            )
        }

    @Test
    fun `NF-9 옛 판이 상자로 둔 마크다운 행을 원문에서 다시 만든다`() =
        runTest {
            val raw = "---\ntitle: a\ndate: 2026.01.01\n---\n# 제목\n\n- 항목"
            val oldBox = """[{"type":"unsupported","reason":"broken_note","raw":"x"}]"""
            val stale =
                cached(
                    "a",
                    "h1",
                    raw = raw,
                    parserVersion = 1,
                    blocksJson = oldBox,
                ).copy(path = "notes/android/a.md")
            val expected =
                listOf(
                    NoteBlock.Heading(1, listOf(NoteSpan("제목"))),
                    NoteBlock.BulletList(listOf(NoteListItem(listOf(NoteSpan("항목"))))),
                )

            val offline = FakeAdapter(emptyList(), listError = IllegalStateException("offline"))
            val offlineLocal = FakeLocal(listOf(stale))
            val offlineRepo = repo(offline, offlineLocal)

            assertTrue(offlineRepo.sync().isFailure)
            assertEquals(
                PARSER_VERSION,
                offlineLocal.store.value
                    .getValue("s0/a")
                    .parserVersion,
            )
            assertEquals(expected, offlineRepo.blocks().getValue("s0/a"))
            assertTrue(offline.fetched.isEmpty())

            val online = FakeAdapter(listOf(mdEntry("a")))
            val onlineRepo = repo(online, FakeLocal(listOf(stale)))
            val outcome = onlineRepo.sync().getOrThrow()

            assertEquals(0, outcome.changed)
            assertTrue(online.fetched.isEmpty())
            assertEquals(expected, onlineRepo.blocks().getValue("s0/a"))
        }

    @Test
    fun `NF-16 이전 뒤 동기화는 북마크를 지키고 22개를 갱신한다`() =
        runTest {
            val notes = ParityNotes.all
            val old =
                notes.map { note ->
                    cached(
                        note.id,
                        note.hash,
                        raw = note.json,
                        parserVersion = PARSER_VERSION - 1,
                        blocksJson = format.derive(note.jsonPath, note.json).blocksJson,
                    ).copy(
                        key = "s0/${note.id}",
                        path = note.jsonPath,
                        group = note.group,
                        title = note.title,
                        date = note.date,
                    )
                }
            val bookmarked = notes.take(3).map { "s0/${it.id}" }
            val bookmarks =
                FakeBookmarkStore(
                    bookmarked.withIndex().associate { (index, key) ->
                        key to index.toLong()
                    },
                )
            val adapter =
                FakeAdapter(
                    notes.map {
                        SourceEntry(
                            it.id,
                            it.markdownPath,
                            sha256(it.markdownBytes),
                            it.group,
                            it.title,
                            it.date,
                        )
                    },
                    docs = notes.associate { it.id to it.markdown },
                )
            val local = FakeLocal(old)
            val repo = repo(adapter, local)

            val outcome = repo.sync().getOrThrow()
            BookmarkRepositoryImpl(bookmarks).remove(outcome.deleted)

            assertEquals(22, notes.size)
            assertEquals(0, outcome.added)
            assertEquals(22, outcome.updated)
            assertTrue(outcome.deleted.isEmpty())
            assertEquals(bookmarked.toSet(), bookmarks.entries.value.keys)
            val blocks = repo.blocks()
            assertEquals(notes.map { "s0/${it.id}" }.toSet(), blocks.keys)
            notes.forEach { note ->
                val row = local.store.value.getValue("s0/${note.id}")
                assertEquals(note.markdownPath, row.path)
                assertEquals(PARSER_VERSION, row.parserVersion)
                assertTrue(note.markdownPath, !blocks.getValue("s0/${note.id}").hasBox())
            }
        }
}
