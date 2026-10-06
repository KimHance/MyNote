package com.hancekim.mynote.core.ai.download.decision

internal const val HEADER_RANGE = "Range"
internal const val HEADER_ACCEPT_ENCODING = "Accept-Encoding"
internal const val HEADER_CONTENT_RANGE = "Content-Range"
internal const val ENCODING_IDENTITY = "identity"

internal fun rangeHeader(from: Long): String? = if (from > 0L) "bytes=$from-" else null
