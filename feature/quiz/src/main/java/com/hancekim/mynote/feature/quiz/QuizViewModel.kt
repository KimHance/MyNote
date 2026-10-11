package com.hancekim.mynote.feature.quiz

import androidx.lifecycle.viewModelScope
import com.hancekim.mynote.core.common.mvi.MviViewModel
import com.hancekim.mynote.core.common.throttle.WallClock
import com.hancekim.mynote.core.domain.GenerateQuizUseCase
import com.hancekim.mynote.core.domain.ObserveModelStatusUseCase
import com.hancekim.mynote.core.domain.RecordQuizAttemptUseCase
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.model.QuizFailure
import com.hancekim.mynote.core.ui.FailureLog
import com.hancekim.mynote.core.ui.NotesMessages
import com.hancekim.mynote.core.ui.element.NotesSignal
import com.hancekim.mynote.core.ui.element.unseen
import com.hancekim.mynote.core.ui.signal.FeatureSignals
import com.hancekim.mynote.core.ui.signal.QuizSetupRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val UNDO_WINDOW_MILLIS = 3200L

private const val GENERATE_TIMEOUT_MILLIS = 60_000L

private data class QuizUndo(
    val message: String,
    val id: Int,
)

/** 퀴즈 — 리더를 덮는 오버레이의 상태 · 자동 진행 타이머 · 그만두기 되돌리기 · 결과 기록. */
@HiltViewModel
class QuizViewModel @Inject constructor(
    observeModelStatus: ObserveModelStatusUseCase,
    private val generateQuiz: GenerateQuizUseCase,
    private val recordQuizAttempt: RecordQuizAttemptUseCase,
    private val autoAdvancePolicy: QuizAutoAdvancePolicy,
    private val messages: NotesMessages,
    private val signals: FeatureSignals,
    private val wallClock: WallClock,
    private val failureLog: FailureLog,
) : MviViewModel<QuizOverlayState, QuizIntent>() {
    private val quiz = trigger<QuizUiState>(QuizUiState.Idle)
    private val quizUndo = trigger<QuizUndo?>(null)
    private val shownSignals = trigger(persistentMapOf<NotesSignal, Int>())
    private var quizJob: Job? = null
    private var autoAdvanceJob: Job? = null
    private var undoJob: Job? = null
    private var abandonedSolving: QuizUiState.Solving? = null
    private var undoSeq = 0
    private var category = ""
    private var quizMeta: QuizMeta? = null

    private val modelStatus =
        observeModelStatus()
            .catch { emit(ModelStatus.Missing) }
            .triggerIn(ModelStatus.Missing, started = SharingStarted.Lazily)

    init {
        signals.quizCovers(false)
        val seenSetup = signals.quizSetup.value?.id ?: 0
        signals.quizSetup
            .filterNotNull()
            .filter { it.id > seenSetup }
            .onEach { onSetupRequested(it) }
            .launchIn(viewModelScope)
        val seenClose = signals.quizCloses.value
        signals.quizCloses
            .filter { it > seenClose }
            .onEach {
                cancelQuizWork()
                clearUndo()
                setQuiz(QuizUiState.Idle)
            }.launchIn(viewModelScope)
    }

    override fun build() =
        QuizOverlayState(
            quiz = quiz.value,
            quizUndoMessage = quizUndo.value?.message,
            quizUndoId = shownSignals.value.unseen(NotesSignal.QuizUndo, quizUndo.value?.id ?: 0),
            modelStatus = modelStatus.value,
        )

    override fun onFailure(cause: Throwable) {
        failureLog.failed(TAG, "인텐트 처리 실패", cause)
    }

    override fun onCleared() {
        signals.quizCovers(false)
    }

    override fun handleIntent(event: QuizIntent) {
        when (event) {
            is QuizIntent.OpenSetup -> {
                openSetup(event.noteId, event.category)
            }

            is QuizIntent.SetCount -> {
                val setup = quiz.value as? QuizUiState.Setup ?: return
                setQuiz(setup.copy(maxCount = event.count))
            }

            is QuizIntent.Generate -> {
                startQuiz(event.noteId, event.maxCount)
            }

            QuizIntent.Retry -> {
                val failed = quiz.value as? QuizUiState.Failed ?: return
                startQuiz(failed.noteId, failed.maxCount)
            }

            is QuizIntent.RetryWithFewer -> {
                val failed = quiz.value as? QuizUiState.Failed ?: return
                startQuiz(failed.noteId, event.count)
            }

            is QuizIntent.PickChoice -> {
                val solving = quiz.value as? QuizUiState.Solving ?: return
                val graded = solving.pick(event.index)
                if (graded === solving) return

                val auto = graded.isCorrect && autoAdvancePolicy.isEnabled()
                setQuiz(graded.copy(autoAdvancing = auto))
                if (auto) armAutoAdvance(graded.index)
            }

            is QuizIntent.NextQuestion -> {
                autoAdvanceJob?.cancel()
                val solving = quiz.value as? QuizUiState.Solving ?: return
                if (event.fromIndex != null && event.fromIndex != solving.index) return
                val next = solving.advance()
                setQuiz(next)
                recordAttempt(solving, next)
            }

            QuizIntent.Dismiss -> {
                dismissQuiz()
            }

            QuizIntent.UndoExit -> {
                val restored = abandonedSolving ?: return
                clearUndo()
                setQuiz(restored)
                if (restored.autoAdvancing) armAutoAdvance(restored.index)
            }

            is QuizIntent.SignalShown -> {
                shownSignals.update { it.put(event.signal, event.id) }
            }
        }
    }

    private fun onSetupRequested(request: QuizSetupRequest) {
        if (modelStatus.value == ModelStatus.Ready) openSetup(request.noteId, request.category)
    }

    private fun openSetup(
        noteId: String,
        category: String,
    ) {
        cancelQuizWork()
        clearUndo()
        this.category = category
        setQuiz(QuizUiState.Setup(noteId))
    }

    /** 퀴즈 상태는 여기서만 바꾼다 — 리더를 덮는지를 같은 자리에서 계약에 써서 읽기 세션이 같은 틱에 멈춘다(FM-6 ⑦). */
    private fun setQuiz(next: QuizUiState) {
        quiz.value = next
        signals.quizCovers(next.coversReader)
    }

    private fun startQuiz(
        noteId: String,
        maxCount: Int,
    ) {
        if (quizJob?.isActive == true) return
        autoAdvanceJob?.cancel()
        clearUndo()
        setQuiz(QuizUiState.Loading(noteId, maxCount))
        quizJob =
            launchSafely {
                val outcome = withTimeoutOrNull(GENERATE_TIMEOUT_MILLIS.milliseconds) { generateQuiz(noteId, maxCount) }
                val next =
                    outcome?.fold(
                        onSuccess = { questions -> solvingOrEmpty(questions.map { it.toUi() }, noteId, maxCount) },
                        onFailure = { QuizUiState.Failed(it.toFailKind(), maxCount, noteId) },
                    ) ?: QuizUiState.Failed(QuizUiState.Failed.Kind.Timeout, maxCount, noteId)
                if (next is QuizUiState.Solving) quizMeta = QuizMeta(noteId, wallClock.nowMillis())
                setQuiz(next)
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
                onIntent(QuizIntent.NextQuestion(fromIndex = fromIndex))
            }
    }

    private fun dismissQuiz() {
        val leaving = quiz.value
        cancelQuizWork()
        setQuiz(QuizUiState.Idle)

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

    private fun clearUndo() {
        undoJob?.cancel()
        abandonedSolving = null
        if (quizUndo.value != null) quizUndo.value = null
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
                category = category,
                finishedAt = wallClock.nowMillis(),
            ) ?: return
        launchSafely { withContext(NonCancellable) { recordQuizAttempt(attempt) } }
    }

    private fun Throwable.toFailKind(): QuizUiState.Failed.Kind =
        when (this) {
            is QuizFailure.Parse -> QuizUiState.Failed.Kind.Parse
            is QuizFailure.Empty -> QuizUiState.Failed.Kind.Empty
            else -> QuizUiState.Failed.Kind.Engine
        }
}

private const val TAG = "MyNoteNotes"
