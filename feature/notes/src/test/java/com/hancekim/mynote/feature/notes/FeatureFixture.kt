package com.hancekim.mynote.feature.notes

import com.hancekim.mynote.core.common.mvi.MviViewModel
import com.hancekim.mynote.core.common.throttle.Clock
import com.hancekim.mynote.core.common.throttle.WallClock
import com.hancekim.mynote.core.domain.AddBookmarkUseCase
import com.hancekim.mynote.core.domain.GetCategoriesUseCase
import com.hancekim.mynote.core.domain.ObserveAppliedTokenUseCase
import com.hancekim.mynote.core.domain.ObserveBookmarksUseCase
import com.hancekim.mynote.core.domain.ObserveConnectionUseCase
import com.hancekim.mynote.core.domain.ObserveSyncRunsUseCase
import com.hancekim.mynote.core.domain.RecordBookmarkEventUseCase
import com.hancekim.mynote.core.domain.RecordReadSessionUseCase
import com.hancekim.mynote.core.domain.RemoveBookmarksUseCase
import com.hancekim.mynote.core.domain.SyncNotesUseCase
import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.core.domain.model.BookmarkEvent
import com.hancekim.mynote.core.domain.model.Category
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ErasureCount
import com.hancekim.mynote.core.domain.model.FontScale
import com.hancekim.mynote.core.domain.model.NetworkStatus
import com.hancekim.mynote.core.domain.model.Note
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.PendingConnection
import com.hancekim.mynote.core.domain.model.QuizAttempt
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SourceId
import com.hancekim.mynote.core.domain.model.SyncOutcome
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import com.hancekim.mynote.core.domain.repository.AppliedTokenSource
import com.hancekim.mynote.core.domain.repository.BookmarkRepository
import com.hancekim.mynote.core.domain.repository.ConnectionRepository
import com.hancekim.mynote.core.domain.repository.NetworkStatusSource
import com.hancekim.mynote.core.domain.repository.NotesRepository
import com.hancekim.mynote.core.domain.repository.SettingsRepository
import com.hancekim.mynote.core.domain.repository.StudyLogRepository
import com.hancekim.mynote.core.domain.repository.TokenRepository
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.FailureLog
import com.hancekim.mynote.core.ui.NotesMessages
import com.hancekim.mynote.core.ui.signal.FeatureSignals
import com.hancekim.mynote.core.ui.signal.LastSyncHolder
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
 * 노트 ViewModel 을 같은 Navigator · 신호 계약 · 마지막 동기화 홀더로 세운다 — 앱의 싱글턴과 같은 모양.
 * 가짜 저장소로 실제 UseCase 를 만들고, 시계는 runTest 스케줄러에 묶는다.
 */
internal class FeatureFixture(
    private val scope: TestScope,
    token: String? = null,
    bookmarks: List<Bookmark> = emptyList(),
    disconnect: Result<Unit> = Result.success(Unit),
) {
    val clock = Clock { scope.testScheduler.currentTime }
    val wallClock = WallClock { WALL_BASE + scope.testScheduler.currentTime }
    val tokens = FakeTokens(token)
    val bookmarks = FakeBookmarks(bookmarks)
    val studyLog = FakeStudyLog()
    val connections = FakeConnections(disconnect)
    val notesRepo = FakeNotes()
    val failures = mutableListOf<Throwable>()
    val signals = FeatureSignals()
    val lastSync = LastSyncHolder()
    val navigator = Navigator(scope.backgroundScope, NotesNavKey.Home, clock = clock)
    private val failureLog = FailureLog { _, _, cause -> failures += cause }

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
        val categories = MutableStateFlow(listOf(Category(CATEGORY, listOf(READABLE, UNREADABLE))))

        override fun observeCategories(): Flow<List<Category>> = categories

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

    private object FakeSettings : SettingsRepository {
        override fun observeFontScale(): Flow<FontScale> = flowOf(FontScale.Normal)

        override suspend fun setFontScale(scale: FontScale) = Unit

        override fun observeStudyLogEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setStudyLogEnabled(enabled: Boolean) = Unit
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
