package com.hancekim.mynote.core.ai.download.decision

import java.util.Locale

internal fun percentOf(received: Long, total: Long): Int =
    when {
        total <= 0L -> 0
        else -> (received * PERCENT_FULL / total).coerceIn(0L, PERCENT_FULL.toLong()).toInt()
    }

internal fun gigabytesOf(bytes: Long): String =
    String.format(Locale.US, "%.2f", bytes.coerceAtLeast(0L) / BYTES_PER_GIGABYTE)

internal const val PERCENT_FULL = 100

private const val BYTES_PER_GIGABYTE = 1_000_000_000.0
