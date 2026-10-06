package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.SpanStyle
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.html.entities.Entities

private const val REPLACEMENT_CHARACTER = 0xFFFD

private const val EDGE_SPACES = " \t\n"

private val ESCAPE_OR_ENTITY =
    Regex("""\\([!-/:-@\[-`{-~])|&(?:#[xX]([0-9a-fA-F]{1,6})|#([0-9]{1,7})|([a-zA-Z][a-zA-Z0-9]{0,31}));""")

private val LINKS =
    setOf(
        MarkdownElementTypes.INLINE_LINK,
        MarkdownElementTypes.FULL_REFERENCE_LINK,
        MarkdownElementTypes.SHORT_REFERENCE_LINK,
    )

private val LITERAL_TOKENS =
    setOf(
        MarkdownTokenTypes.HTML_TAG,
        MarkdownTokenTypes.AUTOLINK,
        MarkdownTokenTypes.EMAIL_AUTOLINK,
        GFMTokenTypes.GFM_AUTOLINK,
    )

private data class Marks(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strike: Boolean = false,
)

private class Frame(
    val node: ASTNode,
    val marks: Marks,
)

/**
 * 인라인 노드를 글자 구간으로 바꾼다. 깊은 강조에도 스택이 넘치지 않게 반복으로 걷는다.
 * 링크로 성립하지 않는 링크 노드(정의 없는 참조 · 공백 든 목적지)는 원문 글자 그대로 둔다.
 *
 * @param source 노드 위치가 가리키는 본문
 * @param defined 괄호를 포함한 참조 라벨에 정의가 있는지
 * @param inTable 표 칸이면 코드 구간의 `\|` 를 세로줄 하나로 읽는다
 */
