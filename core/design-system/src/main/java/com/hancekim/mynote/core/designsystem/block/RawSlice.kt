package com.hancekim.mynote.core.designsystem.block

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal data class RawSlice(
    val lines: ImmutableList<String>,
    val hasMore: Boolean,
)

internal fun sliceRaw(
    raw: String,
    maxLines: Int,
    maxChars: Int = UnsupportedBlockDefaults.MAX_CHARS,
): RawSlice {
    if (raw.isEmpty()) return RawSlice(persistentListOf(), hasMore = false)
    val all = raw.lines().let { if (it.size > 1 && it.last().isEmpty()) it.dropLast(1) else it }
    val shown = ArrayList<String>(minOf(all.size, maxLines))
    var used = 0
    for (line in all) {
        if (shown.size == maxLines) break
        val cost = line.length + if (shown.isEmpty()) 0 else 1
        if (used + cost > maxChars) {
            if (shown.isEmpty()) shown += line.take(maxChars)
            return RawSlice(shown.toImmutableList(), hasMore = true)
        }
        shown += line
        used += cost
    }
    return RawSlice(shown.toImmutableList(), hasMore = shown.size < all.size)
}

internal fun leadingCells(line: String): Int =
    line.takeWhile { it == ' ' || it == '\t' }.sumOf { if (it == '\t') TAB_CELLS else 1 }

private const val TAB_CELLS = 2
