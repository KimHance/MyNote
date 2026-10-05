package com.hancekim.mynote.core.navigation.key

import kotlinx.serialization.Serializable

@Serializable
sealed interface NotesNavKey : MyNoteNavKey {
    @Serializable
    data object Home : NotesNavKey

    @Serializable
    data object Bookmarks : NotesNavKey

    @Serializable
    data object Stats : NotesNavKey

    @Serializable
    data object StatsHistory : NotesNavKey

    @Serializable
    data class Reader(
        val noteId: String,
    ) : NotesNavKey

    @Serializable
    data object Settings : NotesNavKey
}

fun List<MyNoteNavKey>.isSettingsTop(): Boolean = lastOrNull() is NotesNavKey.Settings

fun List<MyNoteNavKey>.activeNoteId(): String? = (lastOrNull() as? NotesNavKey.Reader)?.noteId