internal class SpanWriter(
    private val source: String,
    private val defined: (label: String) -> Boolean,
    private val inTable: Boolean = false,
) {
    private val spans = ArrayList<NoteSpan>()
    private val pending = StringBuilder()
    private var pendingMarks = Marks()
    private var afterBreak = false
    private var afterLineEnd = false

    fun inline(children: List<ASTNode>): SpanWriter {
        walk(children.dropEmailBrackets(), Marks())
        return this
    }

    fun literal(text: String): SpanWriter {
        appendLiteral(text, Marks(), code = false)
        return this
    }

    fun spans(): List<NoteSpan> {
        flush()
        return spans.trimmed()
    }

    private fun walk(
        nodes: List<ASTNode>,
        marks: Marks,
    ) {
        val stack = ArrayDeque<Frame>()
        nodes.asReversed().forEach { stack.addLast(Frame(it, marks)) }
        while (stack.isNotEmpty()) {
            val frame = stack.removeLast()
            val inner = nested(frame.node, frame.marks)
            if (inner == null) {
                write(frame.node, frame.marks)
            } else {
                inner.second
                    .dropEmailBrackets()
                    .asReversed()
                    .forEach { stack.addLast(Frame(it, inner.first)) }
            }
        }
    }

    private fun nested(
        node: ASTNode,
        marks: Marks,
    ): Pair<Marks, List<ASTNode>>? =
        when (node.type) {
            MarkdownElementTypes.STRONG -> marks.copy(bold = true) to node.children.inner(2)
            MarkdownElementTypes.EMPH -> marks.copy(italic = true) to node.children.inner(1)
            GFMElementTypes.STRIKETHROUGH -> marks.copy(strike = true) to node.children.inner(tildeRun(node))
            in LINKS -> marks to if (isLink(node)) node.linkText() else node.children
            MarkdownElementTypes.IMAGE -> marks to image(node)
            MarkdownElementTypes.CODE_SPAN, MarkdownElementTypes.AUTOLINK -> null
            else -> if (node.children.isEmpty()) null else marks to node.children
        }

    private fun write(
        node: ASTNode,
        marks: Marks,
    ) {
        when (node.type) {
            MarkdownElementTypes.CODE_SPAN -> appendLiteral(codeSpan(node), marks, code = true)
            MarkdownElementTypes.AUTOLINK -> autolink(node, marks)
            else -> leaf(node, marks)
        }
    }

    private fun leaf(
        node: ASTNode,
        marks: Marks,
    ) {
        val type = node.type
        val lineStart = afterLineEnd
        val breakBefore = afterBreak
        afterLineEnd = false
        afterBreak = false
        when {
            type == MarkdownTokenTypes.HARD_LINE_BREAK -> {
                appendLiteral("\n", marks, code = false)
                afterBreak = true
            }

            type == MarkdownTokenTypes.EOL -> {
                if (!breakBefore) {
                    dropTrailingSpaces()
                    appendText(" ", marks)
                }
                afterLineEnd = true
            }

            type == MarkdownTokenTypes.BLOCK_QUOTE && inTable -> {
                appendText(text(node), marks)
            }

            type == MarkdownTokenTypes.BLOCK_QUOTE -> {
                afterLineEnd = lineStart
            }

            type == MarkdownTokenTypes.WHITE_SPACE && lineStart -> {
                Unit
            }

            type in LITERAL_TOKENS -> {
                appendLiteral(text(node), marks, code = false)
            }

            else -> {
                appendText(text(node), marks)
            }
        }
    }

    private fun autolink(
        node: ASTNode,
        marks: Marks,
    ) {
        val link = node.children.firstOrNull { it.type == MarkdownTokenTypes.AUTOLINK }
        appendLiteral(link?.let(::text) ?: text(node), marks, code = false)
    }

    private fun codeSpan(node: ASTNode): String {
        val content = StringBuilder()
        var lineStart = false
        for (child in node.children.trimBackticks()) {
            when (child.type) {
                MarkdownTokenTypes.EOL -> {
                    content.append(' ')
                    lineStart = true
                }

                MarkdownTokenTypes.BLOCK_QUOTE -> {
                    Unit
                }

                MarkdownTokenTypes.WHITE_SPACE -> {
                    if (!lineStart) content.append(text(child))
                }

                else -> {
                    content.append(text(child))
                    lineStart = false
                }
            }
        }
        val code = if (inTable) content.toString().replace("\\|", "|") else content.toString()
        return if (code.length >= 2 && code.startsWith(' ') && code.endsWith(' ') && code.isNotBlank()) {
            code.substring(1, code.length - 1)
        } else {
            code
        }
    }

    private fun appendText(
        text: String,
        marks: Marks,
    ) {
        if (pending.isNotEmpty() && marks != pendingMarks) flush()
        pendingMarks = marks
        pending.append(text)
    }

    private fun appendLiteral(
        text: String,
        marks: Marks,
        code: Boolean,
    ) {
        flush()
        if (text.isEmpty()) return
        spans += marks.span(text, code)
    }

    private fun dropTrailingSpaces() {
        if (pending.isNotEmpty()) {
            pending.setLength(pending.trimEnd(' ', '\t').length)
            return
        }
        val last = spans.lastOrNull() ?: return
        if (last.style != SpanStyle.Code) spans[spans.lastIndex] = last.copy(text = last.text.trimEnd(' ', '\t'))
    }

    private fun flush() {
        if (pending.isEmpty()) return
        spans += pendingMarks.span(decode(pending.toString()), code = false)
        pending.setLength(0)
    }

    private fun image(node: ASTNode): List<ASTNode> {
        val link = node.children.firstOrNull { it.type in LINKS }
        return if (link != null && isLink(link)) listOf(link) else node.children
    }

    private fun isLink(node: ASTNode): Boolean =
        if (node.type == MarkdownElementTypes.INLINE_LINK) {
            node.children.none { child ->
                child.type == MarkdownElementTypes.LINK_DESTINATION &&
                    text(child).let { !it.startsWith('<') && it.any(Char::isWhitespace) }
            }
        } else {
            node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_LABEL }?.let { defined(text(it)) } == true
        }

    private fun text(node: ASTNode): String = source.substring(node.startOffset, node.endOffset)

    private fun ASTNode.linkText(): List<ASTNode> {
        val label =
            children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
                ?: children.firstOrNull { it.type == MarkdownElementTypes.LINK_LABEL }
                ?: return emptyList()
        return label.children.trimBrackets()
    }
}

