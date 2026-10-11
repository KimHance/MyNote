package com.hancekim.mynote.core.ui.element

import androidx.annotation.StringRes
import com.hancekim.mynote.core.ui.R

enum class TokenFailureKind { Rejected, NotVisible, NoList, Limit, Offline, Transient, BadList }

enum class TokenReason { Needed, Limit }

@StringRes
fun tokenFailureRes(kind: TokenFailureKind): Int =
    when (kind) {
        TokenFailureKind.Rejected -> R.string.ui_token_err_rejected
        TokenFailureKind.NotVisible -> R.string.ui_token_err_not_visible
        TokenFailureKind.NoList -> R.string.ui_token_err_no_list
        TokenFailureKind.Limit -> R.string.ui_token_err_limit_title
        TokenFailureKind.Offline -> R.string.ui_connect_err_offline
        TokenFailureKind.Transient -> R.string.ui_connect_err_transient
        TokenFailureKind.BadList -> R.string.ui_connect_err_bad_list
    }
