package com.hancekim.mynote.core.domain.connect

import com.hancekim.mynote.core.domain.model.ErasureCount
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SyncFailure
import com.hancekim.mynote.core.domain.model.SyncOutcome

sealed interface ConnectReplacement {
    data object None : ConnectReplacement

    data class Erases(
        val count: ErasureCount,
        val dropsToken: Boolean,
        val adopts: Boolean,
    ) : ConnectReplacement
}

sealed interface ConnectVerification {
    data class Ready(
        val connection: RepoConnection,
        val candidateToken: String?,
        val replacement: ConnectReplacement,
    ) : ConnectVerification

    data class Rejected(
        val reason: ConnectLinkRejection,
    ) : ConnectVerification

    data class Failed(
        val failure: ConnectProbeFailure,
    ) : ConnectVerification
}

sealed interface ConnectProgress {
    data object Syncing : ConnectProgress

    data class Connected(
        val outcome: SyncOutcome,
    ) : ConnectProgress

    data class Failed(
        val failure: SyncFailure,
    ) : ConnectProgress

    data class ConfirmFailed(
        val cause: Throwable,
    ) : ConnectProgress
}
