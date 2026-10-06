package com.hancekim.mynote.core.designsystem.block

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.R
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

internal object NoteListDefaults {
    val ItemGap = 6.dp
    const val CHECK_CELL_EM = 1.625f
    const val CHECK_ICON_EM = 1.25f
    const val VISIBLE_DEPTH = 3
}

/**
 * 글머리 · 번호 목록. 기호 칸은 그 목록의 기호를 실제로 잰 최댓값이고, 항목 글과 하위 블록은 칸 끝에서 시작한다.
 *
 * @param start 번호 목록의 첫 번호, 글머리 목록이면 null
 * @param depth 목록 중첩 깊이(1부터)
 */
@Composable
internal fun NoteList(
    items: ImmutableList<NoteContentListItem>,
    start: Int?,
    depth: Int,
    modifier: Modifier = Modifier,
) {
    val metrics = rememberListMetrics(items.size, start, depth, hasCheck = items.any { it.checked != null })
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(NoteListDefaults.ItemGap)) {
        items.forEachIndexed { index, item ->
            ListItem(item = item, number = start?.let { it.toLong() + index }, depth = depth, metrics = metrics)
        }
    }
}

/** 깊이(1부터)마다 도는 글머리. */
internal fun bulletGlyph(depth: Int): String = BULLETS[(depth - 1).mod(BULLETS.size)]

/** 목록 깊이(1부터)가 보이는 들여쓰기 한도를 넘는지. */
internal fun stopsIndent(depth: Int): Boolean = depth > NoteListDefaults.VISIBLE_DEPTH

private val BULLETS = listOf("•", "◦", "▪")

private fun bulletMarker(depth: Int): String = "${bulletGlyph(depth)}  "

@Immutable
private data class ListMetrics(
    val cell: Int,
    val numberWidth: Int,
    val checkX: Int,
    val checkSize: Int,
    val lineBaseline: Int,
    val lineHeight: Int,
)

@Composable
private fun rememberListMetrics(
    itemCount: Int,
    start: Int?,
    depth: Int,
    hasCheck: Boolean,
): ListMetrics {
    val body = MyNoteTheme.textStyles.body
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(itemCount, start, depth, hasCheck, body, density) {
        fun width(text: String): Int = measurer.measure(text, body).size.width
        val em = with(density) { body.fontSize.toDp().toPx() }
        val checkCell = if (hasCheck) (em * NoteListDefaults.CHECK_CELL_EM).roundToInt() else 0
        val line = measurer.measure(LINE_SAMPLE, body)
        val checkSize = (em * NoteListDefaults.CHECK_ICON_EM).roundToInt()
        if (start == null) {
            ListMetrics(
                cell = maxOf(width(bulletMarker(depth)), checkCell),
                numberWidth = 0,
                checkX = 0,
                checkSize = checkSize,
                lineBaseline = line.firstBaseline.roundToInt(),
                lineHeight = line.size.height,
            )
        } else {
            val first = start.toLong()
            val last = first + (itemCount - 1).coerceAtLeast(0)
            val numberCell = maxOf(width("$first. "), width("$last. "))
            ListMetrics(
                cell = numberCell + checkCell,
                numberWidth = maxOf(width("$first."), width("$last.")),
                checkX = numberCell,
                checkSize = checkSize,
                lineBaseline = line.firstBaseline.roundToInt(),
                lineHeight = line.size.height,
            )
        }
    }
}

