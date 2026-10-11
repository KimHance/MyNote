package com.hancekim.mynote.feature.notes

import androidx.lifecycle.viewModelScope
import com.hancekim.mynote.core.common.mvi.MviViewModel
import com.hancekim.mynote.core.common.throttle.Clock
import com.hancekim.mynote.core.common.throttle.WallClock
import com.hancekim.mynote.core.domain.AddBookmarkUseCase
import com.hancekim.mynote.core.domain.GetCategoriesUseCase
import com.hancekim.mynote.core.domain.ObserveBookmarksUseCase
import com.hancekim.mynote.core.domain.ObserveConnectionUseCase
import com.hancekim.mynote.core.domain.ObserveSyncRunsUseCase
import com.hancekim.mynote.core.domain.RecordBookmarkEventUseCase
import com.hancekim.mynote.core.domain.RecordReadSessionUseCase
import com.hancekim.mynote.core.domain.RemoveBookmarksUseCase
import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.core.domain.model.BookmarkEvent
import com.hancekim.mynote.core.domain.model.BookmarkEventKind
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.Moment
import com.hancekim.mynote.core.domain.model.ReadSession
import com.hancekim.mynote.core.domain.model.ReadSessionTracker
import com.hancekim.mynote.core.domain.model.SyncRun
import com.hancekim.mynote.core.navigation.NavCommand
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.navigation.key.activeNoteId
import com.hancekim.mynote.feature.notes.element.CategoryLoad
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesSignal
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.element.NoticeUi
import com.hancekim.mynote.feature.notes.element.ReaderScrollRequest
import com.hancekim.mynote.feature.notes.element.lastSyncOf
import com.hancekim.mynote.feature.notes.element.settledCategories
import com.hancekim.mynote.feature.notes.element.unseen
import com.hancekim.mynote.feature.notes.mapper.bookmarkedNotes
import com.hancekim.mynote.feature.notes.mapper.toUi
import com.hancekim.mynote.feature.notes.navigation.openReaderCommand
import com.hancekim.mynote.feature.notes.navigation.switchTabCommand
import com.hancekim.mynote.feature.notes.signal.FeatureSignals
import com.hancekim.mynote.feature.notes.signal.LastSyncHolder
import com.hancekim.mynote.feature.notes.signal.ReaderOpened
import com.hancekim.mynote.feature.notes.signal.openSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject

private data class SyncState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val errorId: Int = 0,
    val infoMessage: String? = null,
    val infoId: Int = 0,
)

