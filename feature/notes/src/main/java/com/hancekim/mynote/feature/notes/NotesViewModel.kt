package com.hancekim.mynote.feature.notes

import android.os.SystemClock
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.viewModelScope
import com.hancekim.mynote.core.common.mvi.MviViewModel
import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale
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
import com.hancekim.mynote.core.domain.VerifyConnectLinkUseCase
import com.hancekim.mynote.core.domain.connect.ConnectVerification
import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.core.domain.model.BookmarkEvent
import com.hancekim.mynote.core.domain.model.BookmarkEventKind
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.FontScale
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.model.Moment
import com.hancekim.mynote.core.domain.model.QuizFailure
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.ReadSessionTracker
import com.hancekim.mynote.core.domain.model.SyncRun
import com.hancekim.mynote.core.navigation.NavCommand
import com.hancekim.mynote.core.navigation.NavResult
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.navigation.key.activeNoteId
import com.hancekim.mynote.feature.notes.element.DisconnectDialogUi
import com.hancekim.mynote.feature.notes.element.LastSync
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.element.NoticeUi
import com.hancekim.mynote.feature.notes.element.ReaderScrollRequest
import com.hancekim.mynote.feature.notes.element.TokenEditEffect
import com.hancekim.mynote.feature.notes.element.TokenEditEvent
import com.hancekim.mynote.feature.notes.element.TokenEditState
import com.hancekim.mynote.feature.notes.element.TokenFailureKind
import com.hancekim.mynote.feature.notes.element.connectionLinkOf
import com.hancekim.mynote.feature.notes.element.lastSyncOf
import com.hancekim.mynote.feature.notes.element.maskToken
import com.hancekim.mynote.feature.notes.element.reduceTokenEdit
import com.hancekim.mynote.feature.notes.element.screenCleanupOf
import com.hancekim.mynote.feature.notes.element.tokenFailureOfProbe
import com.hancekim.mynote.feature.notes.mapper.bookmarkedNotes
import com.hancekim.mynote.feature.notes.mapper.toUi
import com.hancekim.mynote.feature.notes.navigation.switchTabCommand
import com.hancekim.mynote.feature.notes.quiz.QuizAutoAdvancePolicy
import com.hancekim.mynote.feature.notes.quiz.QuizMeta
import com.hancekim.mynote.feature.notes.quiz.QuizUiState
import com.hancekim.mynote.feature.notes.quiz.completedAttempt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val EXIT_WINDOW_MILLIS = 1000L

private const val UNDO_WINDOW_MILLIS = 3200L

private const val GENERATE_TIMEOUT_MILLIS = 60_000L

private data class CategoryLoad(
    val loaded: Boolean,
    val list: ImmutableList<CategoryUi>,
)

private data class QuizUndo(
    val message: String,
    val id: Int,
)

