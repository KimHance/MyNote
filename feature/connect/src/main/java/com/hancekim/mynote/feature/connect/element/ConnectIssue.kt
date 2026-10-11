package com.hancekim.mynote.feature.connect.element

import androidx.annotation.StringRes
import com.hancekim.mynote.core.domain.connect.ConnectProbeFailure
import com.hancekim.mynote.core.domain.model.SyncFailure
import com.hancekim.mynote.core.ui.element.TokenFailureKind
import com.hancekim.mynote.feature.connect.R
import com.hancekim.mynote.core.ui.R as UiR

enum class ConnectNotice { NoList, BadList, Offline, Transient }

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

@StringRes
fun connectNoticeRes(kind: ConnectNotice): Int =
    when (kind) {
        ConnectNotice.NoList -> R.string.connect_err_no_list
        ConnectNotice.BadList -> UiR.string.ui_connect_err_bad_list
        ConnectNotice.Offline -> UiR.string.ui_connect_err_offline
        ConnectNotice.Transient -> UiR.string.ui_connect_err_transient
    }
