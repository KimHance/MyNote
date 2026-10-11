package com.hancekim.mynote.feature.notes.element

import com.hancekim.mynote.core.ui.element.TokenFailureKind

data class TokenEditState(
    val editing: Boolean = false,
    val verifying: Boolean = false,
    val failure: TokenFailureKind? = null,
    val undoable: Boolean = false,
) {
    val locksRepoActions: Boolean get() = verifying

    companion object {
        val Viewing = TokenEditState()
    }
}

sealed interface TokenEditEvent {
    data object Start : TokenEditEvent

    data class Save(
        val blank: Boolean,
    ) : TokenEditEvent

    data object VerifySucceeded : TokenEditEvent

    data class VerifyFailed(
        val kind: TokenFailureKind,
    ) : TokenEditEvent

    data object Dismiss : TokenEditEvent

    data object EnterSettings : TokenEditEvent

    data class Clear(
        val hasToken: Boolean,
    ) : TokenEditEvent

    data object Undo : TokenEditEvent

    data object ChangeRepository : TokenEditEvent

    data object Disconnect : TokenEditEvent
}

enum class TokenEditEffect { Verify, CancelRequest, DiscardInput, ClearStored, RestoreStored, Saved, Blocked }

data class TokenEditTransition(
    val next: TokenEditState,
    val effects: List<TokenEditEffect> = emptyList(),
)

fun reduceTokenEdit(
    state: TokenEditState,
    event: TokenEditEvent,
): TokenEditTransition {
    val stay = TokenEditTransition(state)
    return when (event) {
        TokenEditEvent.Start -> {
            if (state.editing) stay else TokenEditTransition(state.copy(editing = true, failure = null))
        }

        is TokenEditEvent.Save -> {
            if (!state.editing || state.verifying || event.blank) {
                stay
            } else {
                TokenEditTransition(state.copy(verifying = true, failure = null), listOf(TokenEditEffect.Verify))
            }
        }

        TokenEditEvent.VerifySucceeded -> {
            if (!state.verifying) {
                stay
            } else {
                TokenEditTransition(TokenEditState.Viewing, listOf(TokenEditEffect.Saved, TokenEditEffect.DiscardInput))
            }
        }

        is TokenEditEvent.VerifyFailed -> {
            if (!state.verifying) {
                stay
            } else {
                TokenEditTransition(state.copy(verifying = false, failure = event.kind))
            }
        }

        TokenEditEvent.Dismiss -> {
            when {
                state.editing && state.verifying -> {
                    TokenEditTransition(
                        state.copy(verifying = false, failure = null),
                        listOf(TokenEditEffect.CancelRequest),
                    )
                }

                state.editing -> {
                    TokenEditTransition(
                        state.copy(editing = false, failure = null),
                        listOf(TokenEditEffect.DiscardInput),
                    )
                }

                else -> {
                    stay
                }
            }
        }

        TokenEditEvent.EnterSettings -> {
            if (!state.editing) {
                stay
            } else {
                val effects =
                    buildList {
                        if (state.verifying) add(TokenEditEffect.CancelRequest)
                        add(TokenEditEffect.DiscardInput)
                    }
                TokenEditTransition(state.copy(editing = false, verifying = false, failure = null), effects)
            }
        }

        is TokenEditEvent.Clear -> {
            if (!event.hasToken || state.verifying) {
                stay
            } else {
                val effects =
                    buildList {
                        add(TokenEditEffect.ClearStored)
                        if (state.editing) add(TokenEditEffect.DiscardInput)
                    }
                TokenEditTransition(TokenEditState(undoable = true), effects)
            }
        }

        TokenEditEvent.Undo -> {
            if (!state.undoable) {
                stay
            } else {
                TokenEditTransition(state.copy(undoable = false), listOf(TokenEditEffect.RestoreStored))
            }
        }

        TokenEditEvent.ChangeRepository, TokenEditEvent.Disconnect -> {
            if (state.locksRepoActions) {
                TokenEditTransition(state, listOf(TokenEditEffect.Blocked))
            } else {
                stay
            }
        }
    }
}