private data class SyncState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val errorId: Int = 0,
    val infoMessage: String? = null,
    val infoId: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    getCategories: GetCategoriesUseCase,
    observeSyncRuns: ObserveSyncRunsUseCase,
    private val saveToken: SaveTokenUseCase,
    observeConnection: ObserveConnectionUseCase,
    private val consumeSeed: ConsumeSeedUseCase,
    private val countErasable: CountErasableUseCase,
    private val disconnect: DisconnectUseCase,
    private val verifyConnectLink: VerifyConnectLinkUseCase,
    observeAppliedToken: ObserveAppliedTokenUseCase,
    observeFontScale: ObserveFontScaleUseCase,
    private val setFontScale: SetFontScaleUseCase,
    observeBookmarks: ObserveBookmarksUseCase,
    private val addBookmark: AddBookmarkUseCase,
    private val removeBookmarks: RemoveBookmarksUseCase,
    private val recordReadSession: RecordReadSessionUseCase,
    private val recordQuizAttempt: RecordQuizAttemptUseCase,
    private val recordBookmarkEvent: RecordBookmarkEventUseCase,
    observeStudyLogEnabled: ObserveStudyLogEnabledUseCase,
    private val setStudyLogEnabled: SetStudyLogEnabledUseCase,
    private val clearStudyLog: ClearStudyLogUseCase,
    observeModelStatus: ObserveModelStatusUseCase,
    private val startModelDownload: StartModelDownloadUseCase,
    private val cancelModelDownload: CancelModelDownloadUseCase,
    private val discardPartialModel: DiscardPartialModelUseCase,
    private val deleteModel: DeleteModelUseCase,
    private val generateQuiz: GenerateQuizUseCase,
    private val autoAdvancePolicy: QuizAutoAdvancePolicy,
    private val messages: NotesMessages,
    private val navigator: Navigator,
) : MviViewModel<NotesUiState, NotesIntent>() {
    private val navStack = navigator.stack.triggerIn(navigator.stack.value)
    private val navSettled = navigator.isSettled.triggerIn(true)
    private val quiz = trigger<QuizUiState>(QuizUiState.Idle)

    private val quizUndo = trigger<QuizUndo?>(null)
    private var quizJob: Job? = null
    private var autoAdvanceJob: Job? = null
    private var undoJob: Job? = null

    private var abandonedSolving: QuizUiState.Solving? = null
    private var undoSeq = 0
    private val exitHints = trigger(0)
    private val exitRequests = trigger(0)
    private var lastRootBackMillis = 0L
    private val query = trigger("")
    private val expanded = trigger(persistentMapOf<String, Boolean>())
    private val fontScale =
        observeFontScale()
            .map { it.toUi() }
            .catch { emit(MyNoteFontScale.Normal) }
            .triggerIn(MyNoteFontScale.Normal, started = SharingStarted.Lazily)
    private val modelStatus =
        observeModelStatus()
            .catch { emit(ModelStatus.Missing) }
            .triggerIn(ModelStatus.Missing, started = SharingStarted.Lazily)
    private val tokenEdit = trigger(TokenEditState.Viewing)
    private var verifyJob: Job? = null
    private val connection = trigger<ConnectionState?>(null)
    private var previousConnection: ConnectionState? = null
    private val seedPending = trigger(true)
    private val lastSync = trigger<LastSync>(LastSync.None)
    private val disconnectDialog = trigger<DisconnectDialogUi?>(null)
    private val notice = trigger<NoticeUi?>(null)
    private var clearedToken: String? = null
    private var noticeSeq = 0
    private val refreshTicks = MutableStateFlow(0)

    private val appliedToken =
        observeAppliedToken()
            .triggerIn(null, started = SharingStarted.Lazily)

    private val categories: Flow<ImmutableList<CategoryUi>> =
        getCategories()
            .catch { emit(emptyList()) }
            .map { it.toUi() }

    private val categoryLoad =
        categories
            .map { CategoryLoad(loaded = true, list = it) }
            .triggerIn(CategoryLoad(loaded = false, list = persistentListOf()), started = SharingStarted.Lazily)

    private val categoryList: Flow<ImmutableList<CategoryUi>> = categoryLoad.map { it.list }

    private val bookmarkEntries =
        observeBookmarks()
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val bookmarkedIds =
        bookmarkEntries
            .map { entries -> entries.map { it.noteId }.toImmutableSet() }
            .triggerIn(persistentSetOf())

    private val bookmarks =
        combine(categoryList, bookmarkEntries) { list, entries -> bookmarkedNotes(list, entries) }
            .triggerIn(persistentListOf())

    private var removedBookmark: Bookmark? = null
    private var removedEventId: Long? = null

    private val studyLogEnabled =
        observeStudyLogEnabled()
            .catch { emit(true) }
            .triggerIn(true, started = SharingStarted.Lazily)

    private val readerScrollRequest = trigger<ReaderScrollRequest?>(null)
    private var scrollRequestSeq = 0
    private val tracker = ReadSessionTracker()
    private var quizMeta: QuizMeta? = null

    private val recentNotes =
        categoryList
            .map { list ->
                list
                    .flatMap { c -> c.notes.map { RecentNoteUi(it, c.name) } }
                    .sortedByDescending { it.note.date }
                    .toImmutableList()
            }.triggerIn(persistentListOf())

    private val visibleCategories =
        combine(categoryList, query) { list, keyword ->
            if (keyword.isBlank()) {
                list
            } else {
                list
                    .mapNotNull { c ->
                        val matched =
                            c.notes.filter { note ->
                                note.title.contains(keyword, ignoreCase = true) ||
                                    c.name.contains(keyword, ignoreCase = true)
                            }
                        if (matched.isEmpty()) null else c.copy(notes = matched.toImmutableList())
                    }.toImmutableList()
            }
        }.triggerIn(persistentListOf())

    private var errorSeq = 0
    private var infoSeq = 0

    private val sync =
        observeSyncRuns(refreshTicks)
            .onEach { run -> afterSyncRun(run) }
            .map { run -> run.toSyncState() }
            .catch { emit(SyncState(isLoading = false, errorMessage = messages.syncFailed(), errorId = ++errorSeq)) }
            .triggerIn(SyncState())

    private val snackbarHostState = SnackbarHostState()

    init {
        navigator.stack
            .map { it.activeNoteId() }
            .distinctUntilChanged()
            .onEach { noteId -> recordSession(tracker.onTop(noteId, categoryOf(noteId), moment())) }
            .launchIn(viewModelScope)
        quiz
            .map { it.coversReader }
            .distinctUntilChanged()
            .onEach { covered -> tracker.onCovered(covered, moment()) }
            .launchIn(viewModelScope)
        observeConnection()
            .catch { emit(ConnectionState.None) }
            .distinctUntilChanged()
            .onEach { next -> onConnectionChanged(next) }
            .launchIn(viewModelScope)
        launchSafely {
            try {
                consumeSeed()
            } finally {
                seedPending.value = false
            }
        }
    }

    override fun build() =
        NotesUiState(
            categories = categoryLoad.value.list,
            visibleCategories = visibleCategories.value,
            recentNotes = recentNotes.value,
            bookmarkedIds = bookmarkedIds.value,
            bookmarks = bookmarks.value,
            backStack = navStack.value.toImmutableList(),
            activeNoteId = navStack.value.activeNoteId(),
            isNavSettled = navSettled.value,
            exitHintId = exitHints.value,
            exitRequestId = exitRequests.value,
            query = query.value,
            expanded = expanded.value,
            fontScale = fontScale.value,
            isLoading = sync.value.isLoading,
            errorMessage = sync.value.errorMessage,
            errorId = sync.value.errorId,
            hasToken = !appliedToken.value.isNullOrBlank(),
            maskedToken = maskToken(appliedToken.value),
            tokenEdit = tokenEdit.value,
            connection = connection.value,
            notesLoaded = categoryLoad.value.loaded,
            seedPending = seedPending.value,
            lastSync = lastSync.value,
            disconnectDialog = disconnectDialog.value,
            notice = notice.value,
            infoMessage = sync.value.infoMessage,
            infoId = sync.value.infoId,
            modelStatus = modelStatus.value,
            quiz = quiz.value,
            quizUndoMessage = quizUndo.value?.message,
            quizUndoId = quizUndo.value?.id ?: 0,
            studyLogEnabled = studyLogEnabled.value,
            readerScrollRequest = readerScrollRequest.value,
            snackbarHostState = snackbarHostState,
        )

    override fun onFailure(cause: Throwable) {
        Log.e(TAG, "인텐트 처리 실패", cause)
    }

    override fun handleIntent(event: NotesIntent) {
        when (event) {
            is NotesIntent.SelectNote -> {
                if (navigator.stack.value.lastOrNull() is NotesNavKey.Settings) {
                    navigator.dispatchImmediately(NavCommand.Pop)
                }
                val result = navigator.push(NotesNavKey.Reader(event.id))
                val resumeAt = event.resumeAt
                if (resumeAt != null && result != NavResult.Throttled) {
                    readerScrollRequest.value = ReaderScrollRequest(event.id, resumeAt, ++scrollRequestSeq)
                }
            }

            is NotesIntent.OpenQuizFor -> {
                if (navigator.stack.value.lastOrNull() is NotesNavKey.Settings) {
                    navigator.dispatchImmediately(NavCommand.Pop)
                }
                if (navigator.push(NotesNavKey.Reader(event.noteId)) == NavResult.Throttled) return
                if (modelStatus.value == ModelStatus.Ready && isReadable(event.noteId)) {
                    cancelQuizWork()
                    clearUndo()
                    quiz.value = QuizUiState.Setup(event.noteId)
                }
            }

            is NotesIntent.ForegroundChanged -> {
                recordSession(tracker.onVisible(event.visible, moment()))
            }

            is NotesIntent.ReaderProgress -> {
                tracker.onProgress(event.noteId, event.percent)
            }

            is NotesIntent.SetStudyLogEnabled -> {
                launchSafely { setStudyLogEnabled(event.enabled) }
            }

            NotesIntent.ClearStudyLog -> {
                removedEventId = null
                launchSafely {
                    clearStudyLog()
                    announce(messages.studyLogCleared())
                }
            }

            NotesIntent.OpenSettings -> {
                applyTokenEdit(TokenEditEvent.EnterSettings)
                navigator.push(NotesNavKey.Settings)
            }

            NotesIntent.OpenConnect -> {
                if (!applyTokenEdit(TokenEditEvent.ChangeRepository)) navigator.push(NotesNavKey.Connect)
            }

            NotesIntent.RequestDisconnect -> {
                if (applyTokenEdit(TokenEditEvent.Disconnect)) return
                launchSafely {
                    val count = countErasable()
                    disconnectDialog.value = DisconnectDialogUi(count.notes, count.bookmarks)
                }
            }

            NotesIntent.ConfirmDisconnect -> {
                disconnectDialog.value = null
                launchSafely {
                    disconnect().onFailure {
                        Log.e(TAG, "연결 끊기 실패", it)
                        announce(messages.disconnectFailed())
                    }
                }
            }

            NotesIntent.DismissDisconnect -> {
                disconnectDialog.value = null
            }

            is NotesIntent.QueryChanged -> {
                query.value = event.query
            }

            is NotesIntent.ToggleCategory -> {
                expanded.update { it.putting(event.name, !(it[event.name] ?: false)) }
            }

            is NotesIntent.SetFontScale -> {
                launchSafely { setFontScale(event.scale.toDomain()) }
            }

            NotesIntent.StartModelDownload -> {
                startModelDownload()
            }

            NotesIntent.CancelModelDownload -> {
                val received = (modelStatus.value as? ModelStatus.Downloading)?.received ?: 0L
                cancelModelDownload()
                announce(if (received > 0L) messages.downloadPaused() else messages.downloadCancelled())
            }

            NotesIntent.DiscardPartialModel -> {
                discardPartialModel()
                announce(messages.partialDiscarded())
            }

            NotesIntent.DeleteModel -> {
                launchSafely {
                    deleteModel()
                    announce(messages.modelDeleted())
                }
            }

            NotesIntent.Back -> {
                if (navigator.pop() == NavResult.Ignored) {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastRootBackMillis < EXIT_WINDOW_MILLIS) {
                        exitRequests.update { it + 1 }
                    } else {
                        lastRootBackMillis = now
                        exitHints.update { it + 1 }
                    }
                }
            }

            is NotesIntent.SwitchTab -> {
                navigator.dispatch(navigator.stack.value.switchTabCommand(event.tab))
            }

            NotesIntent.ToggleBookmark -> {
                val noteId = navigator.stack.value.activeNoteId() ?: return
                removedBookmark = null
                removedEventId = null
                if (bookmarkEntries.value.any { it.noteId == noteId }) {
                    launchSafely {
                        removeBookmarks(listOf(noteId))
                        announce(messages.bookmarkRemoved())
                    }
                    recordBookmark(noteId, BookmarkEventKind.Removed)
                } else {
                    launchSafely {
                        addBookmark(noteId)
                        announce(messages.bookmarkAdded())
                    }
                    recordBookmark(noteId, BookmarkEventKind.Added)
                }
            }

            is NotesIntent.RemoveBookmark -> {
                val entry = bookmarkEntries.value.firstOrNull { it.noteId == event.noteId } ?: return
                removedBookmark = entry
                removedEventId = null
                launchSafely {
                    removeBookmarks(listOf(entry.noteId))
                    announce(messages.bookmarkRemoved(), undo = NotesIntent.UndoRemoveBookmark)
                }
                recordBookmark(entry.noteId, BookmarkEventKind.Removed) { id ->
                    if (removedBookmark === entry) removedEventId = id
                }
            }

            NotesIntent.UndoRemoveBookmark -> {
                val entry = removedBookmark ?: return
                val eventId = removedEventId
                removedBookmark = null
                removedEventId = null
                if (categoryLoad.value.list.none { c -> c.notes.any { it.id == entry.noteId } }) return
                launchSafely { addBookmark(entry.noteId, entry.addedAt) }
                if (eventId !=
                    null
                ) {
                    launchSafely { withContext(NonCancellable) { recordBookmarkEvent.cancel(eventId) } }
                }
            }

            NotesIntent.StartTokenEdit -> {
                applyTokenEdit(TokenEditEvent.Start)
            }

            NotesIntent.CancelTokenEdit -> {
                applyTokenEdit(TokenEditEvent.Dismiss)
            }

            is NotesIntent.SaveToken -> {
                val token = event.token.value.trim()
                applyTokenEdit(TokenEditEvent.Save(blank = token.isEmpty()), token)
            }

            NotesIntent.ClearToken -> {
                applyTokenEdit(TokenEditEvent.Clear(hasToken = !appliedToken.value.isNullOrBlank()))
            }

            NotesIntent.UndoClearToken -> {
                applyTokenEdit(TokenEditEvent.Undo)
            }

            is NotesIntent.OpenQuizSetup -> {
                cancelQuizWork()
                clearUndo()
                quiz.value = QuizUiState.Setup(event.noteId)
            }

            is NotesIntent.SetQuizCount -> {
                val setup = quiz.value as? QuizUiState.Setup ?: return
                quiz.value = setup.copy(maxCount = event.count)
            }

            is NotesIntent.GenerateQuiz -> {
                startQuiz(event.noteId, event.maxCount)
            }

            NotesIntent.RetryQuiz -> {
                val failed = quiz.value as? QuizUiState.Failed ?: return
                startQuiz(failed.noteId, failed.maxCount)
            }

            is NotesIntent.RetryQuizWithFewer -> {
                val failed = quiz.value as? QuizUiState.Failed ?: return
                startQuiz(failed.noteId, event.count)
            }

            is NotesIntent.PickChoice -> {
                val solving = quiz.value as? QuizUiState.Solving ?: return
                val graded = solving.pick(event.index)
                if (graded === solving) return

                val auto = graded.isCorrect && autoAdvancePolicy.isEnabled()
                quiz.value = graded.copy(autoAdvancing = auto)
                if (auto) armAutoAdvance(graded.index)
            }

            is NotesIntent.NextQuestion -> {
                autoAdvanceJob?.cancel()
                val solving = quiz.value as? QuizUiState.Solving ?: return
                if (event.fromIndex != null && event.fromIndex != solving.index) return
                val next = solving.advance()
                quiz.value = next
                recordAttempt(solving, next)
            }

            NotesIntent.DismissQuiz -> {
                dismissQuiz()
            }

            NotesIntent.UndoQuizExit -> {
                val restored = abandonedSolving ?: return
                clearUndo()
                quiz.value = restored
                if (restored.autoAdvancing) armAutoAdvance(restored.index)
            }

            NotesIntent.Refresh -> {
                refreshTicks.update { it + 1 }
            }
        }
    }

    private fun startQuiz(
        noteId: String,
        maxCount: Int,
    ) {
        if (quizJob?.isActive == true) return
        autoAdvanceJob?.cancel()
        clearUndo()
        quiz.value = QuizUiState.Loading(noteId, maxCount)
        quizJob =
            launchSafely {
                val outcome = withTimeoutOrNull(GENERATE_TIMEOUT_MILLIS.milliseconds) { generateQuiz(noteId, maxCount) }
                val next =
                    outcome?.fold(
                        onSuccess = { questions -> solvingOrEmpty(questions.map { it.toUi() }, noteId, maxCount) },
                        onFailure = { QuizUiState.Failed(it.toFailKind(), maxCount, noteId) },
                    ) ?: QuizUiState.Failed(QuizUiState.Failed.Kind.Timeout, maxCount, noteId)
                if (next is QuizUiState.Solving) quizMeta = QuizMeta(noteId, System.currentTimeMillis())
                quiz.value = next
            }
    }

    private fun solvingOrEmpty(
        questions: List<QuizQuestionUi>,
        noteId: String,
        maxCount: Int,
    ): QuizUiState =
        if (questions.isEmpty()) {
            QuizUiState.Failed(QuizUiState.Failed.Kind.Empty, maxCount, noteId)
        } else {
            QuizUiState.Solving(
                questions = questions.toImmutableList(),
                partial = questions.size < maxCount,
            )
        }

    private fun armAutoAdvance(fromIndex: Int) {
        autoAdvanceJob?.cancel()
        autoAdvanceJob =
            launchSafely {
                delay(QuizUiState.AUTO_ADVANCE_MILLIS.milliseconds)
                onIntent(NotesIntent.NextQuestion(fromIndex = fromIndex))
            }
    }

    private fun dismissQuiz() {
        val leaving = quiz.value
        cancelQuizWork()
        quiz.value = QuizUiState.Idle

        if (leaving !is QuizUiState.Solving) {
            clearUndo()
            return
        }
        abandonedSolving = leaving
        quizUndo.value = QuizUndo(messages.quizAbandoned(), ++undoSeq)
        undoJob?.cancel()
        undoJob =
            launchSafely {
                delay(UNDO_WINDOW_MILLIS.milliseconds)
                abandonedSolving = null
                quizUndo.value = null
            }
    }

    private fun cancelQuizWork() {
        autoAdvanceJob?.cancel()
        quizJob?.cancel()
    }

    private fun moment(): Moment = Moment(System.currentTimeMillis(), SystemClock.elapsedRealtime())

    private fun categoryOf(noteId: String?): String? {
        if (noteId == null) return null
        return categoryLoad.value.list
            .firstOrNull { c -> c.notes.any { it.id == noteId } }
            ?.name
    }

    private fun isReadable(noteId: String): Boolean =
        categoryLoad.value.list
            .firstNotNullOfOrNull { c -> c.notes.firstOrNull { it.id == noteId } }
            ?.readable != false

    private fun recordSession(session: ReadSession?) {
        val closed = session ?: return
        val resolved =
            if (closed.category.isEmpty()) {
                closed.copy(
                    category = categoryOf(closed.noteId).orEmpty(),
                )
            } else {
                closed
            }
        launchSafely { withContext(NonCancellable) { recordReadSession(resolved) } }
    }

    private fun recordAttempt(
        before: QuizUiState,
        after: QuizUiState,
    ) {
        val meta = quizMeta ?: return
        val attempt =
            completedAttempt(
                before = before,
                after = after,
                meta = meta,
                category = categoryOf(meta.noteId).orEmpty(),
                finishedAt = System.currentTimeMillis(),
            ) ?: return
        launchSafely { withContext(NonCancellable) { recordQuizAttempt(attempt) } }
    }

    private fun recordBookmark(
        noteId: String,
        kind: BookmarkEventKind,
        onRecorded: (Long) -> Unit = {},
    ) {
        val event = BookmarkEvent(noteId, categoryOf(noteId).orEmpty(), kind, System.currentTimeMillis())
        launchSafely {
            val id = withContext(NonCancellable) { recordBookmarkEvent(event) }
            if (id != null) onRecorded(id)
        }
    }

    private fun announce(
        message: String,
        undo: NotesIntent? = null,
    ) {
        notice.value = NoticeUi(message, undo = undo, id = ++noticeSeq)
    }

    private fun clearUndo() {
        undoJob?.cancel()
        abandonedSolving = null
        if (quizUndo.value != null) quizUndo.value = null
    }

    private fun afterSyncRun(run: SyncRun) {
        if (run !is SyncRun.Finished) return
        lastSync.value = lastSyncOf(run.result)
        val outcome = run.result.getOrNull() ?: return
        val openNote = navigator.stack.value.activeNoteId()
        if (openNote != null && openNote in outcome.deleted) {
            navigator.dispatchImmediately(NavCommand.Pop)
        }
        if (outcome.deleted.isNotEmpty()) {
            val deleted = outcome.deleted
            launchSafely { removeBookmarks(deleted) }
        }
    }

    private fun SyncRun.toSyncState(): SyncState =
        when (this) {
            SyncRun.Idle -> {
                SyncState(isLoading = false)
            }

            is SyncRun.Running -> {
                SyncState(isLoading = true)
            }

            is SyncRun.Finished -> {
                val outcome = result.getOrNull()
                when {
                    outcome == null -> {
                        SyncState(isLoading = false, errorMessage = messages.syncFailed(), errorId = ++errorSeq)
                    }

                    announce && outcome.hasChanges -> {
                        SyncState(
                            isLoading = false,
                            infoMessage = messages.syncApplied(outcome),
                            infoId = ++infoSeq,
                        )
                    }

                    else -> {
                        SyncState(isLoading = false)
                    }
                }
            }
        }

    private fun onConnectionChanged(next: ConnectionState) {
        val cleanup = screenCleanupOf(previousConnection, next)
        previousConnection = next
        if (cleanup != null) {
            if (cleanup.resetStack) {
                navigator.dispatchImmediately(NavCommand.ReplaceAll(listOf(NotesNavKey.Home)))
            }
            if (cleanup.dismissQuiz) {
                cancelQuizWork()
                clearUndo()
                quiz.value = QuizUiState.Idle
            }
            lastSync.value = if (next is ConnectionState.Connected) LastSync.Succeeded else LastSync.None
        }
        connection.value = next
    }

    private fun applyTokenEdit(
        event: TokenEditEvent,
        token: String? = null,
    ): Boolean {
        val transition = reduceTokenEdit(tokenEdit.value, event)
        tokenEdit.value = transition.next
        var blocked = false
        transition.effects.forEach { effect ->
            when (effect) {
                TokenEditEffect.Verify -> verifyToken(token.orEmpty())
                TokenEditEffect.CancelRequest -> verifyJob?.cancel()
                TokenEditEffect.DiscardInput -> Unit
                TokenEditEffect.ClearStored -> clearStoredToken()
                TokenEditEffect.RestoreStored -> restoreStoredToken()
                TokenEditEffect.Saved -> announce(messages.tokenSaved())
                TokenEditEffect.Blocked -> blocked = true
            }
        }
        return blocked
    }

    private fun verifyToken(token: String) {
        verifyJob?.cancel()
        val repo = (connection.value as? ConnectionState.Connected)?.connection
        verifyJob =
            launchSafely {
                try {
                    if (repo == null) {
                        applyTokenEdit(TokenEditEvent.VerifyFailed(TokenFailureKind.Transient))
                        return@launchSafely
                    }
                    when (val outcome = verifyConnectLink(connectionLinkOf(repo), token)) {
                        is ConnectVerification.Ready -> {
                            withContext(NonCancellable) { saveToken(token) }
                            clearedToken = null
                            applyTokenEdit(TokenEditEvent.VerifySucceeded)
                        }

                        is ConnectVerification.Rejected -> {
                            applyTokenEdit(TokenEditEvent.VerifyFailed(TokenFailureKind.Rejected))
                        }

                        is ConnectVerification.Failed -> {
                            applyTokenEdit(TokenEditEvent.VerifyFailed(tokenFailureOfProbe(outcome.failure)))
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Log.e(TAG, "토큰 확인 실패", failure)
                    applyTokenEdit(TokenEditEvent.VerifyFailed(TokenFailureKind.Transient))
                }
            }
    }

    private fun clearStoredToken() {
        val previous = appliedToken.value
        if (previous.isNullOrBlank()) return
        clearedToken = previous
        launchSafely {
            saveToken("")
            announce(messages.tokenCleared(), undo = NotesIntent.UndoClearToken)
        }
    }

    private fun restoreStoredToken() {
        val previous = clearedToken ?: return
        clearedToken = null
        launchSafely { saveToken(previous) }
    }

    private fun Throwable.toFailKind(): QuizUiState.Failed.Kind =
        when (this) {
            is QuizFailure.Parse -> QuizUiState.Failed.Kind.Parse
            is QuizFailure.Empty -> QuizUiState.Failed.Kind.Empty
            else -> QuizUiState.Failed.Kind.Engine
        }
}

private fun FontScale.toUi(): MyNoteFontScale =
    when (this) {
        FontScale.Normal -> MyNoteFontScale.Normal
        FontScale.Large -> MyNoteFontScale.Large
    }

private fun MyNoteFontScale.toDomain(): FontScale =
    when (this) {
        MyNoteFontScale.Normal -> FontScale.Normal
        MyNoteFontScale.Large -> FontScale.Large
    }

private const val TAG = "MyNoteNotes"
