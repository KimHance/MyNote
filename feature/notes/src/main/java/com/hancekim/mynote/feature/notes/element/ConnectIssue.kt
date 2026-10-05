package com.hancekim.mynote.feature.notes.element

import androidx.annotation.StringRes
import com.hancekim.mynote.core.domain.connect.ConnectProbeFailure
import com.hancekim.mynote.core.domain.model.SyncFailure
import com.hancekim.mynote.feature.notes.R

enum class TokenFailureKind { Rejected, NotVisible, NoList, Limit, Offline, Transient, BadList }

enum class ConnectNotice { NoList, BadList, Offline, Transient }

enum class TokenReason { Needed, Limit }

sealed interface ConnectIssue {
    data object NeedsToken : ConnectIssue

    data object Limit : ConnectIssue

    data class TokenField(
        val kind: TokenFailureKind,
    ) : ConnectIssue

    data class Notice(
        val kind: ConnectNotice,
    ) : ConnectIssue
}

fun issueOfProbe(
    failure: ConnectProbeFailure,
    hadToken: Boolean,
): ConnectIssue =
    when (failure) {
        ConnectProbeFailure.NeedsToken -> {
            if (hadToken) ConnectIssue.TokenField(TokenFailureKind.Rejected) else ConnectIssue.NeedsToken
        }

        ConnectProbeFailure.TokenRejected -> {
            ConnectIssue.TokenField(TokenFailureKind.Rejected)
        }

        ConnectProbeFailure.RepoNotVisible -> {
            if (hadToken) ConnectIssue.TokenField(TokenFailureKind.NotVisible) else ConnectIssue.NeedsToken
        }

        ConnectProbeFailure.ManifestInaccessible -> {
            ConnectIssue.TokenField(TokenFailureKind.NoList)
        }

        ConnectProbeFailure.RateLimited -> {
            ConnectIssue.Limit
        }

        ConnectProbeFailure.NoManifest -> {
            ConnectIssue.Notice(ConnectNotice.NoList)
        }

        ConnectProbeFailure.MalformedManifest -> {
            ConnectIssue.Notice(ConnectNotice.BadList)
        }

        ConnectProbeFailure.Offline -> {
            ConnectIssue.Notice(ConnectNotice.Offline)
        }

        ConnectProbeFailure.Transient -> {
            ConnectIssue.Notice(ConnectNotice.Transient)
        }
    }

fun issueOfSync(
    failure: SyncFailure,
    hadToken: Boolean,
): ConnectIssue =
    when (failure) {
        is SyncFailure.Offline -> {
            ConnectIssue.Notice(ConnectNotice.Offline)
        }

        is SyncFailure.TokenRejected -> {
            ConnectIssue.TokenField(TokenFailureKind.Rejected)
        }

        is SyncFailure.AccessDenied -> {
            if (hadToken) ConnectIssue.TokenField(TokenFailureKind.NotVisible) else ConnectIssue.NeedsToken
        }

        is SyncFailure.RateLimited -> {
            ConnectIssue.Limit
        }

        is SyncFailure.Server -> {
            ConnectIssue.Notice(ConnectNotice.Transient)
        }

        is SyncFailure.Format -> {
            ConnectIssue.Notice(ConnectNotice.BadList)
        }
    }

fun tokenFailureOfProbe(failure: ConnectProbeFailure): TokenFailureKind =
    when (val issue = issueOfProbe(failure, hadToken = true)) {
        is ConnectIssue.TokenField -> {
            issue.kind
        }

        ConnectIssue.Limit -> {
            TokenFailureKind.Limit
        }

        ConnectIssue.NeedsToken -> {
            TokenFailureKind.Rejected
        }

        is ConnectIssue.Notice -> {
            when (issue.kind) {
                ConnectNotice.NoList -> TokenFailureKind.NoList
                ConnectNotice.BadList -> TokenFailureKind.BadList
                ConnectNotice.Offline -> TokenFailureKind.Offline
                ConnectNotice.Transient -> TokenFailureKind.Transient
            }
        }
    }

@StringRes
fun tokenFailureRes(kind: TokenFailureKind): Int =
    when (kind) {
        TokenFailureKind.Rejected -> R.string.notes_token_err_rejected
        TokenFailureKind.NotVisible -> R.string.notes_token_err_not_visible
        TokenFailureKind.NoList -> R.string.notes_token_err_no_list
        TokenFailureKind.Limit -> R.string.notes_token_err_limit_title
        TokenFailureKind.Offline -> R.string.notes_connect_err_offline
        TokenFailureKind.Transient -> R.string.notes_connect_err_transient
        TokenFailureKind.BadList -> R.string.notes_connect_err_bad_list
    }

@StringRes
fun connectNoticeRes(kind: ConnectNotice): Int =
    when (kind) {
        ConnectNotice.NoList -> R.string.notes_connect_err_no_list
        ConnectNotice.BadList -> R.string.notes_connect_err_bad_list
        ConnectNotice.Offline -> R.string.notes_connect_err_offline
        ConnectNotice.Transient -> R.string.notes_connect_err_transient
    }
