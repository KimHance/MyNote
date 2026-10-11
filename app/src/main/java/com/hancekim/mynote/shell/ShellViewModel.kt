package com.hancekim.mynote.shell

import androidx.lifecycle.viewModelScope
import com.hancekim.mynote.core.common.mvi.MviViewModel
import com.hancekim.mynote.core.common.throttle.Clock
import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale
import com.hancekim.mynote.core.domain.ConsumeSeedUseCase
import com.hancekim.mynote.core.domain.ObserveConnectionUseCase
import com.hancekim.mynote.core.domain.ObserveFontScaleUseCase
import com.hancekim.mynote.core.domain.ObserveModelStatusUseCase
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.navigation.NavCommand
import com.hancekim.mynote.core.navigation.NavResult
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.navigation.key.activeNoteId
import com.hancekim.mynote.core.navigation.switchTabCommand
import com.hancekim.mynote.core.ui.FailureLog
import com.hancekim.mynote.core.ui.element.LastSync
import com.hancekim.mynote.core.ui.element.NotesSignal
import com.hancekim.mynote.core.ui.element.toUi
import com.hancekim.mynote.core.ui.element.unseen
import com.hancekim.mynote.core.ui.signal.FeatureSignals
import com.hancekim.mynote.core.ui.signal.LastSyncHolder
import com.hancekim.mynote.core.ui.signal.openSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val EXIT_WINDOW_MILLIS = 1000L

/** 셸 — 백스택 파생 · 두 번 눌러 종료 · 시작 분기 · 연결이 바뀔 때 화면 정리(FM-6 ⑨). 노트 목록 · 퀴즈 · 토큰은 들지 않는다. */
@HiltViewModel
class ShellViewModel @Inject constructor(
    observeConnection: ObserveConnectionUseCase,
    private val consumeSeed: ConsumeSeedUseCase,
    observeFontScale: ObserveFontScaleUseCase,
    observeModelStatus: ObserveModelStatusUseCase,
    private val navigator: Navigator,
    private val signals: FeatureSignals,
    private val lastSync: LastSyncHolder,
    private val clock: Clock,
    private val failureLog: FailureLog,
) : MviViewModel<ShellUiState, ShellIntent>() {
    private val navStack = navigator.stack.triggerIn(navigator.stack.value)
    private val navSettled = navigator.isSettled.triggerIn(true)
    private val exitHints = trigger(0)
    private val exitRequests = trigger(0)
    private val shownSignals = trigger(persistentMapOf<NotesSignal, Int>())
    private var lastRootBackMillis: Long? = null
    private val connection = trigger<ConnectionState?>(null)
    private var previousConnection: ConnectionState? = null
    private val seedPending = trigger(true)
    private val fontScale =
        observeFontScale()
            .map { it.toUi() }
            .catch { emit(MyNoteFontScale.Normal) }
            .triggerIn(MyNoteFontScale.Normal, started = SharingStarted.Lazily)
    private val modelStatus =
        observeModelStatus()
            .catch { emit(ModelStatus.Missing) }
            .triggerIn(ModelStatus.Missing, started = SharingStarted.Lazily)

    init {
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
        ShellUiState(
            backStack = navStack.value.toImmutableList(),
            activeNoteId = navStack.value.activeNoteId(),
            isNavSettled = navSettled.value,
            exitHintId = shownSignals.value.unseen(NotesSignal.ExitHint, exitHints.value),
            exitRequestId = exitRequests.value,
            connection = connection.value,
            seedPending = seedPending.value,
            fontScale = fontScale.value,
            modelStatus = modelStatus.value,
        )

    override fun onFailure(cause: Throwable) {
        failureLog.failed(TAG, "인텐트 처리 실패", cause)
    }

    override fun handleIntent(event: ShellIntent) {
        when (event) {
            ShellIntent.Back -> {
                if (navigator.pop() == NavResult.Ignored) {
                    val now = clock.elapsedMillis()
                    val last = lastRootBackMillis
                    if (last != null && now - last < EXIT_WINDOW_MILLIS) {
                        exitRequests.update { it + 1 }
                    } else {
                        lastRootBackMillis = now
                        exitHints.update { it + 1 }
                    }
                }
            }

            is ShellIntent.SwitchTab -> {
                navigator.dispatch(navigator.stack.value.switchTabCommand(event.tab))
            }

            ShellIntent.OpenSettings -> {
                navigator.openSettings(signals)
            }

            is ShellIntent.SignalShown -> {
                shownSignals.update { it.put(event.signal, event.id) }
            }
        }
    }

    /** 스택 · 퀴즈 · 마지막 동기화를 한 호출 안에서 같이 정리한다 — 셋이 같은 틱에 바뀐다. */
    private fun onConnectionChanged(next: ConnectionState) {
        val cleanup = screenCleanupOf(previousConnection, next)
        previousConnection = next
        if (cleanup != null) {
            if (cleanup.resetStack) {
                navigator.dispatchImmediately(NavCommand.ReplaceAll(listOf(NotesNavKey.Home)))
            }
            if (cleanup.dismissQuiz) signals.closeQuiz()
            lastSync.set(if (next is ConnectionState.Connected) LastSync.Succeeded else LastSync.None)
        }
        connection.value = next
    }
}

private const val TAG = "MyNoteNotes"
