package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteLimits
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.TableAlign
import com.hancekim.mynote.core.domain.model.TableCell
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser

private const val INDENTED_CODE_COLUMNS = 4

private val HEADINGS: Map<IElementType, Int> =
    mapOf(
        MarkdownElementTypes.ATX_1 to 1,
        MarkdownElementTypes.ATX_2 to 2,
        MarkdownElementTypes.ATX_3 to 3,
        MarkdownElementTypes.ATX_4 to 4,
        MarkdownElementTypes.ATX_5 to 5,
        MarkdownElementTypes.ATX_6 to 6,
        MarkdownElementTypes.SETEXT_1 to 1,
        MarkdownElementTypes.SETEXT_2 to 2,
    )

private val HEADING_CONTENT = setOf(MarkdownTokenTypes.ATX_CONTENT, MarkdownTokenTypes.SETEXT_CONTENT)

private val QUOTES = setOf(MarkdownElementTypes.BLOCK_QUOTE, GFMElementTypes.ALERT)

private val LISTS = setOf(MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST)

private val SKIPPED =
    setOf(
        MarkdownTokenTypes.EOL,
        MarkdownTokenTypes.WHITE_SPACE,
        MarkdownTokenTypes.BLOCK_QUOTE,
        MarkdownTokenTypes.LIST_BULLET,
        MarkdownTokenTypes.LIST_NUMBER,
        GFMTokenTypes.CHECK_BOX,
        MarkdownElementTypes.LINK_DEFINITION,
    )

/**
 * GFM 마크다운 원문을 블록으로 바꾼다. 블록 깊이나 표 칸 수가 [limits] 의 상한을 넘으면 노트 전체가 한도 넘음 상자 하나다.
 *
 * @param limits 블록 깊이 · 표 칸 수 상한을 든 한도
 */
internal class GfmMarkdownConverter(
    private val limits: NoteLimits = NoteLimits.Default,
) : MarkdownConverter {
    private val parser = MarkdownParser(NoteMarkdownFlavour, assertionsEnabled = false)

    override fun convert(raw: String): List<NoteBlock> {
        val body = markdownSource(raw).body
        val root = parser.buildMarkdownTreeFromString(body)
        val links = LinkMap.buildLinkMap(root, body)
        val reader = BlockReader(body, limits) { !it.isFootnoteLabel() && links.getLinkInfo(it) != null }
        val blocks = reader.blocks(root.children, depth = 1)
        return if (reader.exceeded) {
            listOf(NoteBlock.Unsupported(UnsupportedReason.LimitExceeded, type = null, text = null, raw = raw))
        } else {
            blocks
        }
    }
}