@Composable
private fun ListItem(
    item: NoteContentListItem,
    number: Long?,
    depth: Int,
    metrics: ListMetrics,
) {
    val colors = MyNoteTheme.colors
    val body = MyNoteTheme.textStyles.body
    val checked = item.checked
    val checkState =
        checked?.let { stringResource(if (it) R.string.ds_block_checked else R.string.ds_block_unchecked) }
    val iconSize = with(LocalDensity.current) { metrics.checkSize.toDp() }
    val hasText = item.spans.isNotEmpty()
    Layout(
        contents =
            listOf(
                {
                    if (number != null || checked == null) {
                        Text(
                            text = if (number != null) "$number." else bulletMarker(depth),
                            style = body,
                            color = colors.text2,
                            softWrap = false,
                            maxLines = 1,
                        )
                    }
                    if (checked != null) {
                        Icon(
                            imageVector = if (checked) MyNoteIcons.CheckBoxChecked else MyNoteIcons.CheckBox,
                            contentDescription = if (hasText) null else checkState,
                            tint = colors.text2,
                            modifier = Modifier.size(iconSize),
                        )
                    }
                },
                {
                    Column(verticalArrangement = Arrangement.spacedBy(NoteListDefaults.ItemGap)) {
                        if (hasText) {
                            NoteSpansText(
                                spans = item.spans,
                                style = body,
                                color = colors.bodyText,
                                modifier =
                                    if (checkState != null) {
                                        Modifier.semantics { stateDescription = checkState }
                                    } else {
                                        Modifier
                                    },
                            )
                        }
                        item.children.forEach { child ->
                            NoteBlockContent(block = child, listDepth = depth, hang = metrics.cell)
                        }
                    }
                },
            ),
    ) { (markerMeasurables, contentMeasurables), constraints ->
        val contentMax = if (constraints.hasBoundedWidth) constraints.maxWidth - metrics.cell else Constraints.Infinity
        val content = contentMeasurables.firstOrNull()?.measure(Constraints(maxWidth = contentMax.coerceAtLeast(0)))
        val markers = markerMeasurables.map { it.measure(Constraints()) }
        val iconIndex = if (checked != null) markers.lastIndex else -1
        val contentBaseline =
            content?.get(FirstBaseline)?.takeIf { it != AlignmentLine.Unspecified } ?: metrics.lineBaseline
        val markerTops =
            IntArray(markers.size) { index ->
                if (index == iconIndex) {
                    contentBaseline - metrics.lineBaseline + (metrics.lineHeight - markers[index].height) / 2
                } else {
                    contentBaseline - markers[index][FirstBaseline]
                }
            }
        val shift = maxOf(0, -(markerTops.minOrNull() ?: 0))
        val contentHeight = content?.height ?: 0
        val markerBottom = markers.indices.maxOfOrNull { markerTops[it] + shift + markers[it].height } ?: 0
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else metrics.cell + (content?.width ?: 0)
        layout(width, maxOf(shift + contentHeight, markerBottom), mapOf(FirstBaseline to shift + contentBaseline)) {
            markers.forEachIndexed { index, marker ->
                val x =
                    when {
                        index == iconIndex -> metrics.checkX
                        number != null -> metrics.numberWidth - marker.width
                        else -> 0
                    }
                marker.placeRelative(x, markerTops[index] + shift)
            }
            content?.placeRelative(metrics.cell, shift)
        }
    }
}

/** 주어진 폭(px)만큼 시작 쪽으로 내어 놓고, 그만큼 넓혀 잰다. */
internal fun Modifier.hangLeft(amount: Int): Modifier =
    layout { measurable, constraints ->
        if (amount <= 0 || !constraints.hasBoundedWidth) {
            val placeable = measurable.measure(constraints)
            return@layout layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }
        val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + amount))
        layout((placeable.width - amount).coerceIn(constraints.minWidth, constraints.maxWidth), placeable.height) {
            placeable.placeRelative(-amount, 0)
        }
    }

private const val LINE_SAMPLE = "가"

private fun previewItem(
    text: String,
    checked: Boolean? = null,
    children: ImmutableList<NoteContentBlock> = persistentListOf(),
): NoteContentListItem =
    NoteContentListItem(
        persistentListOf(NoteContentSpan(text, NoteSpanStyle.Normal)),
        checked,
        children,
    )

private val previewDeep: NoteContentBlock.BulletList =
    (5 downTo 1)
        .fold(previewItem("5단")) { inner, level ->
            if (level == 5) {
                inner
            } else {
                previewItem(
                    "${level}단",
                    children = persistentListOf(NoteContentBlock.BulletList(persistentListOf(inner))),
                )
            }
        }.let { NoteContentBlock.BulletList(persistentListOf(it)) }

private val previewChecks: NoteContentBlock.OrderedList =
    NoteContentBlock.OrderedList(
        items =
            persistentListOf(
                previewItem("빌드 통과", checked = true),
                previewItem("기기에서 열기", checked = false),
            ),
        start = 9,
    )

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun NoteListPreview() {
    MyNoteTheme {
        Column(
            modifier =
                Modifier
                    .background(MyNoteTheme.colors.bg)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NoteList(previewDeep.items, start = null, depth = 1)
            NoteList(previewChecks.items, start = previewChecks.start, depth = 1)
        }
    }
}
