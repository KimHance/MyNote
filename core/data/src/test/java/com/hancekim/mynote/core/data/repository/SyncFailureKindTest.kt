package com.hancekim.mynote.core.data.repository

import com.hancekim.mynote.core.domain.model.SyncFailure
import com.hancekim.mynote.core.format.NoteFormat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SyncFailureKindTest {
    @Test
    fun `SY-5 어댑터가 던진 실패 종류가 그대로 전달되고 노트 행은 바뀌지 않는다`() =
        runTest {
            val kinds =
                listOf(
                    SyncFailure.Offline(),
                    SyncFailure.TokenRejected(),
                    SyncFailure.AccessDenied(),
                    SyncFailure.RateLimited(),
                    SyncFailure.Server(),
                    SyncFailure.Format(),
                )

            kinds.forEach { failure ->
                val adapter = FakeAdapter(listOf(entry("a", "h2")), listError = failure)
                val local = FakeLocal(listOf(cached("stale", "h1")))
                var clock = 0L

                val result = NotesRepositoryImpl(setOf(adapter), local, NoteFormat(), {}) { clock++ }.sync()

                assertSame(failure, result.exceptionOrNull())
                assertEquals(setOf("s0/stale"), local.store.value.keys)
            }
        }
}
