package com.hancekim.mynote.feature.notes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.hancekim.mynote.core.navigation.shared.navSharedBounds
import com.hancekim.mynote.core.navigation.shared.navSharedElement

@Stable
internal data class NoteSharedModifiers(
    val chip: Modifier = Modifier,
    val date: Modifier = Modifier,
    val title: Modifier = Modifier,
)

@Composable
internal fun rememberNoteSharedModifiers(noteId: String): NoteSharedModifiers =
    NoteSharedModifiers(
        chip = Modifier.navSharedElement("chip-$noteId"),
        date = Modifier.navSharedElement("date-$noteId"),
        title = Modifier.navSharedBounds("title-$noteId"),
    )
