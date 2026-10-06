package com.hancekim.mynote.core.domain.model

private const val DEFAULT_MAX_CHARS = 140
private const val ELLIPSIS = "…"
private val SENTENCE_ENDS = setOf('.', '?', '!', '。')
private val ABBREVIATIONS = setOf("e.g.", "i.e.", "etc.", "vs.")

fun Note.firstSentence(maxChars: Int = DEFAULT_MAX_CHARS): String? = blocks.firstSentence(maxChars)

/** 첫 문단(없으면 첫 목록 항목)의 첫 문장. [maxChars] 를 넘으면 말줄임으로 자른다. */
fun List<NoteBlock>.firstSentence(maxChars: Int = DEFAULT_MAX_CHARS): String? {
    val spans = firstParagraphSpans() ?: firstListItemSpans() ?: return null
    val flat = flatten(spans)
    val sentence = cutSentence(flat)
    return clip(sentence, maxChars).takeIf { it.isNotBlank() }
}

private fun List<NoteBlock>.firstParagraphSpans(): List<NoteSpan>? =
    asSequence()
        .filterIsInstance<NoteBlock.Paragraph>()
        .map { it.spans }
        .firstOrNull { spans -> spans.any { it.text.isNotBlank() } }

private fun List<NoteBlock>.firstListItemSpans(): List<NoteSpan>? =
    asSequence()
        .flatMap { it.listItems() }
        .map { it.spans }
        .firstOrNull { spans -> spans.any { it.text.isNotBlank() } }

private fun NoteBlock.listItems(): Sequence<NoteListItem> =
    when (this) {
        is NoteBlock.BulletList -> items.asSequence()
        is NoteBlock.OrderedList -> items.asSequence()
        else -> emptySequence()
    }.flatMap { item -> sequenceOf(item) + item.children.asSequence().flatMap { it.listItems() } }

private class FlatText(
    val text: String,
    val inCode: BooleanArray,
)

private fun flatten(spans: List<NoteSpan>): FlatText {
    val builder = StringBuilder()
    val codeFlags = ArrayList<Boolean>()
    var pendingSpace = false
    for (span in spans) {
        val code = span.style == SpanStyle.Code
        for (ch in span.text) {
            if (ch.isWhitespace()) {
                pendingSpace = true
                continue
            }
            if (pendingSpace && builder.isNotEmpty()) {
                builder.append(' ')
                codeFlags.add(false)
            }
            pendingSpace = false
            builder.append(ch)
            codeFlags.add(code)
        }
    }
    return FlatText(builder.toString(), codeFlags.toBooleanArray())
}

private fun cutSentence(flat: FlatText): String {
    val text = flat.text
    for (index in text.indices) {
        val ch = text[index]
        if (ch !in SENTENCE_ENDS || flat.inCode[index]) continue
        val next = text.getOrNull(index + 1)
        if (next != null && next != ' ') continue
        if (ch == '.' && isBetweenDigits(text, index)) continue
        if (ch == '.' && isAbbreviation(text, index)) continue
        return text.substring(0, index + 1)
    }
    return text
}

private fun isBetweenDigits(
    text: String,
    index: Int,
): Boolean = text.getOrNull(index - 1)?.isDigit() == true && text.getOrNull(index + 1)?.isDigit() == true

private fun isAbbreviation(
    text: String,
    index: Int,
): Boolean {
    val start = text.lastIndexOf(' ', index - 1) + 1
    val token = text.substring(start, index + 1).lowercase()
    return token in ABBREVIATIONS
}

private fun clip(
    text: String,
    maxChars: Int,
): String {
    if (text.length <= maxChars) return text
    val head = text.substring(0, maxChars)
    val lastSpace = head.lastIndexOf(' ')
    val cut = if (lastSpace > 0) head.substring(0, lastSpace) else head
    return cut.trimEnd() + ELLIPSIS
}
