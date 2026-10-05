package com.hancekim.mynote.feature.notes.mapper

import com.hancekim.mynote.core.domain.model.Bookmark
import com.hancekim.mynote.feature.notes.BookmarkUi
import com.hancekim.mynote.feature.notes.CategoryUi
import com.hancekim.mynote.feature.notes.RecentNoteUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private val bookmarkOrder = compareByDescending<Bookmark> { it.addedAt }.thenBy { it.noteId }

internal fun bookmarkedNotes(
    categories: List<CategoryUi>,
    entries: List<Bookmark>,
): ImmutableList<BookmarkUi> {
    val byId = HashMap<String, RecentNoteUi>()
    categories.forEach { category ->
        category.notes.forEach { note -> byId.putIfAbsent(note.id, RecentNoteUi(note, category.name)) }
    }
    return entries
        .sortedWith(bookmarkOrder)
        .mapNotNull { entry -> byId[entry.noteId]?.let { BookmarkUi(it.note, it.categoryName, entry.addedAt) } }
        .toImmutableList()
}
