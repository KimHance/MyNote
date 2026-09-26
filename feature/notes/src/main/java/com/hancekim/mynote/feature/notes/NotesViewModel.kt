package com.hancekim.mynote.feature.notes

import android.os.SystemClock
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.viewModelScope
import com.hancekim.mynote.core.common.mvi.MviViewModel
import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale
import com.hancekim.mynote.core.domain.GenerateQuizUseCase
import com.hancekim.mynote.core.domain.GetCategoriesUseCase
import com.hancekim.mynote.core.domain.ObserveAppliedTokenUseCase
import com.hancekim.mynote.core.domain.SaveTokenUseCase
import com.hancekim.mynote.core.domain.SyncNotesUseCase
import com.hancekim.mynote.core.domain.model.QuizFailure
import com.hancekim.mynote.core.domain.repository.QuizGenerator
import com.hancekim.mynote.core.navigation.NavCommand
import com.hancekim.mynote.core.navigation.NavResult
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.navigation.key.activeNoteId
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.mapper.toUi
import com.hancekim.mynote.feature.notes.quiz.QuizAutoAdvancePolicy
import com.hancekim.mynote.feature.notes.quiz.QuizUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val EXIT_WINDOW_MILLIS = 1000L

private const val UNDO_WINDOW_MILLIS = 3200L

private const val GENERATE_TIMEOUT_MILLIS = 60_000L

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
    private val syncNotes: SyncNotesUseCase,
    private val saveToken: SaveTokenUseCase,
    observeAppliedToken: ObserveAppliedTokenUseCase,
    private val generateQuiz: GenerateQuizUseCase,
    private val autoAdvancePolicy: QuizAutoAdvancePolicy,
    private val messages: NotesMessages,
    quizGenerator: QuizGenerator,
    private val navigator: Navigator,
) : MviViewModel<NotesUiState, NotesIntent>() {
    private val navStack = navigator.stack.triggerIn(navigator.stack.value)
    private val navSettled = navigator.isSettled.triggerIn(true)
    private val quiz = trigger<QuizUiState>(QuizUiState.Idle)

    private val quizAvailable = flow { emit(quizGenerator.isAvailable()) }.triggerIn(false)
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
    private val fontScale = trigger(MyNoteFontScale.Normal)
    private val refreshTicks = MutableStateFlow(0)

    private val appliedToken =
        observeAppliedToken()
            .triggerIn(null, started = SharingStarted.Lazily)

    private val categories: Flow<ImmutableList<CategoryUi>> =
        getCategories()
            .catch { emit(emptyList()) }
            .map { it.toUi() }

    private val categoryList =
        categories
            .triggerIn(persistentListOf(), started = SharingStarted.Lazily)

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
        combine(appliedToken, refreshTicks) { _, tick -> tick }
            .flatMapLatest { tick -> syncFlow(announce = tick > 0) }
            .triggerIn(SyncState())

    private val snackbarHostState = SnackbarHostState()

    override fun build() =
        NotesUiState(
            categories = categoryList.value,
            visibleCategories = visibleCategories.value,
            recentNotes = recentNotes.value,
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
            infoMessage = sync.value.infoMessage,
            infoId = sync.value.infoId,
            isQuizAvailable = quizAvailable.value,
            quiz = quiz.value,
            quizUndoMessage = quizUndo.value?.message,
            quizUndoId = quizUndo.value?.id ?: 0,
            snackbarHostState = snackbarHostState,
        )

    override fun onIntent(event: NotesIntent) {
        when (event) {
            is NotesIntent.SelectNote -> {
                navigator.push(NotesNavKey.Reader(event.id))
            }

            is NotesIntent.QueryChanged -> {
                query.value = event.query
            }

            is NotesIntent.ToggleCategory -> {
                expanded.update { it.putting(event.name, !(it[event.name] ?: false)) }
            }

            NotesIntent.ToggleFontScale -> {
                fontScale.update {
                    if (it == MyNoteFontScale.Normal) MyNoteFontScale.Large else MyNoteFontScale.Normal
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

            NotesIntent.CloseNote -> {
                navigator.pop()
            }

            is NotesIntent.SaveToken -> {
                viewModelScope.launch { saveToken(event.token) }
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
                quiz.value = solving.advance()
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
            viewModelScope.launch {
                val outcome = withTimeoutOrNull(GENERATE_TIMEOUT_MILLIS.milliseconds) { generateQuiz(noteId, maxCount) }
                quiz.value =
                    outcome?.fold(
                        onSuccess = { questions -> solvingOrEmpty(questions.map { it.toUi() }, noteId, maxCount) },
                        onFailure = { QuizUiState.Failed(it.toFailKind(), maxCount, noteId) },
                    ) ?: QuizUiState.Failed(QuizUiState.Failed.Kind.Timeout, maxCount, noteId)
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
            viewModelScope.launch {
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
            viewModelScope.launch {
                delay(UNDO_WINDOW_MILLIS.milliseconds)
                abandonedSolving = null
                quizUndo.value = null
            }
    }

    private fun cancelQuizWork() {
        autoAdvanceJob?.cancel()
        quizJob?.cancel()
    }

    private fun clearUndo() {
        undoJob?.cancel()
        abandonedSolving = null
        if (quizUndo.value != null) quizUndo.value = null
    }

    private fun syncFlow(announce: Boolean): Flow<SyncState> =
        flow {
            emit(SyncState(isLoading = true))
            emit(
                syncNotes().fold(
                    onSuccess = { outcome ->
                        val openNote = navigator.stack.value.activeNoteId()
                        if (openNote != null && openNote in outcome.deleted) {
                            navigator.dispatchImmediately(NavCommand.Pop)
                        }
                        if (announce && outcome.hasChanges) {
                            SyncState(
                                isLoading = false,
                                infoMessage = messages.syncApplied(outcome),
                                infoId = ++infoSeq,
                            )
                        } else {
                            SyncState(isLoading = false)
                        }
                    },
                    onFailure = {
                        SyncState(
                            isLoading = false,
                            errorMessage = messages.syncFailed(),
                            errorId = ++errorSeq,
                        )
                    },
                ),
            )
        }

    private fun Throwable.toFailKind(): QuizUiState.Failed.Kind =
        when (this) {
            is QuizFailure.Parse -> QuizUiState.Failed.Kind.Parse
            is QuizFailure.Empty -> QuizUiState.Failed.Kind.Empty
            else -> QuizUiState.Failed.Kind.Engine
        }
}
