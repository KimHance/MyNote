package com.hancekim.mynote.feature.notes

import androidx.compose.runtime.Immutable
import com.hancekim.mynote.core.designsystem.block.NoteContentBlock
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class NoteUi(
    val id: String,
    val title: String,
    val date: String,
    val blocks: ImmutableList<NoteContentBlock> = persistentListOf(),
    val excerpt: String? = null,
    val readable: Boolean = true,
)

@Immutable
data class CategoryUi(
    val name: String,
    val notes: ImmutableList<NoteUi> = persistentListOf(),
)

@Immutable
data class RecentNoteUi(
    val note: NoteUi,
    val categoryName: String,
)

@Immutable
data class BookmarkUi(
    val note: NoteUi,
    val categoryName: String,
    val addedAt: Long,
)
