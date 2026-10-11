package com.hancekim.mynote.shell

import androidx.lifecycle.SavedStateHandle
import com.hancekim.mynote.core.common.mvi.MviViewModel
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
import com.hancekim.mynote.core.domain.ObserveInsightsUseCase
import com.hancekim.mynote.core.domain.ObserveModelStatusUseCase
import com.hancekim.mynote.core.domain.ObserveStudyLogEnabledUseCase
import com.hancekim.mynote.core.domain.ObserveStudyStatsUseCase
import com.hancekim.mynote.core.domain.ObserveSyncRunsUseCase
import com.hancekim.mynote.core.domain.ObserveWeeklyHistoryUseCase
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
import com.hancekim.mynote.core.domain.model.UnsupportedReason
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
import com.hancekim.mynote.core.domain.stats.StatsPeriod
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.FailureLog
import com.hancekim.mynote.core.ui.NotesMessages
import com.hancekim.mynote.core.ui.signal.FeatureSignals
import com.hancekim.mynote.core.ui.signal.LastSyncHolder
import com.hancekim.mynote.feature.notes.NotesViewModel
import com.hancekim.mynote.feature.notes.quiz.QuizViewModel
import com.hancekim.mynote.feature.notes.settings.SettingsViewModel
import com.hancekim.mynote.feature.stats.StatsTrace
import com.hancekim.mynote.feature.stats.StatsViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import java.time.LocalDate

/**
 * 셸 ViewModel 과 기능 ViewModel 셋(노트 · 설정 · 퀴즈)과 통계 ViewModel 을 같은 Navigator · 신호 계약 · 마지막 동기화 홀더로 세운다 — 앱의 싱글턴과 같은 모양.
 * 가짜 저장소로 실제 UseCase 를 만들고, 시계는 runTest 스케줄러에 묶는다.
 */
