package com.hancekim.mynote.core.navigation.key

import kotlinx.serialization.Serializable

@Serializable
sealed interface NotesNavKey : MyNoteNavKey {
    @Serializable
    data object Home : NotesNavKey

    @Serializable
    data class Reader(
        val noteId: String,
    ) : NotesNavKey
}

fun notesBackStackOf(activeNoteId: String?): List<MyNoteNavKey> =
    if (activeNoteId == null) {
        listOf(NotesNavKey.Home)
    } else {
        listOf(NotesNavKey.Home, NotesNavKey.Reader(activeNoteId))
    }
