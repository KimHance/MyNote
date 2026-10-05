package com.hancekim.mynote.core.database.datasource

import com.hancekim.mynote.core.domain.model.BookmarkEventKind

private const val ADDED = "added"
private const val REMOVED = "removed"

internal fun BookmarkEventKind.toColumn(): String =
    when (this) {
        BookmarkEventKind.Added -> ADDED
        BookmarkEventKind.Removed -> REMOVED
    }

internal fun String.toBookmarkEventKind(): BookmarkEventKind? =
    when (this) {
        ADDED -> BookmarkEventKind.Added
        REMOVED -> BookmarkEventKind.Removed
        else -> null
    }
