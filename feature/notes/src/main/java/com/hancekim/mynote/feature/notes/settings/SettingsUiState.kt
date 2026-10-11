package com.hancekim.mynote.feature.notes.settings

import androidx.compose.runtime.Stable
import com.hancekim.mynote.core.common.mvi.MviEvent
import com.hancekim.mynote.core.common.mvi.MviState
import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale
import com.hancekim.mynote.core.domain.model.ConnectionState
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.feature.notes.element.DisconnectDialogUi
import com.hancekim.mynote.feature.notes.element.LastSync
import com.hancekim.mynote.feature.notes.element.NotesSignal
import com.hancekim.mynote.feature.notes.element.NoticeUi
import com.hancekim.mynote.feature.notes.element.RepoCardUi
import com.hancekim.mynote.feature.notes.element.SecretText
import com.hancekim.mynote.feature.notes.element.TokenEditState
import com.hancekim.mynote.feature.notes.element.repoCardUi

@Stable
data class SettingsUiState(
    val hasToken: Boolean = false,
    val maskedToken: String? = null,
    val tokenEdit: TokenEditState = TokenEditState.Viewing,
    val connection: ConnectionState? = null,
    val lastSync: LastSync = LastSync.None,
    val disconnectDialog: DisconnectDialogUi? = null,
    val fontScale: MyNoteFontScale = MyNoteFontScale.Normal,
    val studyLogEnabled: Boolean = true,
    val modelStatus: ModelStatus = ModelStatus.Missing,
    val notificationsEnabled: Boolean = true,
    val notice: NoticeUi<SettingsIntent>? = null,
) : MviState {
    val connectedRepo: RepoConnection? get() = (connection as? ConnectionState.Connected)?.connection

    val tokenEditing: Boolean get() = tokenEdit.editing

    val repoCard: RepoCardUi get() = repoCardUi(connectedRepo, hasToken, lastSync)
}

sealed interface SettingsIntent : MviEvent {
    data class SetFontScale(
        val scale: MyNoteFontScale,
    ) : SettingsIntent

    data class SetStudyLogEnabled(
        val enabled: Boolean,
    ) : SettingsIntent

    data object ClearStudyLog : SettingsIntent

    data object StartModelDownload : SettingsIntent

    data object CancelModelDownload : SettingsIntent

    data object DiscardPartialModel : SettingsIntent

    data object DeleteModel : SettingsIntent

    data object StartTokenEdit : SettingsIntent

    data object CancelTokenEdit : SettingsIntent

    data class SaveToken(
        val token: SecretText,
    ) : SettingsIntent

    data object ClearToken : SettingsIntent

    data object UndoClearToken : SettingsIntent

    data object OpenConnect : SettingsIntent

    data object RequestDisconnect : SettingsIntent

    data object ConfirmDisconnect : SettingsIntent

    data object DismissDisconnect : SettingsIntent

    data class SignalShown(
        val signal: NotesSignal,
        val id: Int,
    ) : SettingsIntent
}
