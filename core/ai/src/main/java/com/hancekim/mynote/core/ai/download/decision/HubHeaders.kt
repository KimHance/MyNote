package com.hancekim.mynote.core.ai.download.decision

internal const val HEADER_LINKED_SIZE = "x-linked-size"
internal const val HEADER_LINKED_ETAG = "x-linked-etag"
internal const val HEADER_REPO_COMMIT = "x-repo-commit"
internal const val HEADER_RANGE = "Range"
internal const val HEADER_ACCEPT_ENCODING = "Accept-Encoding"
internal const val HEADER_CONTENT_RANGE = "Content-Range"
internal const val HEADER_RETRY_AFTER = "Retry-After"
internal const val ENCODING_IDENTITY = "identity"

internal fun planFrom(size: String?, etag: String?, commit: String?): DownloadPlan? {
    val totalBytes = size?.trim()?.toLongOrNull()?.takeIf { it > 0L } ?: return null
    val sha256 =
        etag
            ?.trim()
            ?.removePrefix("W/")
            ?.trim('"')
            ?.takeIf { it.isNotBlank() } ?: return null
    val repoCommit = commit?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return DownloadPlan(commit = repoCommit, sha256 = sha256, totalBytes = totalBytes)
}

internal fun rangeHeader(from: Long): String? = if (from > 0L) "bytes=$from-" else null
