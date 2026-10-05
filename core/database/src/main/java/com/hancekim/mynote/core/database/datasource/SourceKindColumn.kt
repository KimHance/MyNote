package com.hancekim.mynote.core.database.datasource

import com.hancekim.mynote.core.domain.model.SourceKind

private const val GITHUB = "github"
private const val LINK = "link"
private const val LOCAL = "local"

internal fun SourceKind.toColumn(): String =
    when (this) {
        SourceKind.GitHub -> GITHUB
        SourceKind.Link -> LINK
        SourceKind.Local -> LOCAL
    }

internal fun String.toSourceKind(): SourceKind? =
    when (this) {
        GITHUB -> SourceKind.GitHub
        LINK -> SourceKind.Link
        LOCAL -> SourceKind.Local
        else -> null
    }
