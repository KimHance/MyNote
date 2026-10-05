package com.hancekim.mynote.feature.notes.screen.connect

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.hancekim.mynote.core.common.mvi.MviViewModel
import com.hancekim.mynote.core.domain.ConnectRepositoryUseCase
import com.hancekim.mynote.core.domain.VerifyConnectLinkUseCase
import com.hancekim.mynote.core.domain.connect.ConnectProgress
import com.hancekim.mynote.core.domain.connect.ConnectVerification
import com.hancekim.mynote.feature.notes.element.ConfirmUi
import com.hancekim.mynote.feature.notes.element.ConnectIssue
import com.hancekim.mynote.feature.notes.element.ConnectNotice
import com.hancekim.mynote.feature.notes.element.ConnectStep
import com.hancekim.mynote.feature.notes.element.TokenReason
import com.hancekim.mynote.feature.notes.element.canConnect
import com.hancekim.mynote.feature.notes.element.confirmUiOf
import com.hancekim.mynote.feature.notes.element.issueOfProbe
import com.hancekim.mynote.feature.notes.element.issueOfSync
import com.hancekim.mynote.feature.notes.element.linkCheckOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val KEY_LINK = "connect_link"

private const val DONE_RESET_MILLIS = 1_500L

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val verifyConnectLink: VerifyConnectLinkUseCase,
    private val connectRepository: ConnectRepositoryUseCase,
) : MviViewModel<ConnectUiState, ConnectIntent>() {
    private val link = trigger(savedState.get<String>(KEY_LINK).orEmpty())
    private val linkCheck = link.map(::linkCheckOf).triggerIn(linkCheckOf(link.value))
    private val step = trigger(ConnectStep.Editing)
    private val tokenPanel = trigger<TokenPanelUi?>(null)
    private val notice = trigger<ConnectNotice?>(null)
    private val confirm = trigger<ConfirmUi?>(null)

    private var job: Job? = null
    private var resetJob: Job? = null
    private var pending: ConnectVerification.Ready? = null

    override fun build() =
        ConnectUiState(
            link = link.value,
            linkCheck = linkCheck.value,
            step = step.value,
            tokenPanel = tokenPanel.value,
            notice = notice.value,
            confirm = confirm.value,
        )

    override fun onFailure(cause: Throwable) {
        Log.e(TAG, "연결 처리 실패", cause)
        job = null
        if (step.value != ConnectStep.Done) {
            pending = null
            confirm.value = null
            notice.value = ConnectNotice.Transient
            step.value = ConnectStep.Editing
        }
    }

    override fun handleIntent(event: ConnectIntent) {
        when (event) {
            is ConnectIntent.LinkChanged -> {
                if (step.value != ConnectStep.Editing) return
                savedState[KEY_LINK] = event.link
                link.value = event.link
                tokenPanel.value = null
                notice.value = null
            }

            is ConnectIntent.Submit -> {
                submit(event.token?.value)
            }

            ConnectIntent.CancelProbe -> {
                if (step.value != ConnectStep.Probing) return
                job?.cancel()
                job = null
                step.value = ConnectStep.Editing
            }

            ConnectIntent.AcceptConfirm -> {
                val ready = pending ?: return
                if (step.value != ConnectStep.Editing) return
                start(ready)
            }

            ConnectIntent.DismissConfirm -> {
                pending = null
                confirm.value = null
            }
        }
    }

    private fun submit(rawToken: String?) {
        if (step.value != ConnectStep.Editing || !linkCheck.value.canConnect) return
        val candidate = rawToken?.trim()?.ifBlank { null }
        if (tokenPanel.value != null && candidate == null) return
        notice.value = null
        tokenPanel.value = tokenPanel.value?.copy(failure = null)
        step.value = ConnectStep.Probing
        job =
            launchSafely {
                when (val outcome = verifyConnectLink(link.value, candidate)) {
                    is ConnectVerification.Rejected -> {
                        step.value = ConnectStep.Editing
                    }

                    is ConnectVerification.Failed -> {
                        fail(issueOfProbe(outcome.failure, hadToken = candidate != null))
                    }

                    is ConnectVerification.Ready -> {
                        val dialog = confirmUiOf(outcome.replacement)
                        if (dialog == null) {
                            start(outcome)
                        } else {
                            pending = outcome
                            confirm.value = dialog
                            step.value = ConnectStep.Editing
                        }
                    }
                }
            }
    }

    private fun start(ready: ConnectVerification.Ready) {
        pending = null
        confirm.value = null
        step.value = ConnectStep.Syncing
        job =
            launchSafely {
                connectRepository(ready).collect { progress ->
                    when (progress) {
                        ConnectProgress.Syncing -> {
                            step.value = ConnectStep.Syncing
                        }

                        is ConnectProgress.Connected -> {
                            finish()
                        }

                        is ConnectProgress.Failed -> {
                            fail(issueOfSync(progress.failure, hadToken = ready.candidateToken != null))
                        }
                    }
                }
            }
    }

    private fun fail(issue: ConnectIssue) {
        step.value = ConnectStep.Editing
        when (issue) {
            ConnectIssue.NeedsToken -> {
                tokenPanel.value = TokenPanelUi(TokenReason.Needed)
            }

            ConnectIssue.Limit -> {
                tokenPanel.value = TokenPanelUi(TokenReason.Limit)
            }

            is ConnectIssue.TokenField -> {
                val reason = tokenPanel.value?.reason ?: TokenReason.Needed
                tokenPanel.value = TokenPanelUi(reason, issue.kind)
            }

            is ConnectIssue.Notice -> {
                notice.value = issue.kind
            }
        }
    }

    private fun finish() {
        step.value = ConnectStep.Done
        resetJob?.cancel()
        resetJob =
            launchSafely {
                delay(DONE_RESET_MILLIS.milliseconds)
                savedState[KEY_LINK] = ""
                link.value = ""
                tokenPanel.value = null
                notice.value = null
                step.value = ConnectStep.Editing
            }
    }
}

private const val TAG = "MyNoteConnect"
