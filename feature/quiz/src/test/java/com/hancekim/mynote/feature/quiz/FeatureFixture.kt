package com.hancekim.mynote.feature.quiz

import com.hancekim.mynote.core.common.mvi.MviViewModel
import com.hancekim.mynote.core.common.throttle.WallClock
import com.hancekim.mynote.core.domain.GenerateQuizUseCase
import com.hancekim.mynote.core.domain.ObserveModelStatusUseCase
import com.hancekim.mynote.core.domain.RecordQuizAttemptUseCase
import com.hancekim.mynote.core.domain.model.BookmarkEvent
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.FontScale
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.QuizAttempt
import com.hancekim.mynote.core.domain.model.QuizQuestion
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import com.hancekim.mynote.core.domain.repository.NotesRepository
import com.hancekim.mynote.core.domain.repository.QuizGenerator
import com.hancekim.mynote.core.domain.repository.QuizModelSource
import com.hancekim.mynote.core.domain.repository.SettingsRepository
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import com.hancekim.mynote.core.ui.FailureLog
import com.hancekim.mynote.core.ui.NotesMessages
import com.hancekim.mynote.core.ui.signal.FeatureSignals
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy

/**
 * 퀴즈 ViewModel 을 앱의 싱글턴과 같은 신호 계약으로 세운다 — 노트 fixture 의 퀴즈 쪽 사본(기능 모듈 시험은 서로를 못 본다 · FM-2).
 * 가짜 저장소로 실제 UseCase 를 만들고, 시계는 runTest 스케줄러에 묶는다.
 */
internal class FeatureFixture(
    private val scope: TestScope,
    model: ModelStatus = ModelStatus.Ready,
) {
    val wallClock = WallClock { WALL_BASE + scope.testScheduler.currentTime }
    val studyLog = FakeStudyLog()
    val notesRepo = FakeNotes()
    val models = FakeModels(model)
    val failures = mutableListOf<Throwable>()
    val signals = FeatureSignals()
    private val failureLog = FailureLog { _, _, cause -> failures += cause }

    fun quiz() =
        QuizViewModel(
            observeModelStatus = ObserveModelStatusUseCase(models),
            generateQuiz = GenerateQuizUseCase(notesRepo, FakeGenerator),
            recordQuizAttempt = RecordQuizAttemptUseCase(studyLog, FakeSettings),
            autoAdvancePolicy = { true },
            messages = FakeMessages,
            signals = signals,
            wallClock = wallClock,
            failureLog = failureLog,
        )

    /** 상태를 구독하고 첫 동기화 · 쓰로틀 창이 지나도록 기다린다. */
    fun subscribe(vararg vms: MviViewModel<*, *>): List<Job> {
        val jobs =
            vms.map { vm ->
                scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) { vm.state.collect {} }
            }
        scope.advanceTimeBy(SETTLE)
        return jobs
    }

    class FakeNotes : NotesRepository {
        var syncs = 0

        override fun observeCategories(): Flow<List<Category>> =
            flowOf(listOf(Category(CATEGORY, listOf(READABLE, UNREADABLE))))

        override suspend fun sync(): Result<SyncOutcome> {
            syncs += 1
            return Result.success(SyncOutcome())
        }
    }

    class FakeStudyLog : StudyLogRepository {
        val sessions = mutableListOf<ReadSession>()
        val events = mutableListOf<BookmarkEvent>()
        val cancelledEvents = mutableListOf<Long>()

        override suspend fun record(session: ReadSession) {
            sessions += session
        }

        override suspend fun record(attempt: QuizAttempt) = Unit

        override suspend fun record(event: BookmarkEvent): Long {
            events += event
            return EVENT_ID
        }

        override suspend fun deleteBookmarkEvent(id: Long) {
            cancelledEvents += id
        }

        override fun observeWindow(
            from: Long,
            until: Long,
        ) = emptyFlow<Nothing>()

        override fun observeNoteFacts() = emptyFlow<Nothing>()

        override fun observeFirstRecordAt(): Flow<Long?> = flowOf(null)

        override suspend fun wrongAnswers(
            since: Long,
            limit: Int,
        ) = emptyList<Nothing>()

        override suspend fun countWrongAnswers(since: Long) = 0

        override suspend fun clear() = Unit

        override suspend fun prune(before: Long) = Unit

        override suspend fun pruneAnswers(before: Long) = Unit
    }

    class FakeModels(
        initial: ModelStatus,
    ) : QuizModelSource {
        val status = MutableStateFlow(initial)

        override fun status(): Flow<ModelStatus> = status

        override fun start() = Unit

        override fun cancel() = Unit

        override fun discardPartial() = Unit

        override suspend fun deleteModel() = Unit
    }

    private object FakeSettings : SettingsRepository {
        override fun observeFontScale(): Flow<FontScale> = flowOf(FontScale.Normal)

        override suspend fun setFontScale(scale: FontScale) = Unit

        override fun observeStudyLogEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setStudyLogEnabled(enabled: Boolean) = Unit
    }

    private object FakeGenerator : QuizGenerator {
        override suspend fun isAvailable() = true

        override suspend fun generate(
            noteTitle: String,
            noteText: String,
            count: Int,
        ): Result<List<QuizQuestion>> =
            Result.success(List(count) { QuizQuestion("문항 $it", listOf("가", "나", "다", "라"), 0, "해설") })
    }

    object FakeMessages : NotesMessages {
        override fun syncFailed() = "syncFailed"

        override fun quizAbandoned() = "quizAbandoned"

        override fun tokenSaved() = "tokenSaved"

        override fun disconnectFailed() = "disconnectFailed"

        override fun tokenCleared() = "tokenCleared"

        override fun downloadPaused() = "downloadPaused"

        override fun downloadCancelled() = "downloadCancelled"

        override fun partialDiscarded() = "partialDiscarded"

        override fun modelDeleted() = "modelDeleted"

        override fun bookmarkAdded() = "bookmarkAdded"

        override fun bookmarkRemoved() = "bookmarkRemoved"

        override fun studyLogCleared() = "studyLogCleared"

        override fun syncApplied(outcome: SyncOutcome) = "syncApplied"
    }

    companion object {
        const val NOTE_ID = "n1"
        const val UNREADABLE_ID = "n2"
        const val CATEGORY = "안드로이드"
        const val EVENT_ID = 7L
        const val WALL_BASE = 1_760_000_000_000L
        const val SETTLE = 1_000L

        private val READABLE =
            Note(NOTE_ID, "제목", "2026-10-01", listOf(NoteBlock.Paragraph(listOf(NoteSpan("본문 한 줄")))))

        private val UNREADABLE =
            Note(
                UNREADABLE_ID,
                "깨진 노트",
                "2026-10-02",
                listOf(NoteBlock.Unsupported(UnsupportedReason.BrokenNote, type = null, text = null, raw = "{")),
            )
    }
}
