package com.hancekim.mynote.core.ai.download

internal fun enoughSpace(allocatableBytes: Long, totalBytes: Long, partBytes: Long): Boolean =
    allocatableBytes >= totalBytes - partBytes