private class BlockReader(
    private val source: String,
    private val limits: NoteLimits,
    private val defined: (label: String) -> Boolean,
) {
    var exceeded = false
        private set

    private var tableCells = 0L

    fun blocks(
        nodes: List<ASTNode>,
        depth: Int,
    ): List<NoteBlock> {
        if (depth > limits.maxDepth) exceeded = true
        if (exceeded) return emptyList()
        return nodes.filterNot(::skipped).mapNotNull { block(it, depth) }
    }

    private fun skipped(node: ASTNode): Boolean = node.type in SKIPPED && !isFootnoteDefinition(node)

    private fun isFootnoteDefinition(node: ASTNode): Boolean {
        if (node.type != MarkdownElementTypes.LINK_DEFINITION) return false
        val label = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_LABEL } ?: return false
        return text(label).isFootnoteLabel()
    }

    private fun block(
        node: ASTNode,
        depth: Int,
    ): NoteBlock? {
        val type = node.type
        return when {
            type in HEADINGS -> {
                NoteBlock.Heading(HEADINGS.getValue(type), headingSpans(node))
            }

            type == MarkdownElementTypes.PARAGRAPH -> {
                spans(node).takeIf { it.isNotEmpty() }?.let(NoteBlock::Paragraph)
            }

            type == MarkdownElementTypes.CODE_FENCE -> {
                NoteBlock.Code(fenceCode(node))
            }

            type == MarkdownElementTypes.CODE_BLOCK -> {
                NoteBlock.Code(indentedCode(node))
            }

            type == MarkdownElementTypes.HTML_BLOCK -> {
                NoteBlock.Code(htmlCode(node))
            }

            type in QUOTES -> {
                NoteBlock.Quote(quoteSpans(node))
            }

            type in LISTS -> {
                list(node, depth)
            }

            type == GFMElementTypes.TABLE -> {
                table(node)
            }

            type == MarkdownTokenTypes.HORIZONTAL_RULE -> {
                NoteBlock.Divider
            }

            type == MarkdownElementTypes.LINK_DEFINITION -> {
                NoteBlock.Paragraph(literalSpans(node))
            }

            else -> {
                literalSpans(node).takeIf { it.isNotEmpty() }?.let(NoteBlock::Paragraph)
            }
        }
    }

    private fun list(
        node: ASTNode,
        depth: Int,
    ): NoteBlock {
        val items = node.children.filter { it.type == MarkdownElementTypes.LIST_ITEM }
        val listItems = items.map { item(it, depth) }
        if (node.type == MarkdownElementTypes.UNORDERED_LIST) return NoteBlock.BulletList(listItems)
        val start =
            items
                .firstOrNull()
                ?.children
                ?.firstOrNull { it.type == MarkdownTokenTypes.LIST_NUMBER }
                ?.let { text(it).trim().dropLast(1).toIntOrNull() }
                ?: 1
        return NoteBlock.OrderedList(listItems, start)
    }

    private fun item(
        node: ASTNode,
        depth: Int,
    ): NoteListItem {
        val checked = node.children.firstOrNull { it.type == GFMTokenTypes.CHECK_BOX }?.let(::isChecked)
        val content = node.children.filterNot(::skipped)
        val first = content.firstOrNull()?.takeIf { it.type == MarkdownElementTypes.PARAGRAPH }
        val rest = if (first == null) content else content.drop(1)
        return NoteListItem(
            spans = first?.let(::spans).orEmpty(),
            checked = checked,
            children = if (rest.isEmpty()) emptyList() else blocks(rest, depth + 1),
        )
    }

    private fun table(node: ASTNode): NoteBlock.Table {
        val header =
            node.children
                .firstOrNull { it.type == GFMElementTypes.HEADER }
                ?.let(::cells)
                .orEmpty()
        val columns = header.size
        val rowNodes = node.children.filter { it.type == GFMElementTypes.ROW }
        tableCells += columns.toLong() * (rowNodes.size + 1)
        if (tableCells > limits.maxTableCells) {
            exceeded = true
            return NoteBlock.Table(emptyList(), emptyList(), emptyList())
        }
        val rows = rowNodes.map { row -> cells(row).take(columns).padded(columns) { TableCell(emptyList()) } }
        val delimiter = node.children.firstOrNull { it.type == GFMTokenTypes.TABLE_SEPARATOR && '-' in text(it) }
        val align = delimiter?.let { alignments(text(it)) }.orEmpty()
        return NoteBlock.Table(header, rows, align.take(columns).padded(columns) { TableAlign.None })
    }

    private fun cells(row: ASTNode): List<TableCell> =
        row.children
            .filter { it.type == GFMTokenTypes.CELL }
            .map { TableCell(SpanWriter(source, defined, inTable = true).inline(it.children).spans()) }

    private fun headingSpans(node: ASTNode): List<NoteSpan> =
        node.children
            .firstOrNull { it.type in HEADING_CONTENT }
            ?.let(::spans)
            .orEmpty()

    private fun spans(node: ASTNode): List<NoteSpan> = SpanWriter(source, defined).inline(node.children).spans()

    private fun literalSpans(node: ASTNode): List<NoteSpan> = SpanWriter(source, defined).literal(text(node)).spans()

    private fun checkMark(item: ASTNode): String? =
        item.children.firstOrNull { it.type == GFMTokenTypes.CHECK_BOX }?.let { if (isChecked(it)) "[x] " else "[ ] " }

    private fun isChecked(box: ASTNode): Boolean = text(box).trim().equals("[x]", ignoreCase = true)

    private fun quoteSpans(node: ASTNode): List<NoteSpan> {
        val lines = ArrayList<List<NoteSpan>>()
        quoteLines(node, lines)
        return lines
            .filter { it.isNotEmpty() }
            .flatMapIndexed { index, line -> if (index == 0) line else listOf(NoteSpan("\n")) + line }
            .mergeAdjacent()
    }

    private fun quoteLines(
        node: ASTNode,
        lines: MutableList<List<NoteSpan>>,
    ) {
        for (child in node.children) {
            val type = child.type
            when {
                type == MarkdownElementTypes.PARAGRAPH -> {
                    lines += spans(child)
                }

                type == GFMTokenTypes.ALERT_TITLE || isFootnoteDefinition(child) -> {
                    lines += literalSpans(child)
                }

                type in HEADINGS -> {
                    lines += headingSpans(child)
                }

                type == MarkdownElementTypes.CODE_FENCE -> {
                    lines += plainLines(fenceCode(child))
                }

                type == MarkdownElementTypes.CODE_BLOCK -> {
                    lines += plainLines(indentedCode(child))
                }

                type == MarkdownElementTypes.HTML_BLOCK -> {
                    lines += plainLines(htmlCode(child))
                }

                type == GFMElementTypes.TABLE -> {
                    lines += tableLines(table(child))
                }

                type == MarkdownElementTypes.LIST_ITEM -> {
                    val first = lines.size
                    quoteLines(child, lines)
                    val mark = checkMark(child)
                    if (mark != null && lines.size > first) lines[first] = listOf(NoteSpan(mark)) + lines[first]
                }

                type in QUOTES || type in LISTS -> {
                    quoteLines(child, lines)
                }
            }
        }
    }

    private fun plainLines(text: String): List<List<NoteSpan>> =
        if (text.isEmpty()) emptyList() else text.split('\n').map { listOf(NoteSpan(it)) }

    private fun tableLines(table: NoteBlock.Table): List<List<NoteSpan>> =
        (listOf(table.header) + table.rows).map { row ->
            row.flatMapIndexed { index, cell -> if (index == 0) cell.spans else listOf(NoteSpan(" | ")) + cell.spans }
        }

    private fun fenceCode(node: ASTNode): String {
        val start = node.children.firstOrNull { it.type == MarkdownTokenTypes.CODE_FENCE_START }
        val indent = start?.let { text(it).takeWhile { ch -> ch == ' ' }.length } ?: 0
        val closed = node.children.any { it.type == MarkdownTokenTypes.CODE_FENCE_END }
        val endsWithLine = node.children.lastOrNull()?.type == MarkdownTokenTypes.EOL
        val lines = codeLines(node, MarkdownTokenTypes.CODE_FENCE_CONTENT) { it.dropSpaces(indent) }.drop(1)
        return (if (closed || endsWithLine) lines.dropLast(1) else lines).joinToString("\n")
    }

    private fun indentedCode(node: ASTNode): String =
        codeLines(node, MarkdownTokenTypes.CODE_LINE) { it.dropColumns(INDENTED_CODE_COLUMNS) }
            .dropLastWhile { it.isBlank() }
            .joinToString("\n")

    private fun htmlCode(node: ASTNode): String =
        codeLines(node, MarkdownTokenTypes.HTML_BLOCK_CONTENT) {
            it
        }.joinToString("\n")

    private fun codeLines(
        node: ASTNode,
        content: IElementType,
        line: (String) -> String,
    ): List<String> {
        val lines = mutableListOf(StringBuilder())
        for (child in node.children) {
            when (child.type) {
                MarkdownTokenTypes.EOL -> lines += StringBuilder()
                content -> lines.last().append(line(text(child)))
            }
        }
        return lines.map { it.toString() }
    }

    private fun text(node: ASTNode): String = source.substring(node.startOffset, node.endOffset)
}

private fun alignments(delimiterRow: String): List<TableAlign> =
    delimiterRow
        .trim()
        .removePrefix("|")
        .removeSuffix("|")
        .split('|')
        .map { cell ->
            val marker = cell.trim()
            when {
                marker.startsWith(':') && marker.endsWith(':') && marker.length > 1 -> TableAlign.Center
                marker.startsWith(':') -> TableAlign.Left
                marker.endsWith(':') -> TableAlign.Right
                else -> TableAlign.None
            }
        }

private fun String.isFootnoteLabel(): Boolean = startsWith("[^")

private fun <T> List<T>.padded(
    size: Int,
    filler: () -> T,
): List<T> = if (this.size >= size) this else this + List(size - this.size) { filler() }

private fun String.dropSpaces(count: Int): String = drop(takeWhile { it == ' ' }.length.coerceAtMost(count))

private fun String.dropColumns(columns: Int): String {
    var column = 0
    var index = 0
    while (index < length && column < columns) {
        column += if (this[index] == '\t') INDENTED_CODE_COLUMNS - column % INDENTED_CODE_COLUMNS else 1
        index++
    }
    return substring(index)
}