private fun Marks.span(
    text: String,
    code: Boolean,
): NoteSpan =
    if (code) {
        NoteSpan(text, SpanStyle.Code, strike = strike)
    } else {
        NoteSpan(text, if (bold) SpanStyle.Bold else SpanStyle.Text, italic = italic, strike = strike)
    }

private fun List<ASTNode>.inner(count: Int): List<ASTNode> = drop(count).dropLast(count)

private fun tildeRun(node: ASTNode): Int = node.children.takeWhile { it.type == GFMTokenTypes.TILDE }.size

private fun List<ASTNode>.trimBackticks(): List<ASTNode> = trimEnds(MarkdownTokenTypes.BACKTICK)

private fun List<ASTNode>.trimBrackets(): List<ASTNode> {
    val start = if (firstOrNull()?.type == MarkdownTokenTypes.LBRACKET) 1 else 0
    val end = if (size > start && last().type == MarkdownTokenTypes.RBRACKET) size - 1 else size
    return subList(start, end)
}

private fun List<ASTNode>.trimEnds(type: IElementType): List<ASTNode> {
    val start = if (firstOrNull()?.type == type) 1 else 0
    val end = if (size > start && last().type == type) size - 1 else size
    return subList(start, end)
}

private fun List<ASTNode>.dropEmailBrackets(): List<ASTNode> {
    if (none { it.type == MarkdownTokenTypes.EMAIL_AUTOLINK }) return this
    val dropped = HashSet<Int>()
    forEachIndexed { index, node ->
        if (node.type == MarkdownTokenTypes.EMAIL_AUTOLINK &&
            getOrNull(index - 1)?.type == MarkdownTokenTypes.LT &&
            getOrNull(index + 1)?.type == MarkdownTokenTypes.GT
        ) {
            dropped += index - 1
            dropped += index + 1
        }
    }
    return filterIndexed { index, _ -> index !in dropped }
}

internal fun List<NoteSpan>.mergeAdjacent(): MutableList<NoteSpan> {
    val merged = ArrayList<NoteSpan>(size)
    for (span in this) {
        val last = merged.lastOrNull()
        if (last != null && last.copy(text = "") == span.copy(text = "")) {
            merged[merged.lastIndex] = last.copy(text = last.text + span.text)
        } else {
            merged += span
        }
    }
    return merged
}

private fun List<NoteSpan>.trimmed(): List<NoteSpan> {
    val merged = mergeAdjacent()
    while (merged.firstOrNull()?.isBlankText() == true) merged.removeAt(0)
    while (merged.lastOrNull()?.isBlankText() == true) merged.removeAt(merged.lastIndex)
    if (merged.isNotEmpty()) {
        merged[0] = merged.first().trimText { it.trimStart(*EDGE_SPACES.toCharArray()) }
        merged[merged.lastIndex] = merged.last().trimText { it.trimEnd(*EDGE_SPACES.toCharArray()) }
    }
    return merged
}

private fun NoteSpan.isBlankText(): Boolean = style != SpanStyle.Code && text.all { it in EDGE_SPACES }

private fun NoteSpan.trimText(trim: (String) -> String): NoteSpan =
    if (style ==
        SpanStyle.Code
    ) {
        this
    } else {
        copy(text = trim(text))
    }

private fun decode(text: String): String {
    if ('\\' !in text && '&' !in text) return text
    return ESCAPE_OR_ENTITY.replace(text) { match ->
        val (escaped, hex, decimal, name) = match.destructured
        when {
            escaped.isNotEmpty() -> escaped
            hex.isNotEmpty() -> codePoint(hex.toInt(16))
            decimal.isNotEmpty() -> codePoint(decimal.toInt())
            else -> TWO_CODE_POINT_ENTITIES[name] ?: Entities.map["&$name;"]?.let(::codePoint) ?: match.value
        }
    }
}

private fun codePoint(code: Int): String {
    val valid =
        code in 1..Character.MAX_CODE_POINT && code !in Character.MIN_SURROGATE.code..Character.MAX_SURROGATE.code
    return String(Character.toChars(if (valid) code else REPLACEMENT_CHARACTER))
}
