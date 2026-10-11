package com.hancekim.mynote.feature.connect.screen

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.hancekim.mynote.core.common.mvi.MviEvent
import com.hancekim.mynote.core.common.mvi.MviState
import com.hancekim.mynote.core.domain.connect.ConnectLinkRejection
import com.hancekim.mynote.core.domain.connect.TemplateRepo
import com.hancekim.mynote.core.ui.element.SecretText
import com.hancekim.mynote.core.ui.element.TokenFailureKind
import com.hancekim.mynote.core.ui.element.TokenReason
import com.hancekim.mynote.feature.connect.element.ConfirmUi
import com.hancekim.mynote.feature.connect.element.ConnectNotice
import com.hancekim.mynote.feature.connect.element.ConnectStep
import com.hancekim.mynote.feature.connect.element.LinkCheck
import com.hancekim.mynote.feature.connect.element.entryVisible

@Immutable
data class TokenPanelUi(
    val reason: TokenReason,
    val failure: TokenFailureKind? = null,
)

@Stable
data class ConnectUiState(
    val link: String = "",
    val linkCheck: LinkCheck = LinkCheck.Rejected(ConnectLinkRejection.Empty),
    val step: ConnectStep = ConnectStep.Editing,
    val tokenPanel: TokenPanelUi? = null,
    val notice: ConnectNotice? = null,
    val confirm: ConfirmUi? = null,
    val resetCount: Int = 0,
    val template: TemplateRepo? = null,
) : MviState

val ConnectUiState.entryShown: Boolean
    get() = entryVisible(step, tokenPanel != null, notice, template != null)

sealed interface ConnectIntent : MviEvent {
    data class LinkChanged(
        val link: String,
    ) : ConnectIntent

    data class Submit(
        val token: SecretText?,
    ) : ConnectIntent

    data object CancelProbe : ConnectIntent

    data object AcceptConfirm : ConnectIntent

    data object DismissConfirm : ConnectIntent
}
