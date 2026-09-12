package com.hancekim.mynote.core.navigation

import com.hancekim.mynote.core.navigation.key.MyNoteNavKey

sealed interface NavCommand {
    data class Push(
        val key: MyNoteNavKey,
    ) : NavCommand

    data object Pop : NavCommand

    data class ReplaceAll(
        val keys: List<MyNoteNavKey>,
    ) : NavCommand
}
