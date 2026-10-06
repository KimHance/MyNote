package com.hancekim.mynote.core.designsystem.block

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.token.MyNoteRadius
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun NoteContent(
    blocks: ImmutableList<NoteContentBlock>,
    modifier: Modifier = Modifier,
    state: NoteContentState = rememberNoteContentState(),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        blocks.forEachIndexed { index, block ->
            key(index) {
                if (block is NoteContentBlock.Unsupported) {
                    UnsupportedBlock(
                        block = block,
                        expanded = state.isExpanded(index),
                        showAll = state.isShowingAll(index),
                        onToggle = { state.toggle(index) },
                        onShowAll = { state.showAll(index) },
                    )
                } else {
                    NoteBlockContent(block = block, listDepth = 0, hang = 0)
                }
            }
        }
    }
}

/**
 * 블록 하나를 그린다. 목록 항목의 하위 블록도 이것으로 그린다.
 *
 * @param listDepth 이 블록을 감싼 목록 수
 * @param hang 감싼 목록의 기호 칸 폭(px) — 들여쓰기 한도를 넘은 하위 목록을 그만큼 당긴다
 */
@Composable
internal fun NoteBlockContent(
    block: NoteContentBlock,
    listDepth: Int,
    hang: Int,
) {
    when (block) {
        is NoteContentBlock.Heading -> {
            NoteSpansText(
                spans = block.spans,
                style =
                    if (block.level <= 2) {
                        MyNoteTheme.textStyles.mdHeading2
                    } else {
                        MyNoteTheme.textStyles.mdHeading3
                    },
                color = MyNoteTheme.colors.text,
            )
        }

        is NoteContentBlock.Paragraph -> {
            NoteSpansText(
                spans = block.spans,
                style = MyNoteTheme.textStyles.body,
                color = MyNoteTheme.colors.bodyText,
            )
        }

        is NoteContentBlock.Code -> {
            CodeBlock(block.code)
        }

        is NoteContentBlock.Quote -> {
            QuoteBlock(block.spans)
        }

        is NoteContentBlock.BulletList -> {
            NoteList(
                items = block.items,
                start = null,
                depth = listDepth + 1,
                modifier = if (stopsIndent(listDepth + 1)) Modifier.hangLeft(hang) else Modifier,
            )
        }

        is NoteContentBlock.OrderedList -> {
            NoteList(
                items = block.items,
                start = block.start,
                depth = listDepth + 1,
                modifier = if (stopsIndent(listDepth + 1)) Modifier.hangLeft(hang) else Modifier,
            )
        }

        is NoteContentBlock.Table -> {
            NoteTable(block)
        }

        NoteContentBlock.Divider -> {
            DividerBlock()
        }

        NoteContentBlock.Spacer -> {
            Spacer(Modifier.height(8.dp))
        }

        is NoteContentBlock.Unsupported -> {
            UnsupportedBlock(
                block = block.copy(raw = null),
                expanded = false,
                showAll = false,
                onToggle = {},
                onShowAll = {},
            )
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    Text(
        text = code,
        style = MyNoteTheme.textStyles.mono,
        color = MyNoteTheme.colors.text,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MyNoteRadius.list))
                .background(MyNoteTheme.colors.surface2)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
        softWrap = false,
    )
}

@Composable
private fun QuoteBlock(spans: ImmutableList<NoteContentSpan>) {
    val accent = MyNoteTheme.colors.accent
    NoteSpansText(
        spans = spans,
        style = MyNoteTheme.textStyles.body.copy(fontStyle = FontStyle.Italic),
        color = MyNoteTheme.colors.text2,
        uprightItalic = true,
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(color = accent, size = Size(3.dp.toPx(), size.height)) }
                .padding(start = 12.dp),
    )
}

@Composable
private fun DividerBlock() {
    val color = MyNoteTheme.colors.border
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(1.dp),
    ) {
        drawLine(
            color = color,
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = size.height,
        )
    }
}

private val previewBlocks: ImmutableList<NoteContentBlock> =
    persistentListOf(
        NoteContentBlock.Heading(2, persistentListOf(NoteContentSpan("섹션 제목", NoteSpanStyle.Normal))),
        NoteContentBlock.Paragraph(
            persistentListOf(
                NoteContentSpan("일반 텍스트와 ", NoteSpanStyle.Normal),
                NoteContentSpan("굵게", NoteSpanStyle.Bold),
                NoteContentSpan(" 그리고 ", NoteSpanStyle.Normal),
                NoteContentSpan("code", NoteSpanStyle.Code),
                NoteContentSpan(" 가 섞인 단락.", NoteSpanStyle.Normal),
            ),
        ),
        NoteContentBlock.Quote(persistentListOf(NoteContentSpan("인용 한 줄", NoteSpanStyle.Normal))),
        NoteContentBlock.Spacer,
        NoteContentBlock.Paragraph(persistentListOf(NoteContentSpan("spacer 로 한 줄 띄운 단락.", NoteSpanStyle.Normal))),
        NoteContentBlock.Divider,
        NoteContentBlock.Heading(3, persistentListOf(NoteContentSpan("divider 아래 새 섹션", NoteSpanStyle.Normal))),
        NoteContentBlock.BulletList(
            persistentListOf(
                NoteContentListItem(
                    spans = persistentListOf(NoteContentSpan("첫째", NoteSpanStyle.Normal)),
                    children =
                        persistentListOf(
                            NoteContentBlock.BulletList(
                                persistentListOf(
                                    NoteContentListItem(
                                        persistentListOf(NoteContentSpan("하위 항목", NoteSpanStyle.Normal)),
                                    ),
                                ),
                            ),
                        ),
                ),
                NoteContentListItem(persistentListOf(NoteContentSpan("둘째", NoteSpanStyle.Normal)), checked = true),
            ),
        ),
        NoteContentBlock.Paragraph(
            persistentListOf(
                NoteContentSpan("기울임", NoteSpanStyle.Normal, italic = true),
                NoteContentSpan(" 과 ", NoteSpanStyle.Normal),
                NoteContentSpan("취소선", NoteSpanStyle.Normal, strike = true),
            ),
        ),
        NoteContentBlock.Table(
            header =
                persistentListOf(
                    NoteContentCell(persistentListOf(NoteContentSpan("이름", NoteSpanStyle.Normal))),
                    NoteContentCell(persistentListOf(NoteContentSpan("값", NoteSpanStyle.Normal))),
                ),
            rows =
                persistentListOf(
                    persistentListOf(
                        NoteContentCell(persistentListOf(NoteContentSpan("x", NoteSpanStyle.Code))),
                        NoteContentCell(persistentListOf(NoteContentSpan("1", NoteSpanStyle.Normal))),
                    ),
                ),
            align = persistentListOf(NoteColumnAlign.None, NoteColumnAlign.Right),
        ),
        NoteContentBlock.Code("val x = 1"),
        NoteContentBlock.Unsupported(
            kind = UnsupportedKind.UnknownType,
            salvaged = "1. 클라이언트 → 서버: SYN\n2. 서버 → 클라이언트: SYN+ACK",
            raw = "{\n  \"type\": \"timeline\",\n  \"fallback\": \"…\"\n}",
        ),
        NoteContentBlock.Paragraph(persistentListOf(NoteContentSpan("상자 다음 단락.", NoteSpanStyle.Normal))),
    )

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun NoteContentPreview() {
    MyNoteTheme {
        NoteContent(
            blocks = previewBlocks,
            modifier =
                Modifier
                    .background(MyNoteTheme.colors.bg)
                    .padding(16.dp),
        )
    }
}
