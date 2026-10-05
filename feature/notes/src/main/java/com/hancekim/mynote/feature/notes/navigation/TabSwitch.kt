package com.hancekim.mynote.feature.notes.navigation

import com.hancekim.mynote.core.navigation.NavCommand
import com.hancekim.mynote.core.navigation.key.MyNoteNavKey
import com.hancekim.mynote.core.navigation.key.NotesNavKey
import com.hancekim.mynote.feature.notes.element.NotesTab

internal fun List<MyNoteNavKey>.switchTabCommand(tab: NotesTab): NavCommand =
    when (tab) {
        NotesTab.Home -> NavCommand.ReplaceAll(HOME_ROOT)
        NotesTab.Bookmarks -> tabCommand(NotesNavKey.Bookmarks, BOOKMARKS_ROOT)
        NotesTab.Stats -> tabCommand(NotesNavKey.Stats, STATS_ROOT)
    }

private fun List<MyNoteNavKey>.tabCommand(
    key: MyNoteNavKey,
    root: List<MyNoteNavKey>,
): NavCommand = if (this == HOME_ROOT) NavCommand.Push(key) else NavCommand.ReplaceAll(root)

private val HOME_ROOT: List<MyNoteNavKey> = listOf(NotesNavKey.Home)
private val BOOKMARKS_ROOT: List<MyNoteNavKey> = listOf(NotesNavKey.Home, NotesNavKey.Bookmarks)
private val STATS_ROOT: List<MyNoteNavKey> = listOf(NotesNavKey.Home, NotesNavKey.Stats)
