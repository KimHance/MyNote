package com.hancekim.mynote.feature.notes.settings

import androidx.lifecycle.viewModelScope
import com.hancekim.mynote.core.common.mvi.MviViewModel
import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale
import com.hancekim.mynote.core.domain.CancelModelDownloadUseCase
import com.hancekim.mynote.core.domain.ClearStudyLogUseCase
import com.hancekim.mynote.core.domain.CountErasableUseCase
import com.hancekim.mynote.core.domain.DeleteModelUseCase
import com.hancekim.mynote.core.domain.DiscardPartialModelUseCase
import com.hancekim.mynote.core.domain.DisconnectUseCase
import com.hancekim.mynote.core.domain.ObserveAppliedTokenUseCase
import com.hancekim.mynote.core.domain.ObserveConnectionUseCase
import com.hancekim.mynote.core.domain.ObserveFontScaleUseCase
import com.hancekim.mynote.core.domain.ObserveModelStatusUseCase
import com.hancekim.mynote.core.domain.ObserveStudyLogEnabledUseCase
import com.hancekim.mynote.core.domain.SaveTokenUseCase
import com.hancekim.mynote.core.domain.SetFontScaleUseCase
import com.hancekim.mynote.core.domain.SetStudyLogEnabledUseCase
import com.hancekim.mynote.core.domain.StartModelDownloadUseCase
import com.hancekim.mynote.core.domain.VerifyConnectLinkUseCase
import com.hancekim.mynote.core.domain.connect.ConnectVerification
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.navigation.Navigator
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.core.ui.FailureLog
import com.hancekim.mynote.core.ui.NotesMessages
import com.hancekim.mynote.core.ui.element.NotesSignal
import com.hancekim.mynote.core.ui.element.NoticeUi
import com.hancekim.mynote.core.ui.element.TokenFailureKind
import com.hancekim.mynote.core.ui.element.toDomain
import com.hancekim.mynote.core.ui.element.toUi
import com.hancekim.mynote.core.ui.element.unseen
import com.hancekim.mynote.core.ui.signal.FeatureSignals
import com.hancekim.mynote.core.ui.signal.LastSyncHolder
import com.hancekim.mynote.feature.notes.element.DisconnectDialogUi
import com.hancekim.mynote.feature.notes.element.TokenEditEffect
import com.hancekim.mynote.feature.notes.element.TokenEditEvent
import com.hancekim.mynote.feature.notes.element.TokenEditState
import com.hancekim.mynote.feature.notes.element.connectionLinkOf
import com.hancekim.mynote.feature.notes.element.maskToken
import com.hancekim.mynote.feature.notes.element.reduceTokenEdit
import com.hancekim.mynote.feature.notes.element.tokenFailureOfProbe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 설정 — 토큰 편집 · 글자 크기 · 학습 기록 · 모델 받기 조작 · 끊기. 마지막 동기화는 홀더에서 읽기만 한다(FM-7). */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeConnection: ObserveConnectionUseCase,
    observeAppliedToken: ObserveAppliedTokenUseCase,
    observeFontScale: ObserveFontScaleUseCase,
    observeStudyLogEnabled: ObserveStudyLogEnabledUseCase,
    observeModelStatus: ObserveModelStatusUseCase,
    private val saveToken: SaveTokenUseCase,
    private val countErasable: CountErasableUseCase,
    private val disconnect: DisconnectUseCase,
    private val verifyConnectLink: VerifyConnectLinkUseCase,
    private val setFontScale: SetFontScaleUseCase,
    private val setStudyLogEnabled: SetStudyLogEnabledUseCase,
    private val clearStudyLog: ClearStudyLogUseCase,
    private val startModelDownload: StartModelDownloadUseCase,
    private val cancelModelDownload: CancelModelDownloadUseCase,
    private val discardPartialModel: DiscardPartialModelUseCase,
    private val deleteModel: DeleteModelUseCase,
    private val messages: NotesMessages,
    private val navigator: Navigator,
    private val signals: FeatureSignals,
    lastSync: LastSyncHolder,
    private val failureLog: FailureLog,
) : MviViewModel<SettingsUiState, SettingsIntent>() {
    private val shownSignals = trigger(persistentMapOf<NotesSignal, Int>())
    private val tokenEdit = trigger(TokenEditState.Viewing)
    private var verifyJob: Job? = null
    private val disconnectDialog = trigger<DisconnectDialogUi?>(null)
    private val notice = trigger<NoticeUi<SettingsIntent>?>(null)
    private var clearedToken: String? = null
    private var noticeSeq = 0

    private val connection =
        observeConnection()
            .catch { emit(ConnectionState.None) }
            .distinctUntilChanged()
            .triggerIn(null, started = SharingStarted.Lazily)
    private val appliedToken =
        observeAppliedToken()
            .triggerIn(null, started = SharingStarted.Lazily)
    private val fontScale =
        observeFontScale()
            .map { it.toUi() }
            .catch { emit(MyNoteFontScale.Normal) }
            .triggerIn(MyNoteFontScale.Normal, started = SharingStarted.Lazily)
    private val studyLogEnabled =
        observeStudyLogEnabled()
            .catch { emit(true) }
            .triggerIn(true, started = SharingStarted.Lazily)
    private val modelStatus =
        observeModelStatus()
            .catch { emit(ModelStatus.Missing) }
            .triggerIn(ModelStatus.Missing, started = SharingStarted.Lazily)
    private val last = lastSync.value.triggerIn(lastSync.value.value)

    init {
        signals.lockRepoActions(false)
        signals.settingsEntries
            .drop(1)
            .onEach { applyTokenEdit(TokenEditEvent.EnterSettings) }
            .launchIn(viewModelScope)
    }

    override fun build() =
        SettingsUiState(
            hasToken = !appliedToken.value.isNullOrBlank(),
            maskedToken = maskToken(appliedToken.value),
            tokenEdit = tokenEdit.value,
            connection = connection.value,
            lastSync = last.value,
            disconnectDialog = disconnectDialog.value,
            fontScale = fontScale.value,
            studyLogEnabled = studyLogEnabled.value,
            modelStatus = modelStatus.value,
            notice = notice.value?.takeIf { shownSignals.value.unseen(NotesSignal.Notice, it.id) != 0 },
        )

    override fun onFailure(cause: Throwable) {
        failureLog.failed(TAG, "인텐트 처리 실패", cause)
    }

    override fun handleIntent(event: SettingsIntent) {
        when (event) {
            is SettingsIntent.SetFontScale -> {
                launchSafely { setFontScale(event.scale.toDomain()) }
            }

            is SettingsIntent.SetStudyLogEnabled -> {
                launchSafely { setStudyLogEnabled(event.enabled) }
            }

            SettingsIntent.ClearStudyLog -> {
                signals.studyLogCleared()
                launchSafely {
                    clearStudyLog()
                    announce(messages.studyLogCleared())
                }
            }

            SettingsIntent.StartModelDownload -> {
                startModelDownload()
            }

            SettingsIntent.CancelModelDownload -> {
                val received = (modelStatus.value as? ModelStatus.Downloading)?.received ?: 0L
                cancelModelDownload()
                announce(if (received > 0L) messages.downloadPaused() else messages.downloadCancelled())
            }

            SettingsIntent.DiscardPartialModel -> {
                discardPartialModel()
                announce(messages.partialDiscarded())
            }

            SettingsIntent.DeleteModel -> {
                launchSafely {
                    deleteModel()
                    announce(messages.modelDeleted())
                }
            }

            SettingsIntent.StartTokenEdit -> {
                applyTokenEdit(TokenEditEvent.Start)
            }

            SettingsIntent.CancelTokenEdit -> {
                applyTokenEdit(TokenEditEvent.Dismiss)
            }

            is SettingsIntent.SaveToken -> {
                val token = event.token.value.trim()
                applyTokenEdit(TokenEditEvent.Save(blank = token.isEmpty()), token)
            }

            SettingsIntent.ClearToken -> {
                applyTokenEdit(TokenEditEvent.Clear(hasToken = !appliedToken.value.isNullOrBlank()))
            }

            SettingsIntent.UndoClearToken -> {
                applyTokenEdit(TokenEditEvent.Undo)
            }

            SettingsIntent.OpenConnect -> {
                if (!applyTokenEdit(TokenEditEvent.ChangeRepository)) navigator.push(NotesNavKey.Connect)
            }

            SettingsIntent.RequestDisconnect -> {
                if (applyTokenEdit(TokenEditEvent.Disconnect)) return
                launchSafely {
                    val count = countErasable()
                    disconnectDialog.value = DisconnectDialogUi(count.notes, count.bookmarks)
                }
            }

            SettingsIntent.ConfirmDisconnect -> {
                disconnectDialog.value = null
                launchSafely {
                    disconnect().onFailure {
                        failureLog.failed(TAG, "연결 끊기 실패", it)
                        announce(messages.disconnectFailed())
                    }
                }
            }

            SettingsIntent.DismissDisconnect -> {
                disconnectDialog.value = null
            }

            is SettingsIntent.SignalShown -> {
                shownSignals.update { it.put(event.signal, event.id) }
            }
        }
    }

    private fun announce(
        message: String,
        undo: SettingsIntent? = null,
    ) {
        notice.value = NoticeUi(message, undo = undo, id = ++noticeSeq)
    }

    /** 토큰 편집 전이. 저장소 행동 잠금을 같은 자리에서 계약에 쓴다 — 노트가 연결 화면을 열기 전에 읽는다(FM-6 ③). */
    private fun applyTokenEdit(
        event: TokenEditEvent,
        token: String? = null,
    ): Boolean {
        val transition = reduceTokenEdit(tokenEdit.value, event)
        tokenEdit.value = transition.next
        signals.lockRepoActions(transition.next.locksRepoActions)
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
                    failureLog.failed(TAG, "토큰 확인 실패", failure)
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
            announce(messages.tokenCleared(), undo = SettingsIntent.UndoClearToken)
        }
    }

    private fun restoreStoredToken() {
        val previous = clearedToken ?: return
        clearedToken = null
        launchSafely { saveToken(previous) }
    }
}

private const val TAG = "MyNoteNotes"