internal class FeatureFixture(
    private val scope: TestScope,
    token: String? = null,
    bookmarks: List<Bookmark> = emptyList(),
    disconnect: Result<Unit> = Result.success(Unit),
    model: ModelStatus = ModelStatus.Ready,
    val probeGate: CompletableDeferred<Unit>? = null,
) {
    val clock = Clock { scope.testScheduler.currentTime }
    val wallClock = WallClock { WALL_BASE + scope.testScheduler.currentTime }
    val tokens = FakeTokens(token)
    val bookmarks = FakeBookmarks(bookmarks)
    val studyLog = FakeStudyLog()
    val connections = FakeConnections(disconnect)
    val notesRepo = FakeNotes()
    val models = FakeModels(model)
    val failures = mutableListOf<Throwable>()
    val signals = FeatureSignals()
    val lastSync = LastSyncHolder()
    val navigator = Navigator(scope.backgroundScope, NotesNavKey.Home, clock = clock)
    private val failureLog = FailureLog { _, _, cause -> failures += cause }
    private val probe = FakeProbe(probeGate)

    fun shell() =
        ShellViewModel(
            observeConnection = ObserveConnectionUseCase(connections),
            consumeSeed =
                ConsumeSeedUseCase(
                    null,
                    connections,
                    tokens,
                    SyncPendingConnectionUseCase(connections, tokens, tokens),
                ),
            observeFontScale = ObserveFontScaleUseCase(FakeSettings),
            observeModelStatus = ObserveModelStatusUseCase(models),
            navigator = navigator,
            signals = signals,
            lastSync = lastSync,
            clock = clock,
            failureLog = failureLog,
        )

    fun notes() =
        NotesViewModel(
            getCategories = GetCategoriesUseCase(notesRepo),
            observeSyncRuns =
                ObserveSyncRunsUseCase(SyncNotesUseCase(notesRepo), ObserveAppliedTokenUseCase(tokens), connections),
            observeConnection = ObserveConnectionUseCase(connections),
            observeBookmarks = ObserveBookmarksUseCase(bookmarks),
            addBookmark = AddBookmarkUseCase(bookmarks) { wallClock.nowMillis() },
            removeBookmarks = RemoveBookmarksUseCase(bookmarks),
            recordReadSession = RecordReadSessionUseCase(studyLog, FakeSettings),
            recordBookmarkEvent = RecordBookmarkEventUseCase(studyLog, FakeSettings),
            messages = FakeMessages,
            navigator = navigator,
            signals = signals,
            lastSync = lastSync,
            clock = clock,
            wallClock = wallClock,
            failureLog = failureLog,
        )

    fun settings() =
        SettingsViewModel(
            observeConnection = ObserveConnectionUseCase(connections),
            observeAppliedToken = ObserveAppliedTokenUseCase(tokens),
            observeFontScale = ObserveFontScaleUseCase(FakeSettings),
            observeStudyLogEnabled = ObserveStudyLogEnabledUseCase(FakeSettings),
            observeModelStatus = ObserveModelStatusUseCase(models),
            saveToken = SaveTokenUseCase(tokens),
            countErasable = CountErasableUseCase(connections),
            disconnect = DisconnectUseCase(connections),
            verifyConnectLink = VerifyConnectLinkUseCase(probe, tokens, connections, tokens),
            setFontScale = SetFontScaleUseCase(FakeSettings),
            setStudyLogEnabled = SetStudyLogEnabledUseCase(FakeSettings),
            clearStudyLog = ClearStudyLogUseCase(studyLog, FakeInsights, FakeScheduler, models, FakeSettings),
            startModelDownload = StartModelDownloadUseCase(models),
            cancelModelDownload = CancelModelDownloadUseCase(models),
            discardPartialModel = DiscardPartialModelUseCase(models),
            deleteModel = DeleteModelUseCase(models),
            messages = FakeMessages,
            navigator = navigator,
            signals = signals,
            lastSync = lastSync,
            failureLog = failureLog,
        )

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

    fun stats() =
        StatsViewModel(
            savedStateHandle = SavedStateHandle(),
            observeStudyStats = ObserveStudyStatsUseCase(studyLog, notesRepo, bookmarks),
            observeStudyLogEnabled = ObserveStudyLogEnabledUseCase(FakeSettings),
            observeInsights = ObserveInsightsUseCase(FakeInsights, FakeScheduler),
            observeHistory = ObserveWeeklyHistoryUseCase(studyLog, FakeInsights, FakeScheduler),
            observeModelStatus = ObserveModelStatusUseCase(models),
            navigator = navigator,
            signals = signals,
            work = UnconfinedTestDispatcher(scope.testScheduler),
            trace = SilentTrace,
            clock = clock,
            wallClock = wallClock,
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

    fun settle() = scope.advanceTimeBy(SETTLE)

    class FakeNotes : NotesRepository {
        var syncs = 0

        override fun observeCategories(): Flow<List<Category>> =
            flowOf(listOf(Category(CATEGORY, listOf(READABLE, UNREADABLE))))

        override suspend fun sync(): Result<SyncOutcome> {
            syncs += 1
            return Result.success(SyncOutcome())
        }
    }

    class FakeTokens(
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

    class FakeConnections(
        private val disconnectResult: Result<Unit>,
    ) : ConnectionRepository {
        val connection = MutableStateFlow<ConnectionState>(ConnectionState.Connected(SourceId("s1"), REPO))

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

    class FakeBookmarks(
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

    private class FakeProbe(
        private val gate: CompletableDeferred<Unit>?,
    ) : ConnectProbe {
        override suspend fun verify(
            repo: ConnectLinkRepo,
            candidates: List<ConnectCandidate>,
            token: String?,
        ): ConnectProbeResult {
            gate?.await()
            return ConnectProbeResult.Failed(ConnectProbeFailure.Transient)
        }
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

    private object SilentTrace : StatsTrace {
        override fun started(period: StatsPeriod) = Unit

        override fun computed(
            period: StatsPeriod,
            tookMillis: Long,
        ) = Unit

        override fun failed(cause: Throwable) = Unit
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
        val REPO = RepoConnection("octo-learner", "android-notes", "main", "notes/index.json", isPublic = true)

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
