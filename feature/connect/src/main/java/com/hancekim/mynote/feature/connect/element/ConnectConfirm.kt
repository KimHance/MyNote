package com.hancekim.mynote.feature.connect.element

import androidx.compose.runtime.Immutable
import com.hancekim.mynote.core.domain.connect.ConnectReplacement

enum class ConfirmKind { Switch, Adopt }

@Immutable
data class ConfirmUi(
    val kind: ConfirmKind,
    val notes: Int,
    val bookmarks: Int,
    val dropsToken: Boolean,
)

fun confirmUiOf(replacement: ConnectReplacement): ConfirmUi? =
    when (replacement) {
        ConnectReplacement.None -> {
            null
        }

        is ConnectReplacement.Erases -> {
            ConfirmUi(
                kind = if (replacement.adopts) ConfirmKind.Adopt else ConfirmKind.Switch,
                notes = replacement.count.notes,
                bookmarks = replacement.count.bookmarks,
                dropsToken = replacement.dropsToken && !replacement.adopts,
            )
        }
    }
