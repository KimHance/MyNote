package com.hancekim.mynote.core.designsystem.block

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.hancekim.mynote.core.designfoundation.token.MyNoteRadius
import com.hancekim.mynote.core.designsystem.component.horizontalScrollFade
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal object NoteTableDefaults {
    val CellPaddingHorizontal = 12.dp
    val CellPaddingVertical = 8.dp
    val LineWidth = 1.dp
    val CornerRadius = MyNoteRadius.list
    val EdgeFade = 24.dp
    const val LINE_HEIGHT_EM = 1.5f
    const val TARGET_EM = 16f
    const val MINIMUM_EM = 6f
}

/** 표 블록. 열 폭은 칸 글자를 재서 정하고, 본문보다 넓으면 표만 가로로 스크롤한다. */
@Composable
internal fun NoteTable(
    table: NoteContentBlock.Table,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val body = MyNoteTheme.textStyles.body
    val cellStyle = remember(body) { body.copy(lineHeight = NoteTableDefaults.LINE_HEIGHT_EM.em) }
    val headStyle = remember(cellStyle) { cellStyle.copy(fontWeight = FontWeight.W600) }
    val em: Dp = with(LocalDensity.current) { body.fontSize.toDp() }
    val columns = maxOf(table.header.size, table.rows.maxOfOrNull { it.size } ?: 0)
    val rowCount = 1 + table.rows.size
    val scroll = rememberScrollState()
    val grid = remember { TableGridHolder() }
    Layout(
        content = {
            for (row in 0 until rowCount) {
                val cells = if (row == 0) table.header else table.rows[row - 1]
                for (column in 0 until columns) {
                    NoteSpansText(
                        spans = cells.getOrNull(column)?.spans ?: persistentListOf(),
                        style = if (row == 0) headStyle else cellStyle,
                        color = if (row == 0) colors.text else colors.bodyText,
                        keepCodeTogether = true,
                        textAlign = table.align.getOrNull(column).toTextAlign(),
                        modifier =
                            Modifier.semantics {
                                collectionItemInfo = CollectionItemInfo(row, 1, column, 1)
                                if (row == 0) heading()
                            },
                    )
                }
            }
        },
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScrollFade(scroll, NoteTableDefaults.EdgeFade, colors.bg)
                .horizontalScroll(scroll)
                .semantics { collectionInfo = CollectionInfo(rowCount, columns) }
                .tableGrid({ grid.value }, line = colors.border, head = colors.surface2),
    ) { measurables, constraints ->
        if (columns == 0) return@Layout layout(constraints.minWidth, 0) {}
        val padH = NoteTableDefaults.CellPaddingHorizontal.roundToPx()
        val padV = NoteTableDefaults.CellPaddingVertical.roundToPx()
        val line = NoteTableDefaults.LineWidth.roundToPx().coerceAtLeast(1)
        val longestLine = IntArray(columns)
        val longestWord = IntArray(columns)
        measurables.forEachIndexed { index, cell ->
            val column = index % columns
            longestLine[column] = maxOf(longestLine[column], cell.maxIntrinsicWidth(Constraints.Infinity))
            longestWord[column] = maxOf(longestWord[column], cell.minIntrinsicWidth(Constraints.Infinity))
        }
        val bodyWidth =
            if (constraints.hasBoundedWidth &&
                constraints.minWidth == 0
            ) {
                constraints.maxWidth
            } else {
                constraints.minWidth
            }
        val widths =
            tableColumnWidths(
                longestLine = longestLine,
                longestWord = longestWord,
                available = bodyWidth - line * (columns + 1) - padH * 2 * columns,
                targetCap = (em * NoteTableDefaults.TARGET_EM).roundToPx(),
                minimumFloor = (em * NoteTableDefaults.MINIMUM_EM).roundToPx(),
            )
        val placeables =
            measurables.mapIndexed { index, cell ->
                cell.measure(Constraints.fixedWidth(widths[index % columns]))
            }
        val columnX = IntArray(columns)
        var x = line
        for (column in 0 until columns) {
            columnX[column] = x
            x += widths[column] + padH * 2 + line
        }
        val rowY = IntArray(rowCount)
        var y = line
        for (row in 0 until rowCount) {
            rowY[row] = y
            val tallest = (0 until columns).maxOf { placeables[row * columns + it].height }
            y += tallest + padV * 2 + line
        }
        val next =
            TableGrid(
                width = x,
                height = y,
                columnLines = columnX.drop(1).map { it - line },
                rowLines = rowY.drop(1).map { it - line },
            )
        if (grid.value != next) grid.value = next
        val baseline = placeables[0][FirstBaseline]
        val alignment: Map<AlignmentLine, Int> =
            if (baseline == AlignmentLine.Unspecified) emptyMap() else mapOf(FirstBaseline to rowY[0] + padV + baseline)
        layout(maxOf(x, constraints.minWidth).coerceAtMost(constraints.maxWidth), y, alignment) {
            placeables.forEachIndexed { index, cell ->
                cell.place(columnX[index % columns] + padH, rowY[index / columns] + padV)
            }
        }
    }
}

