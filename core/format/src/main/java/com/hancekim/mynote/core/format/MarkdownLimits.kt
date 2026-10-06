package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.model.NoteLimits

private const val TAB_STOP = 4
private const val QUOTE = -1
private const val MAX_ITEM_PADDING = 4

internal fun exceedsMarkdownLimits(
    raw: String,
    limits: NoteLimits = NoteLimits.Default,
): Boolean = utf8LengthExceeds(raw, limits.maxBytes) || containerDepthExceeds(raw, limits.maxDepth)

internal fun utf8LengthExceeds(
    text: String,
    limit: Int,
): Boolean {
    if (text.length > limit) return true
    var bytes = 0
    var index = 0
    while (index < text.length) {
        val ch = text[index]
        bytes +=
            when {
                ch.code < 0x80 -> 1
                ch.code < 0x800 -> 2
                ch.isHighSurrogate() && text.getOrNull(index + 1)?.isLowSurrogate() == true -> 4.also { index++ }
                else -> 3
            }
        if (bytes > limit) return true
        index++
    }
    return false
}

/**
 * 줄 앞 컨테이너(`>` · 목록 표시) 중첩이 [limit] 을 넘는지 본다.
 *
 * 열린 컨테이너를 줄마다 이어 가며 세고, 줄 하나의 앞부분이 품을 수 있는 컨테이너 수가 [limit] 의 두 배를 넘어도 넘은 것으로 본다.
 */
internal fun containerDepthExceeds(
    raw: String,
    limit: Int,
): Boolean {
    val open = ArrayList<Int>()
    var start = if (raw.startsWith('\uFEFF')) 1 else 0
    while (true) {
        var end = start
        while (end < raw.length && raw[end] != '\n' && raw[end] != '\r') end++
        if (prefixCapacity(LineCursor(raw, start, end)) > limit * 2) return true
        if (scanLine(LineCursor(raw, start, end), open, limit)) return true
        if (end >= raw.length) return false
        start = if (raw[end] == '\r' && raw.getOrNull(end + 1) == '\n') end + 2 else end + 1
    }
}

private fun scanLine(
    line: LineCursor,
    open: MutableList<Int>,
    limit: Int,
): Boolean {
    var matched = 0
    while (matched < open.size) {
        val width = open[matched]
        if (width == QUOTE) {
            val pos = line.pos
            val col = line.col
            line.skipIndent()
            if (!line.consumeQuote()) {
                line.reset(pos, col)
                break
            }
        } else if (!line.restIsBlank()) {
            if (line.indentAhead() < width) break
            line.advanceColumns(width)
        }
        matched++
    }

    var opened = 0
    while (true) {
        val from = line.col
        val pos = line.pos
        line.skipIndent()
        val width =
            if (line.consumeQuote()) {
                QUOTE
            } else {
                val marker = line.listMarkerWidth()
                if (marker == 0) {
                    line.reset(pos, from)
                    break
                }
                line.advanceMarker(marker)
                val markerEnd = line.col
                val spaces = line.indentAhead()
                val padding = if (line.restIsBlank() || spaces > MAX_ITEM_PADDING) 1 else spaces
                line.advanceColumns(padding)
                markerEnd - from + padding
            }
        if (opened == 0) {
            while (open.size > matched) open.removeAt(open.lastIndex)
        }
        open += width
        opened++
        if (open.size > limit) return true
    }

    if (opened == 0 && line.restIsBlank()) {
        while (open.size > matched) open.removeAt(open.lastIndex)
    }
    return false
}

private fun prefixCapacity(line: LineCursor): Int {
    var markers = 0
    var indent = 0
    while (true) {
        val col = line.col
        line.skipIndent()
        indent += line.col - col
        if (line.consumeQuote()) {
            markers++
            continue
        }
        val marker = line.listMarkerWidth()
        if (marker == 0) break
        line.advanceMarker(marker)
        markers++
    }
    return markers + indent / 2
}

private class LineCursor(
    private val text: String,
    var pos: Int,
    private val end: Int,
) {
    var col = 0
        private set

    fun reset(
        pos: Int,
        col: Int,
    ) {
        this.pos = pos
        this.col = col
    }

    fun indentAhead(): Int {
        var p = pos
        var c = col
        while (p < end && text[p].isIndent()) {
            c += widthAt(p, c)
            p++
        }
        return c - col
    }

    fun advanceColumns(columns: Int) {
        var left = columns
        while (left > 0 && pos < end && text[pos].isIndent()) {
            val width = widthAt(pos, col)
            if (width <= left) {
                col += width
                pos++
                left -= width
            } else {
                col += left
                left = 0
            }
        }
    }

    fun skipIndent() = advanceColumns(Int.MAX_VALUE)

    fun restIsBlank(): Boolean = (pos until end).all { text[it].isIndent() }

    fun consumeQuote(): Boolean {
        if (pos >= end || text[pos] != '>') return false
        pos++
        col++
        advanceColumns(1)
        return true
    }

    fun listMarkerWidth(): Int {
        if (pos >= end) return 0
        val width =
            when (text[pos]) {
                '-', '+', '*' -> {
                    1
                }

                in '0'..'9' -> {
                    var p = pos
                    while (p < end && text[p] in '0'..'9') p++
                    if (p < end && (text[p] == '.' || text[p] == ')')) p - pos + 1 else 0
                }

                else -> {
                    0
                }
            }
        val after = pos + width
        return if (width > 0 && (after == end || text[after].isIndent())) width else 0
    }

    fun advanceMarker(width: Int) {
        pos += width
        col += width
    }

    private fun widthAt(
        index: Int,
        column: Int,
    ): Int = if (text[index] == '\t') TAB_STOP - column % TAB_STOP else 1
}

private fun Char.isIndent(): Boolean = this == ' ' || this == '\t'
