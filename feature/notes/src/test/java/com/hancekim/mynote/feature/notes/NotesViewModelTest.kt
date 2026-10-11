package com.hancekim.mynote.feature.notes

import com.hancekim.mynote.core.common.throttle.Clock
import com.hancekim.mynote.core.common.throttle.WallClock
import com.hancekim.mynote.core.domain.AddBookmarkUseCase
import com.hancekim.mynote.core.domain.CancelModelDownloadUseCase
import com.hancekim.mynote.core.domain.ClearStudyLogUseCase
import com.hancekim.mynote.core.domain.ConsumeSeedUseCase
import com.hancekim.mynote.core.domain.CountErasableUseCase
import com.hancekim.mynote.core.domain.DeleteModelUseCase
import com.hancekim.mynote.core.domain.DiscardPartialModelUseCase
import com.hancekim.mynote.core.domain.DisconnectUseCase
import com.hancekim.mynote.core.domain.GenerateQuizUseCase
import com.hancekim.mynote.core.domain.GetCategoriesUseCase
import com.hancekim.mynote.core.domain.ObserveAppliedTokenUseCase
import com.hancekim.mynote.core.domain.ObserveBookmarksUseCase
import com.hancekim.mynote.core.domain.ObserveConnectionUseCase
import com.hancekim.mynote.core.domain.ObserveFontScaleUseCase
import com.hancekim.mynote.core.domain.ObserveModelStatusUseCase
import com.hancekim.mynote.core.domain.ObserveStudyLogEnabledUseCase
import com.hancekim.mynote.core.domain.ObserveSyncRunsUseCase
import com.hancekim.mynote.core.domain.RecordBookmarkEventUseCase
import com.hancekim.mynote.core.domain.RecordQuizAttemptUseCase
import com.hancekim.mynote.core.domain.RecordReadSessionUseCase
import com.hancekim.mynote.core.domain.RemoveBookmarksUseCase
import com.hancekim.mynote.core.domain.SaveTokenUseCase
import com.hancekim.mynote.core.domain.SetFontScaleUseCase
import com.hancekim.mynote.core.domain.SetStudyLogEnabledUseCase
import com.hancekim.mynote.core.domain.StartModelDownloadUseCase
import com.hancekim.mynote.core.domain.SyncNotesUseCase
import com.hancekim.mynote.core.domain.SyncPendingConnectionUseCase
import com.hancekim.mynote.core.domain.VerifyConnectLinkUseCase
import com.hancekim.mynote.core.domain.connect.ConnectCandidate
import com.hancekim.mynote.core.domain.connect.ConnectLinkRepo
import com.hancekim.mynote.core.domain.connect.ConnectProbe
import com.hancekim.mynote.core.domain.connect.ConnectProbeFailure
import com.hancekim.mynote.core.domain.connect.ConnectProbeResult
import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.core.domain.model.BookmarkEvent
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ErasureCount
import com.hancekim.mynote.core.domain.model.FontScale
import com.hancekim.mynote.core.domain.model.InsightFailureKind
import com.hancekim.mynote.core.domain.model.InsightStamp
import com.hancekim.mynote.core.domain.model.InsightWorkState
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.model.NetworkStatus
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.PendingConnection
import com.hancekim.mynote.core.domain.model.QuizAttempt
import com.hancekim.mynote.core.domain.model.QuizPattern
import com.hancekim.mynote.core.domain.model.QuizQuestion
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.model.WeeklySummary
import com.hancekim.mynote.core.domain.repository.AppliedTokenSource
import com.hancekim.mynote.core.domain.repository.BookmarkRepository
import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import com.hancekim.mynote.core.domain.repository.InsightRepository
import com.hancekim.mynote.core.domain.repository.InsightScheduler
import com.hancekim.mynote.core.domain.repository.NetworkStatusSource
import com.hancekim.mynote.core.domain.repository.NotesRepository
import com.hancekim.mynote.core.domain.repository.QuizGenerator
import com.hancekim.mynote.core.domain.repository.QuizModelSource
import com.hancekim.mynote.core.domain.repository.SettingsRepository
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import com.hancekim.mynote.core.domain.repository.TokenRepository
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesSignal
import com.hancekim.mynote.feature.notes.quiz.QuizUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `FM-9 루트에서 창 안에 두 번 누르면 종료한다`() =
        runTest {
            val vm = Fixture(this).viewModel()
            subscribe(vm)

            vm.onIntent(NotesIntent.Back)
            assertEquals(1, vm.state.value.exitHintId)
            assertEquals(0, vm.state.value.exitRequestId)

            advanceTimeBy(EXIT_WINDOW - 1)
            vm.onIntent(NotesIntent.Back)

            assertEquals(1, vm.state.value.exitRequestId)
        }

    @Test
    fun `FM-9 창이 지나면 다시 안내한다`() =
        runTest {
            val vm = Fixture(this).viewModel()
            subscribe(vm)

            vm.onIntent(NotesIntent.Back)
            advanceTimeBy(EXIT_WINDOW)
            vm.onIntent(NotesIntent.Back)

            assertEquals(2, vm.state.value.exitHintId)
            assertEquals(0, vm.state.value.exitRequestId)

            advanceTimeBy(EXIT_WINDOW - 1)
            vm.onIntent(NotesIntent.Back)
            assertEquals(1, vm.state.value.exitRequestId)
        }

    @Test
    fun `FM-9 늦게 온 자동 진행은 문항을 건너뛰지 않는다`() =
        runTest {
            val vm = Fixture(this).viewModel()
            subscribe(vm)
            startSolving(vm)

            vm.onIntent(NotesIntent.PickChoice(0))
            assertTrue(solving(vm).autoAdvancing)
            advanceTimeBy(AUTO_ADVANCE - 1)
            assertEquals(0, solving(vm).index)
            advanceTimeBy(2)
            assertEquals(1, solving(vm).index)

            vm.onIntent(NotesIntent.PickChoice(0))
            vm.onIntent(NotesIntent.NextQuestion())
            assertEquals(2, solving(vm).index)
            advanceTimeBy(AUTO_ADVANCE + 1)
            assertEquals(2, solving(vm).index)

            vm.onIntent(NotesIntent.PickChoice(0))
            vm.onIntent(NotesIntent.NextQuestion(fromIndex = 1))
            assertEquals(2, solving(vm).index)
        }

    @Test
    fun `FM-9 그만둔 퀴즈는 창 안에서만 되돌린다`() =
        runTest {
            val vm = Fixture(this).viewModel()
            subscribe(vm)
            startSolving(vm)
            vm.onIntent(NotesIntent.PickChoice(0))
            vm.onIntent(NotesIntent.NextQuestion())

            vm.onIntent(NotesIntent.DismissQuiz)
            assertEquals(QuizUiState.Idle, vm.state.value.quiz)
            assertEquals(1, vm.state.value.quizUndoId)
            advanceTimeBy(UNDO_WINDOW - 1)
            vm.onIntent(NotesIntent.UndoQuizExit)
            assertEquals(1, solving(vm).index)

            vm.onIntent(NotesIntent.DismissQuiz)
            assertEquals(2, vm.state.value.quizUndoId)
            advanceTimeBy(UNDO_WINDOW + 1)
            assertNull(vm.state.value.quizUndoMessage)
            assertEquals(0, vm.state.value.quizUndoId)
            vm.onIntent(NotesIntent.UndoQuizExit)
            assertEquals(QuizUiState.Idle, vm.state.value.quiz)
        }

    @Test
    fun `FM-9 지운 토큰을 되돌린다`() =
        runTest {
            val fixture = Fixture(this, token = "ghp_stored0000a3F9")
            val vm = fixture.viewModel()
            subscribe(vm)
            assertTrue(vm.state.value.hasToken)

            vm.onIntent(NotesIntent.ClearToken)
            runCurrent()
            assertEquals(false, vm.state.value.hasToken)
            val notice = vm.state.value.notice
            assertEquals("tokenCleared", notice?.message)
            assertEquals(NotesIntent.UndoClearToken, notice?.undo)

            vm.onIntent(NotesIntent.UndoClearToken)
            runCurrent()

            assertEquals(listOf(null, "ghp_stored0000a3F9"), fixture.tokens.saved)
            assertTrue(vm.state.value.hasToken)
        }

    @Test
    fun `FM-9 해제한 북마크를 되돌린다`() =
        runTest {
            val fixture = Fixture(this, bookmarks = listOf(Bookmark(NOTE_ID, addedAt = 100L)))
            val vm = fixture.viewModel()
            subscribe(vm)
            advanceTimeBy(5_000)

            vm.onIntent(NotesIntent.RemoveBookmark(NOTE_ID))
            runCurrent()
            assertTrue(
                vm.state.value.bookmarks
                    .isEmpty(),
            )
            assertEquals(
                NotesIntent.UndoRemoveBookmark,
                vm.state.value.notice
                    ?.undo,
            )
            assertEquals(
                WALL_BASE + SETTLE + 5_000,
                fixture.studyLog.events
                    .single()
                    .at,
            )

            vm.onIntent(NotesIntent.UndoRemoveBookmark)
            runCurrent()

            assertEquals(listOf(Bookmark(NOTE_ID, addedAt = 100L)), fixture.bookmarks.entries.value)
            assertEquals(listOf(EVENT_ID), fixture.studyLog.cancelledEvents)
            assertEquals(1, vm.state.value.bookmarks.size)
        }

    @Test
    fun `FM-9 본 신호는 다시 나오지 않는다`() =
        runTest {
            val vm = Fixture(this, bookmarks = listOf(Bookmark(NOTE_ID, addedAt = 100L))).viewModel()
            val first = subscribe(vm)

            vm.onIntent(NotesIntent.Back)
            vm.onIntent(NotesIntent.RemoveBookmark(NOTE_ID))
            runCurrent()
            val hint = vm.state.value.exitHintId
            val notice = requireNotNull(vm.state.value.notice)
            assertEquals(1, hint)

            vm.onIntent(NotesIntent.SignalShown(NotesSignal.ExitHint, hint))
            vm.onIntent(NotesIntent.SignalShown(NotesSignal.Notice, notice.id))
            assertEquals(0, vm.state.value.exitHintId)
            assertNull(vm.state.value.notice)

            first.cancel()
            advanceTimeBy(10_000)
            subscribe(vm)
            assertEquals(0, vm.state.value.exitHintId)
            assertNull(vm.state.value.notice)
        }

    @Test
    fun `FM-8 읽기 세션은 주입한 시계로 잰다`() =
        runTest {
            val fixture = Fixture(this)
            val vm = fixture.viewModel()
            subscribe(vm)
            vm.onIntent(NotesIntent.ForegroundChanged(true))

            vm.onIntent(NotesIntent.SelectNote(NOTE_ID))
            advanceTimeBy(40_000)
            vm.onIntent(NotesIntent.ForegroundChanged(false))
            runCurrent()

            val session = fixture.studyLog.sessions.single()
            assertEquals(NOTE_ID, session.noteId)
            assertEquals(WALL_BASE + SETTLE, session.startedAt)
            assertEquals(WALL_BASE + SETTLE + 40_000, session.endedAt)
            assertEquals(40_000L, session.activeMillis)
        }

    @Test
    fun `FM-8 처리 실패는 주입한 기록기로 남긴다`() =
        runTest {
            val fixture = Fixture(this, disconnect = Result.failure(IllegalStateException("boom")))
            val vm = fixture.viewModel()
            subscribe(vm)

            vm.onIntent(NotesIntent.ConfirmDisconnect)
            runCurrent()

            assertEquals("boom", fixture.failures.single().message)
            assertEquals(
                "disconnectFailed",
                vm.state.value.notice
                    ?.message,
            )
        }

    private fun TestScope.subscribe(vm: NotesViewModel): Job {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        advanceTimeBy(SETTLE)
        return job
    }

    private fun TestScope.startSolving(vm: NotesViewModel) {
        vm.onIntent(NotesIntent.OpenQuizSetup(NOTE_ID))
        vm.onIntent(NotesIntent.GenerateQuiz(NOTE_ID, 3))
        runCurrent()
        assertEquals(0, solving(vm).index)
    }

    private fun solving(vm: NotesViewModel): QuizUiState.Solving = vm.state.value.quiz as QuizUiState.Solving

    private class Fixture(
        scope: TestScope,
        token: String? = null,
        bookmarks: List<Bookmark> = emptyList(),
        disconnect: Result<Unit> = Result.success(Unit),
    ) {
        private val testScope = scope
        val clock = Clock { scope.testScheduler.currentTime }
        val wallClock = WallClock { WALL_BASE + scope.testScheduler.currentTime }
        val tokens = FakeTokens(token)
        val bookmarks = FakeBookmarks(bookmarks)
        val studyLog = FakeStudyLog()
        val connections = FakeConnections(disconnect)
        val failures = mutableListOf<Throwable>()

        fun viewModel(): NotesViewModel {
            val notes = FakeNotes
            val settings = FakeSettings
            val models = FakeModels
            val sync = ObserveSyncRunsUseCase(SyncNotesUseCase(notes), ObserveAppliedTokenUseCase(tokens), connections)
            return NotesViewModel(
                getCategories = GetCategoriesUseCase(notes),
                observeSyncRuns = sync,
                saveToken = SaveTokenUseCase(tokens),
                observeConnection = ObserveConnectionUseCase(connections),
                consumeSeed =
                    ConsumeSeedUseCase(
                        null,
                        connections,
                        tokens,
                        SyncPendingConnectionUseCase(connections, tokens, tokens),
                    ),
                countErasable = CountErasableUseCase(connections),
                disconnect = DisconnectUseCase(connections),
                verifyConnectLink = VerifyConnectLinkUseCase(FakeProbe, tokens, connections, tokens),
                observeAppliedToken = ObserveAppliedTokenUseCase(tokens),
                observeFontScale = ObserveFontScaleUseCase(settings),
                setFontScale = SetFontScaleUseCase(settings),
                observeBookmarks = ObserveBookmarksUseCase(bookmarks),
                addBookmark = AddBookmarkUseCase(bookmarks) { wallClock.nowMillis() },
                removeBookmarks = RemoveBookmarksUseCase(bookmarks),
                recordReadSession = RecordReadSessionUseCase(studyLog, settings),
                recordQuizAttempt = RecordQuizAttemptUseCase(studyLog, settings),
                recordBookmarkEvent = RecordBookmarkEventUseCase(studyLog, settings),
                observeStudyLogEnabled = ObserveStudyLogEnabledUseCase(settings),
                setStudyLogEnabled = SetStudyLogEnabledUseCase(settings),
                clearStudyLog = ClearStudyLogUseCase(studyLog, FakeInsights, FakeScheduler, models, settings),
                observeModelStatus = ObserveModelStatusUseCase(models),
                startModelDownload = StartModelDownloadUseCase(models),
                cancelModelDownload = CancelModelDownloadUseCase(models),
                discardPartialModel = DiscardPartialModelUseCase(models),
                deleteModel = DeleteModelUseCase(models),
                generateQuiz = GenerateQuizUseCase(notes, FakeGenerator),
                autoAdvancePolicy = { true },
                messages = FakeMessages,
                navigator = Navigator(testScope.backgroundScope, NotesNavKey.Home, clock = clock),
                clock = clock,
                wallClock = wallClock,
                failureLog = { _, _, cause -> failures += cause },
            )
        }
    }

    private object FakeNotes : NotesRepository {
        private val note =
            Note(NOTE_ID, "제목", "2026-10-01", listOf(NoteBlock.Paragraph(listOf(NoteSpan("본문 한 줄")))))

        override fun observeCategories(): Flow<List<Category>> = flowOf(listOf(Category("안드로이드", listOf(note))))

        override suspend fun sync(): Result<SyncOutcome> = Result.success(SyncOutcome())
    }

    private class FakeTokens(
        token: String?,
    ) : TokenRepository,
        AppliedTokenSource,
        NetworkStatusSource {
        private val stored = MutableStateFlow(token)
        val saved = mutableListOf<String?>()

        override suspend fun currentToken(): String? = stored.value

        override suspend fun saveToken(token: String?) {
            saved += token
            stored.value = token
        }

        override fun observeToken(): Flow<String?> = stored

        override fun applied(): Flow<String?> = stored

        override fun current() = NetworkStatus.WIFI
    }

    private class FakeConnections(
        private val disconnectResult: Result<Unit>,
    ) : ConnectionRepository {
        private val connection = MutableStateFlow<ConnectionState>(ConnectionState.Connected(SourceId("s1"), REPO))

        override fun observeConnection(): Flow<ConnectionState> = connection

        override fun observeAttempting(): Flow<Boolean> = flowOf(false)

        override suspend fun countErasable() = ErasureCount(0, 0)

        override suspend fun countAdoptionErasable(keptDocIds: Set<String>) = ErasureCount(0, 0)

        override suspend fun isSeedEligible() = false

        override suspend fun begin(
            connection: RepoConnection,
            token: String?,
        ): PendingConnection = PendingConnection(SourceId("s2"), connection, PendingConnection.Kind.New)

        override suspend fun syncPending(pending: PendingConnection): Result<SyncOutcome> =
            Result.success(
                SyncOutcome(),
            )

        override suspend fun confirm(pending: PendingConnection) = Unit

        override suspend fun discard(pending: PendingConnection) = Unit

        override suspend fun disconnect(): Result<Unit> = disconnectResult

        override suspend fun recover() = Unit
    }

    private class FakeBookmarks(
        initial: List<Bookmark>,
    ) : BookmarkRepository {
        val entries = MutableStateFlow(initial)

        override fun observeBookmarks(): Flow<List<Bookmark>> = entries

        override suspend fun add(bookmark: Bookmark) {
            entries.value = entries.value.filterNot { it.noteId == bookmark.noteId } + bookmark
        }

        override suspend fun remove(noteIds: Collection<String>) {
            entries.value = entries.value.filterNot { it.noteId in noteIds }
        }
    }

    private class FakeStudyLog : StudyLogRepository {
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

    private object FakeSettings : SettingsRepository {
        override fun observeFontScale(): Flow<FontScale> = flowOf(FontScale.Normal)

        override suspend fun setFontScale(scale: FontScale) = Unit

        override fun observeStudyLogEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setStudyLogEnabled(enabled: Boolean) = Unit
    }

    private object FakeModels : QuizModelSource {
        override fun status(): Flow<ModelStatus> = flowOf(ModelStatus.Ready)

        override fun start() = Unit

        override fun cancel() = Unit

        override fun discardPartial() = Unit

        override suspend fun deleteModel() = Unit
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

    private object FakeProbe : ConnectProbe {
        override suspend fun verify(
            repo: ConnectLinkRepo,
            candidates: List<ConnectCandidate>,
            token: String?,
        ): ConnectProbeResult = ConnectProbeResult.Failed(ConnectProbeFailure.Transient)
    }

    private object FakeInsights : InsightRepository {
        override fun observeLatestSummary(stamp: InsightStamp) = emptyFlow<Nothing>()

        override fun observeSummaries(stamp: InsightStamp) = emptyFlow<Nothing>()

        override fun observeLatestPattern(stamp: InsightStamp) = emptyFlow<Nothing>()

        override fun observeStatus() = emptyFlow<Nothing>()

        override suspend fun summaries(stamp: InsightStamp) = emptyList<Nothing>()

        override suspend fun latestPattern(stamp: InsightStamp) = null

        override suspend fun status() = error("안 쓴다")

        override suspend fun save(
            summary: WeeklySummary,
            stamp: InsightStamp,
        ) = Unit

        override suspend fun save(
            pattern: QuizPattern,
            stamp: InsightStamp,
        ) = Unit

        override suspend fun markSuccess(at: Long) = Unit

        override suspend fun markFailure(
            at: Long,
            kind: InsightFailureKind,
            summaryFailed: Boolean,
        ) = Unit

        override suspend fun markSkipped(at: Long) = Unit

        override suspend fun bumpEpoch() = 0L

        override suspend fun clearStatus() = Unit

        override suspend fun prune(before: LocalDate) = Unit
    }

    private object FakeScheduler : InsightScheduler {
        override val stamp = InsightStamp("test", 1)

        override suspend fun schedule() = Unit

        override fun cancel() = Unit

        override fun state(): Flow<InsightWorkState> = flowOf(InsightWorkState.Idle)
    }

    private object FakeMessages : NotesMessages {
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

    private companion object {
        const val NOTE_ID = "n1"
        const val EVENT_ID = 7L
        const val WALL_BASE = 1_760_000_000_000L
        const val SETTLE = 1_000L
        const val EXIT_WINDOW = 1_000L
        const val UNDO_WINDOW = 3_200L
        const val AUTO_ADVANCE = QuizUiState.AUTO_ADVANCE_MILLIS.toLong()
        val REPO = RepoConnection("octo-learner", "android-notes", "main", "notes/index.json", isPublic = true)
    }
}
