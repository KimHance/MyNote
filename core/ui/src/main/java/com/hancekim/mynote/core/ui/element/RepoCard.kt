package com.hancekim.mynote.core.ui.element

import androidx.annotation.StringRes
import com.hancekim.mynote.core.domain.model.RepoConnection
import com.hancekim.mynote.core.domain.model.SyncFailure
import com.hancekim.mynote.core.ui.R

enum class SyncFailureKind { Offline, TokenRejected, AccessDenied, RateLimited, Server, Format }

sealed interface LastSync {
    data object None : LastSync

    data object Succeeded : LastSync

    data class Failed(
        val kind: SyncFailureKind,
    ) : LastSync
}

enum class RepoCardState { Public, TokenInUse, TokenNeedsCheck, None, NoResult }

enum class RepoDot { Ok, Warn, Muted }

data class RepoCardUi(
    val state: RepoCardState,
    val dot: RepoDot,
    @param:StringRes val label: Int,
    @param:StringRes val description: Int?,
    val showBadge: Boolean,
    @param:StringRes val tokenNone: Int?,
)

internal fun syncFailureKindOf(cause: Throwable?): SyncFailureKind =
    when (cause) {
        is SyncFailure.Offline -> SyncFailureKind.Offline
        is SyncFailure.TokenRejected -> SyncFailureKind.TokenRejected
        is SyncFailure.AccessDenied -> SyncFailureKind.AccessDenied
        is SyncFailure.RateLimited -> SyncFailureKind.RateLimited
        is SyncFailure.Format -> SyncFailureKind.Format
        else -> SyncFailureKind.Server
    }

fun lastSyncOf(result: Result<*>): LastSync =
    result.fold(
        onSuccess = { LastSync.Succeeded },
        onFailure = { LastSync.Failed(syncFailureKindOf(it)) },
    )

@StringRes
fun syncFailureKindRes(kind: SyncFailureKind): Int =
    when (kind) {
        SyncFailureKind.Offline -> R.string.ui_repo_kind_offline
        SyncFailureKind.TokenRejected -> R.string.ui_repo_kind_token
        SyncFailureKind.AccessDenied -> R.string.ui_repo_kind_access
        SyncFailureKind.RateLimited -> R.string.ui_repo_kind_limit
        SyncFailureKind.Server -> R.string.ui_repo_kind_server
        SyncFailureKind.Format -> R.string.ui_repo_kind_format
    }

fun repoCardUi(
    connection: RepoConnection?,
    hasToken: Boolean,
    last: LastSync,
): RepoCardUi {
    if (connection == null) {
        return RepoCardUi(
            state = RepoCardState.None,
            dot = RepoDot.Muted,
            label = R.string.ui_repo_state_none,
            description = R.string.ui_repo_state_none_desc,
            showBadge = false,
            tokenNone = null,
        )
    }
    val tokenNone =
        if (hasToken) {
            null
        } else if (connection.isPublic) {
            R.string.ui_repo_token_none_public
        } else {
            R.string.ui_repo_token_none_private
        }
    val rejected = last == LastSync.Failed(SyncFailureKind.TokenRejected)
    val privateDenied = !connection.isPublic && last == LastSync.Failed(SyncFailureKind.AccessDenied)
    val privateWithoutToken = !connection.isPublic && !hasToken
    return when {
        rejected || privateDenied || privateWithoutToken -> {
            RepoCardUi(
                state = RepoCardState.TokenNeedsCheck,
                dot = RepoDot.Warn,
                label = R.string.ui_repo_state_check_token,
                description = R.string.ui_repo_state_check_token_desc,
                showBadge = true,
                tokenNone = tokenNone,
            )
        }

        last == LastSync.None -> {
            RepoCardUi(
                state = RepoCardState.NoResult,
                dot = RepoDot.Muted,
                label = R.string.ui_repo_state_idle,
                description = R.string.ui_repo_state_idle_desc,
                showBadge = false,
                tokenNone = tokenNone,
            )
        }

        hasToken -> {
            RepoCardUi(
                state = RepoCardState.TokenInUse,
                dot = RepoDot.Ok,
                label = R.string.ui_repo_state_token,
                description = null,
                showBadge = false,
                tokenNone = null,
            )
        }

        else -> {
            RepoCardUi(
                state = RepoCardState.Public,
                dot = RepoDot.Ok,
                label = R.string.ui_repo_state_public,
                description = R.string.ui_repo_state_public_desc,
                showBadge = false,
                tokenNone = tokenNone,
            )
        }
    }
}
