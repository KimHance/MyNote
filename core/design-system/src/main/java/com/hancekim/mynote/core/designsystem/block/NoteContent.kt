package com.hancekim.mynote.core.designsystem.block

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
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
                NoteBlockContent(block, index, state)
            }
        }
    }
}

@Composable
private fun NoteBlockContent(
    block: NoteContentBlock,
    index: Int,
    state: NoteContentState,
) {
    when (block) {
        is NoteContentBlock.Heading -> {
            Text(
                text = block.spans.toAnnotatedString(),
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
            Text(
                text = block.spans.toAnnotatedString(),
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
            ListBlock(block.items, ordered = false)
        }

        is NoteContentBlock.OrderedList -> {
            ListBlock(block.items, ordered = true)
        }

        NoteContentBlock.Divider -> {
            DividerBlock()
        }

        NoteContentBlock.Spacer -> {
            Spacer(Modifier.height(8.dp))
        }

        is NoteContentBlock.Unsupported -> {
            UnsupportedBlock(
                block = block,
                expanded = state.isExpanded(index),
                showAll = state.isShowingAll(index),
                onToggle = { state.toggle(index) },
                onShowAll = { state.showAll(index) },
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
    Text(
        text = spans.toAnnotatedString(),
        style = MyNoteTheme.textStyles.body.copy(fontStyle = FontStyle.Italic),
        color = MyNoteTheme.colors.text2,
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

@Composable
private fun ListBlock(items: ImmutableList<ImmutableList<NoteContentSpan>>, ordered: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEachIndexed { index, spans ->
            Row {
                Text(
                    text = if (ordered) "${index + 1}. " else "•  ",
                    style = MyNoteTheme.textStyles.body,
                    color = MyNoteTheme.colors.text2,
                )
                Text(
                    text = spans.toAnnotatedString(),
                    style = MyNoteTheme.textStyles.body,
                    color = MyNoteTheme.colors.bodyText,
                )
            }
        }
    }
}

@Composable
private fun ImmutableList<NoteContentSpan>.toAnnotatedString(): AnnotatedString {
    val colors = MyNoteTheme.colors
    return buildAnnotatedString {
        this@toAnnotatedString.forEach { span ->
            when (span.style) {
                NoteSpanStyle.Normal -> {
                    append(span.text)
                }

                NoteSpanStyle.Bold -> {
                    withStyle(
                        SpanStyle(fontWeight = FontWeight.W700, color = colors.text),
                    ) { append(span.text) }
                }

                NoteSpanStyle.Code -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = colors.surface2,
                            color = colors.accent,
                            baselineShift = BaselineShift(0.08f),
                        ),
                    ) { append(" ${span.text} ") }
                }
            }
        }
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
                persistentListOf(NoteContentSpan("첫째", NoteSpanStyle.Normal)),
                persistentListOf(NoteContentSpan("둘째", NoteSpanStyle.Normal)),
            ),
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
