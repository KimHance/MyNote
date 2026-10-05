package com.hancekim.mynote.core.database

import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hancekim.mynote.core.database.dao.NoteDao
import com.hancekim.mynote.core.database.entity.SourceNoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private fun row(
    source: String,
    docId: String,
    title: String = docId,
    epochDay: Long? = 20_000L,
    firstSeenAt: Long = 1L,
) = SourceNoteEntity(
    key = "$source/$docId",
    sourceId = source,
    docId = docId,
    path = null,
    group = "g",
    title = title,
    date = null,
    epochDay = epochDay,
    firstSeenAt = firstSeenAt,
    revision = "r",
    fetchedAt = null,
    parserVersion = 1,
    raw = "{}",
    blocksJson = "[]",
    excerpt = null,
)

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {
    private lateinit var database: NotesDatabase
    private lateinit var dao: NoteDao

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    NotesDatabase::class.java,
                ).build()
        dao = database.noteDao()
    }

    @After
    fun close() {
        database.close()
    }

    private suspend fun keys(): List<String> = dao.observeNotes().first().map { it.key }

    @Test
    fun `applySourceSync 는 upsert 와 delete 를 한 번에 반영한다`() =
        runTest {
            dao.upsertAll(listOf(row("s0", "old")))

            dao.applySourceSync("s0", listOf(row("s0", "new")), listOf("old"))

            assertEquals(listOf("s0/new"), keys())
        }

    @Test
    fun `삭제할 키가 900개를 넘어도 나눠서 지운다`() =
        runTest {
            val ids = (1..1_200).map { "n$it" }
            dao.upsertAll(ids.map { row("s0", it) } + row("s0", "keep"))

            dao.applySourceSync("s0", emptyList(), ids)

            assertEquals(listOf("s0/keep"), keys())
        }

    @Test
    fun `다른 소스의 같은 docId 는 지우지 않는다`() =
        runTest {
            dao.upsertAll(listOf(row("s0", "a"), row("s1", "a")))

            dao.applySourceSync("s0", emptyList(), listOf("a"))

            assertEquals(listOf("s1/a"), keys())
        }

    @Test
    fun `정렬은 epoch_day 내림차순 NULL 마지막 처음 본 시각 제목 순이다`() =
        runTest {
            dao.upsertAll(
                listOf(
                    row("s0", "noDate", epochDay = null, firstSeenAt = 9L),
                    row("s0", "older", epochDay = 19_000L),
                    row("s0", "b", title = "b", firstSeenAt = 1L),
                    row("s0", "a", title = "a", firstSeenAt = 1L),
                    row("s0", "newer", firstSeenAt = 5L),
                ),
            )

            assertEquals(listOf("s0/newer", "s0/a", "s0/b", "s0/older", "s0/noDate"), keys())
        }

    @Test
    fun `소스 상태는 그 소스 행만 읽는다`() =
        runTest {
            dao.upsertAll(listOf(row("s0", "a"), row("s1", "b"), row("s0", "c").copy(raw = null)))

            val state = dao.loadSourceState("s0").associateBy { it.docId }

            assertEquals(setOf("a", "c"), state.keys)
            assertEquals(true, state.getValue("a").hasRaw)
            assertEquals(false, state.getValue("c").hasRaw)
        }
}
