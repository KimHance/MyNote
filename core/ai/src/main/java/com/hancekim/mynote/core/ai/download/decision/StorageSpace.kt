package com.hancekim.mynote.core.ai.download.decision

internal fun enoughSpace(allocatableBytes: Long, totalBytes: Long, partBytes: Long): Boolean =
    allocatableBytes >= totalBytes - partBytes
