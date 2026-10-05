package com.hancekim.mynote.core.domain

import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.QuizQuestion
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import com.hancekim.mynote.core.domain.repository.NotesRepository
import com.hancekim.mynote.core.domain.repository.QuizGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FixedNotes(
    private val note: Note,
) : NotesRepository {
    override fun observeCategories(): Flow<List<Category>> = flowOf(listOf(Category("g", listOf(note))))

    override suspend fun sync(): Result<SyncOutcome> = Result.success(SyncOutcome())
}

private class RecordingGenerator : QuizGenerator {
    var text: String? = null

    override suspend fun isAvailable(): Boolean = true

    override suspend fun generate(
        noteTitle: String,
        noteText: String,
        count: Int,
    ): Result<List<QuizQuestion>> {
        text = noteText
        return Result.success(emptyList())
    }
}

class GenerateQuizUseCaseTest {
    @Test
    fun `Unsupported 는 건진 글자만 퀴즈 평문에 들어가고 원문은 들어가지 않는다`() =
        runTest {
            val note =
                Note(
                    id = "s0/a",
                    title = "t",
                    date = "2026.01.01",
                    blocks =
                        listOf(
                            NoteBlock.Paragraph(listOf(NoteSpan("앞"))),
                            NoteBlock.Unsupported(
                                UnsupportedReason.UnknownType,
                                "timeline",
                                "건진 글자",
                                "{\"type\":\"timeline\"}",
                            ),
                            NoteBlock.Unsupported(
                                UnsupportedReason.BrokenBlock,
                                "heading",
                                null,
                                "{\"type\":\"heading\"}",
                            ),
                        ),
                )
            val generator = RecordingGenerator()

            GenerateQuizUseCase(FixedNotes(note), generator)("s0/a")

            assertEquals("앞\n건진 글자", generator.text)
        }
}