/** 노트 · 북마크 · 동기화 · 읽기 기록. 동기화를 구독하는 곳은 여기 하나다(SY-1 · FM-7). */
@HiltViewModel
class NotesViewModel @Inject constructor(
    getCategories: GetCategoriesUseCase,
    observeSyncRuns: ObserveSyncRunsUseCase,
    observeConnection: ObserveConnectionUseCase,
    observeBookmarks: ObserveBookmarksUseCase,
    private val addBookmark: AddBookmarkUseCase,
    private val removeBookmarks: RemoveBookmarksUseCase,
    private val recordReadSession: RecordReadSessionUseCase,
    private val recordBookmarkEvent: RecordBookmarkEventUseCase,
    private val messages: NotesMessages,
    private val navigator: Navigator,
    private val signals: FeatureSignals,
    private val lastSync: LastSyncHolder,
    private val clock: Clock,
    private val wallClock: WallClock,
    private val failureLog: FailureLog,
) : MviViewModel<NotesUiState, NotesIntent>() {
    private val shownSignals = trigger(persistentMapOf<NotesSignal, Int>())
    private val query = trigger("")
    private val expanded = trigger(persistentMapOf<String, Boolean>())
    private val notice = trigger<NoticeUi<NotesIntent>?>(null)
    private var noticeSeq = 0
    private val refreshTicks = MutableStateFlow(0)

    private val connection =
        observeConnection()
            .catch { emit(ConnectionState.None) }
            .distinctUntilChanged()
            .triggerIn(null, started = SharingStarted.Lazily)

    private val categories: Flow<ImmutableList<CategoryUi>> =
        getCategories()
            .catch { emit(emptyList()) }
            .map { it.toUi() }

    private val categoryLoad =
        settledCategories(connection.filterNotNull()) { categories }
            .triggerIn(CategoryLoad.Initial, started = SharingStarted.Lazily)

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
    private var clearsAtRemoval = 0

    private val readerScrollRequest = trigger<ReaderScrollRequest?>(null)
    private var scrollRequestSeq = 0
    private val tracker = ReadSessionTracker()

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
            .triggerIn(SyncState(), started = SharingStarted.Lazily)

    init {
        navigator.stack
            .map { it.activeNoteId() }
            .distinctUntilChanged()
            .onEach { noteId -> recordSession(tracker.onTop(noteId, categoryOf(noteId), moment())) }
            .launchIn(viewModelScope)
        signals.quizCoversReader
            .onEach { covered -> tracker.onCovered(covered, moment()) }
            .launchIn(viewModelScope)
        val seenOpen = signals.readerOpened.value?.id ?: 0
        signals.readerOpened
            .filterNotNull()
            .filter { it.id > seenOpen }
            .onEach { onReaderOpened(it) }
            .launchIn(viewModelScope)
    }

    override fun build() =
        NotesUiState(
            categories = categoryLoad.value.list,
            visibleCategories = visibleCategories.value,
            recentNotes = recentNotes.value,
            bookmarkedIds = bookmarkedIds.value,
            bookmarks = bookmarks.value,
            query = query.value,
            expanded = expanded.value,
            isLoading = sync.value.isLoading,
            errorMessage = sync.value.errorMessage,
            errorId = shownSignals.value.unseen(NotesSignal.Error, sync.value.errorId),
            connection = connection.value,
            notesLoaded = categoryLoad.value.loaded,
            awaitingNotes = categoryLoad.value.settling,
            notice = notice.value?.takeIf { shownSignals.value.unseen(NotesSignal.Notice, it.id) != 0 },
            infoMessage = sync.value.infoMessage,
            infoId = shownSignals.value.unseen(NotesSignal.Info, sync.value.infoId),
            readerScrollRequest = readerScrollRequest.value,
        )

    override fun onFailure(cause: Throwable) {
        failureLog.failed(TAG, "인텐트 처리 실패", cause)
    }

    override fun handleIntent(event: NotesIntent) {
        when (event) {
            is NotesIntent.SelectNote -> {
                navigator.dispatch(navigator.stack.value.openReaderCommand(event.id))
            }

            is NotesIntent.ForegroundChanged -> {
                recordSession(tracker.onVisible(event.visible, moment()))
            }

            is NotesIntent.ReaderProgress -> {
                tracker.onProgress(event.noteId, event.percent)
            }

            NotesIntent.OpenSettings -> {
                navigator.openSettings(signals)
            }

            NotesIntent.OpenConnect -> {
                if (!signals.repoActionsLocked.value) navigator.push(NotesNavKey.Connect)
            }

            is NotesIntent.QueryChanged -> {
                query.value = event.query
            }

            is NotesIntent.ToggleCategory -> {
                expanded.update { it.putting(event.name, !(it[event.name] ?: false)) }
            }

            is NotesIntent.SignalShown -> {
                shownSignals.update { it.put(event.signal, event.id) }
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
                clearsAtRemoval = signals.studyLogClears.value
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
                // 해제 뒤 학습 기록을 지웠으면 그 이벤트 행은 이미 없다 — 같은 id 의 새 행을 지우지 않는다
                val eventId = removedEventId.takeIf { signals.studyLogClears.value == clearsAtRemoval }
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

            NotesIntent.Refresh -> {
                refreshTicks.update { it + 1 }
            }
        }
    }

    private fun onReaderOpened(opened: ReaderOpened) {
        val resumeAt = opened.resumeAt
        if (resumeAt != null) {
            readerScrollRequest.value = ReaderScrollRequest(opened.noteId, resumeAt, ++scrollRequestSeq)
        }
        if (opened.quiz && isReadable(opened.noteId)) {
            signals.requestQuizSetup(opened.noteId, categoryOf(opened.noteId).orEmpty())
        }
    }

    private fun moment(): Moment = Moment(wallClock.nowMillis(), clock.elapsedMillis())

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

    private fun recordBookmark(
        noteId: String,
        kind: BookmarkEventKind,
        onRecorded: (Long) -> Unit = {},
    ) {
        val event = BookmarkEvent(noteId, categoryOf(noteId).orEmpty(), kind, wallClock.nowMillis())
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

    private fun afterSyncRun(run: SyncRun) {
        if (run !is SyncRun.Finished) return
        lastSync.set(lastSyncOf(run.result))
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
}

private const val TAG = "MyNoteNotes"