/** 측정이 남긴 표 선 자리 — 그리기 단계만 읽는다. */
@Immutable
private data class TableGrid(
    val width: Int,
    val height: Int,
    val columnLines: List<Int>,
    val rowLines: List<Int>,
) {
    companion object {
        val Empty = TableGrid(0, 0, emptyList(), emptyList())
    }
}

@Stable
private class TableGridHolder {
    var value by mutableStateOf(TableGrid.Empty)
}

private fun Modifier.tableGrid(
    grid: () -> TableGrid,
    line: Color,
    head: Color,
): Modifier =
    drawWithCache {
        val current = grid()
        val stroke = NoteTableDefaults.LineWidth.toPx().coerceAtLeast(1f)
        val radius = NoteTableDefaults.CornerRadius.toPx()
        val width = current.width.toFloat()
        val height = current.height.toFloat()
        val outline = Path().apply { addRoundRect(RoundRect(0f, 0f, width, height, CornerRadius(radius))) }
        val headBottom = current.rowLines.firstOrNull()?.toFloat() ?: height
        onDrawBehind {
            if (current.width == 0) return@onDrawBehind
            clipPath(outline) {
                drawRect(color = head, size = Size(width, headBottom))
                current.columnLines.forEach { x -> drawRect(line, Offset(x.toFloat(), 0f), Size(stroke, height)) }
                current.rowLines.forEach { y -> drawRect(line, Offset(0f, y.toFloat()), Size(width, stroke)) }
            }
            drawRoundRect(
                color = line,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(width - stroke, height - stroke),
                cornerRadius = CornerRadius(radius - stroke / 2),
                style = Stroke(stroke),
            )
        }
    }

private fun NoteColumnAlign?.toTextAlign(): TextAlign =
    when (this) {
        NoteColumnAlign.Left -> TextAlign.Left
        NoteColumnAlign.Center -> TextAlign.Center
        NoteColumnAlign.Right -> TextAlign.Right
        NoteColumnAlign.None, null -> TextAlign.Start
    }

private fun previewCells(vararg texts: String): ImmutableList<NoteContentCell> =
    texts.map { NoteContentCell(persistentListOf(NoteContentSpan(it, NoteSpanStyle.Normal))) }.toImmutableList()

private val previewNarrow =
    NoteContentBlock.Table(
        header = previewCells("컬렉션", "순서", "평균 조회"),
        rows =
            persistentListOf(
                persistentListOf(
                    NoteContentCell(persistentListOf(NoteContentSpan("ArrayList", NoteSpanStyle.Code))),
                    NoteContentCell(persistentListOf(NoteContentSpan("있음", NoteSpanStyle.Normal))),
                    NoteContentCell(persistentListOf(NoteContentSpan("O(1)", NoteSpanStyle.Normal))),
                ),
                previewCells("HashSet", "없음", "O(1)"),
            ),
        align = persistentListOf(NoteColumnAlign.Left, NoteColumnAlign.Center, NoteColumnAlign.Right),
    )

private val previewWide =
    NoteContentBlock.Table(
        header = previewCells("기능", "LiveData", "StateFlow", "SharedFlow", "Channel"),
        rows =
            persistentListOf(
                previewCells("초기값", "없음", "필수", "없음", "없음"),
                previewCells("같은 값", "보냄", "거름", "보냄", "보냄"),
            ),
        align = persistentListOf(),
    )

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun NoteTablePreview() {
    MyNoteTheme {
        Column(
            modifier =
                Modifier
                    .background(MyNoteTheme.colors.bg)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NoteTable(previewNarrow)
            NoteTable(previewWide)
        }
    }
}
