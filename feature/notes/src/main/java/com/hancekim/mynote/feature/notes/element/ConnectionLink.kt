package com.hancekim.mynote.feature.notes.element

import com.hancekim.mynote.core.domain.model.RepoConnection

fun connectionLinkOf(connection: RepoConnection): String {
    val base = "https://github.com/${connection.owner}/${connection.repo}"
    return when {
        connection.branch.isBlank() -> base
        connection.indexPath == RepoConnection.DEFAULT_INDEX_PATH -> "$base/tree/${connection.branch}"
        else -> "$base/blob/${connection.branch}/${connection.indexPath}"
    }
}
