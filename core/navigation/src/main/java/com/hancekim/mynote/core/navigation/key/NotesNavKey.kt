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

fun List<MyNoteNavKey>.activeNoteId(): String? = (lastOrNull() as? NotesNavKey.Reader)?.noteId
