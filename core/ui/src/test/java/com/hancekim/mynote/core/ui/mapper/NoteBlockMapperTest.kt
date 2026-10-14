package com.hancekim.mynote.core.ui.mapper

import com.hancekim.mynote.core.designsystem.block.NoteColumnAlign
import com.hancekim.mynote.core.designsystem.block.NoteContentBlock
import com.hancekim.mynote.core.designsystem.block.NoteContentCell
import com.hancekim.mynote.core.designsystem.block.NoteContentListItem
import com.hancekim.mynote.core.designsystem.block.NoteContentSpan
import com.hancekim.mynote.core.designsystem.block.NoteSpanStyle
import com.hancekim.mynote.core.designsystem.block.UnsupportedKind
import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteLimits
import com.hancekim.mynote.core.domain.model.NoteListItem
import com.hancekim.mynote.core.domain.model.NoteSpan
import com.hancekim.mynote.core.domain.model.SpanStyle
import com.hancekim.mynote.core.domain.model.TableAlign
import com.hancekim.mynote.core.domain.model.TableCell
import com.hancekim.mynote.core.domain.model.UnsupportedReason
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun normal(text: String) = persistentListOf(NoteContentSpan(text, NoteSpanStyle.Normal))

private fun nested(depth: Int): NoteBlock =
    (depth - 1 downTo 1).fold(NoteBlock.BulletList(listOf(NoteListItem(listOf(NoteSpan("$depth")))))) { inner, level ->
        NoteBlock.BulletList(listOf(NoteListItem(listOf(NoteSpan("$level")), children = listOf(inner))))
    }

private fun depthOf(block: NoteContentBlock): Int {
    val items =
        when (block) {
            is NoteContentBlock.BulletList -> block.items
            is NoteContentBlock.OrderedList -> block.items
            else -> persistentListOf()
        }
    return 1 + (items.flatMap { it.children }.maxOfOrNull(::depthOf) ?: 0)
}

private fun containsBox(block: NoteContentBlock): Boolean =
    when (block) {
        is NoteContentBlock.Unsupported -> true
        is NoteContentBlock.BulletList -> block.items.any { item -> item.children.any(::containsBox) }
        is NoteContentBlock.OrderedList -> block.items.any { item -> item.children.any(::containsBox) }
        else -> false
    }

class NoteBlockMapperTest {
    private fun unsupported(
        reason: UnsupportedReason = UnsupportedReason.UnknownType,
        text: String? = "건진 글자",
        raw: String = "{\n  \"type\": \"timeline\"\n}",
    ) = NoteBlock.Unsupported(reason = reason, type = "timeline", text = text, raw = raw)

    @Test
    fun `이유 세 가지가 같은 이름의 UI 종류로 간다`() {
        val kinds =
            listOf(UnsupportedReason.UnknownType, UnsupportedReason.BrokenBlock, UnsupportedReason.BrokenNote)
                .map { reason ->
                    (
                        listOf(
                            unsupported(reason = reason),
                        ).toContentBlocks().single() as NoteContentBlock.Unsupported
                    ).kind
                }

        assertEquals(
            listOf(UnsupportedKind.UnknownType, UnsupportedKind.BrokenBlock, UnsupportedKind.BrokenNote),
            kinds,
        )
    }

    @Test
    fun `건진 글자와 원문은 그대로 옮긴다`() {
        val block = listOf(unsupported()).toContentBlocks().single() as NoteContentBlock.Unsupported

        assertEquals("건진 글자", block.salvaged)
        assertEquals("{\n  \"type\": \"timeline\"\n}", block.raw)
    }

    @Test
    fun `원문이 비어 있으면 null 이라 원문 보기 버튼이 없다`() {
        val block = listOf(unsupported(raw = "")).toContentBlocks().single() as NoteContentBlock.Unsupported

        assertNull(block.raw)
    }

    @Test
    fun `건진 글자가 없거나 공백뿐이면 null 이다`() {
        val missing = listOf(unsupported(text = null)).toContentBlocks().single() as NoteContentBlock.Unsupported
        val blank = listOf(unsupported(text = "  \n")).toContentBlocks().single() as NoteContentBlock.Unsupported

        assertNull(missing.salvaged)
        assertNull(blank.salvaged)
    }

    @Test
    fun `상자는 원래 블록 자리에 남고 앞뒤 블록 순서가 그대로다`() {
        val blocks =
            listOf(
                NoteBlock.Paragraph(listOf(NoteSpan("앞"))),
                unsupported(),
                NoteBlock.Code("뒤"),
            ).toContentBlocks()

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is NoteContentBlock.Paragraph)
        assertTrue(blocks[1] is NoteContentBlock.Unsupported)
        assertEquals(NoteContentBlock.Code("뒤"), blocks[2])
    }

    @Test
    fun `NF-4 겹친 모양을 화면 모델로 옮긴다`() {
        val blocks =
            listOf(
                NoteBlock.Paragraph(
                    listOf(
                        NoteSpan("a", SpanStyle.Bold, italic = true, strike = true),
                        NoteSpan("b", SpanStyle.Code, strike = true),
                        NoteSpan("c", italic = true),
                    ),
                ),
                NoteBlock.Quote(listOf(NoteSpan("q", italic = true))),
            ).toContentBlocks()

        assertEquals(
            listOf(
                NoteContentBlock.Paragraph(
                    persistentListOf(
                        NoteContentSpan("a", NoteSpanStyle.Bold, italic = true, strike = true),
                        NoteContentSpan("b", NoteSpanStyle.Code, strike = true),
                        NoteContentSpan("c", NoteSpanStyle.Normal, italic = true),
                    ),
                ),
                NoteContentBlock.Quote(persistentListOf(NoteContentSpan("q", NoteSpanStyle.Normal, italic = true))),
            ),
            blocks,
        )
    }

    @Test
    fun `NF-5 표를 화면 모델로 옮긴다`() {
        val table =
            listOf(
                NoteBlock.Table(
                    header =
                        listOf(
                            TableCell(listOf(NoteSpan("h1", SpanStyle.Bold))),
                            TableCell(listOf(NoteSpan("h2"))),
                        ),
                    rows = listOf(listOf(TableCell(listOf(NoteSpan("a"))), TableCell(emptyList()))),
                    align = listOf(TableAlign.Right, TableAlign.None),
                ),
                NoteBlock.Table(
                    listOf(TableCell(listOf(NoteSpan("h"))), TableCell(listOf(NoteSpan("l")))),
                    emptyList(),
                    listOf(TableAlign.Center, TableAlign.Left),
                ),
            ).toContentBlocks()

        assertEquals(
            listOf(
                NoteContentBlock.Table(
                    header =
                        persistentListOf(
                            NoteContentCell(persistentListOf(NoteContentSpan("h1", NoteSpanStyle.Bold))),
                            NoteContentCell(normal("h2")),
                        ),
                    rows =
                        persistentListOf(
                            persistentListOf(NoteContentCell(normal("a")), NoteContentCell(persistentListOf())),
                        ),
                    align = persistentListOf(NoteColumnAlign.Right, NoteColumnAlign.None),
                ),
                NoteContentBlock.Table(
                    persistentListOf(NoteContentCell(normal("h")), NoteContentCell(normal("l"))),
                    persistentListOf(),
                    persistentListOf(NoteColumnAlign.Center, NoteColumnAlign.Left),
                ),
            ),
            table,
        )
    }

    @Test
    fun `NF-6 하위 블록과 체크와 시작 번호를 화면 모델로 옮긴다`() {
        val list =
            NoteBlock.OrderedList(
                items =
                    listOf(
                        NoteListItem(
                            listOf(NoteSpan("a")),
                            checked = true,
                            children =
                                listOf(
                                    NoteBlock.BulletList(listOf(NoteListItem(listOf(NoteSpan("b")), checked = false))),
                                    NoteBlock.Code("x"),
                                ),
                        ),
                        NoteListItem(emptyList(), children = listOf(NoteBlock.Heading(1, listOf(NoteSpan("h"))))),
                    ),
                start = 9,
            )

        assertEquals(
            NoteContentBlock.OrderedList(
                items =
                    persistentListOf(
                        NoteContentListItem(
                            normal("a"),
                            checked = true,
                            children =
                                persistentListOf(
                                    NoteContentBlock.BulletList(
                                        persistentListOf(NoteContentListItem(normal("b"), checked = false)),
                                    ),
                                    NoteContentBlock.Code("x"),
                                ),
                        ),
                        NoteContentListItem(
                            persistentListOf(),
                            children = persistentListOf(NoteContentBlock.Heading(1, normal("h"))),
                        ),
                    ),
                start = 9,
            ),
            listOf(list).toContentBlocks().single(),
        )
        assertEquals(
            NoteContentBlock.BulletList(persistentListOf(NoteContentListItem(normal("c")))),
            listOf(NoteBlock.BulletList(listOf(NoteListItem(listOf(NoteSpan("c")))))).toContentBlocks().single(),
        )
    }

    @Test
    fun `NF-6 하위 블록의 상자는 건진 글자 문단이 되거나 빠진다`() {
        val list =
            NoteBlock.BulletList(
                listOf(
                    NoteListItem(
                        listOf(NoteSpan("a")),
                        children =
                            listOf(
                                unsupported(text = "건진 글자"),
                                unsupported(text = null),
                                NoteBlock.BulletList(
                                    listOf(NoteListItem(emptyList(), children = listOf(unsupported(text = " ")))),
                                ),
                            ),
                    ),
                ),
            )

        val mapped = listOf(list).toContentBlocks().single()

        assertEquals(
            NoteContentBlock.BulletList(
                persistentListOf(
                    NoteContentListItem(
                        normal("a"),
                        children =
                            persistentListOf(
                                NoteContentBlock.Paragraph(normal("건진 글자")),
                                NoteContentBlock.BulletList(persistentListOf(NoteContentListItem(persistentListOf()))),
                            ),
                    ),
                ),
            ),
            mapped,
        )
        assertFalse(containsBox(mapped))
    }

    @Test
    fun `NF-6 32단 하위 블록은 화면 매핑을 지난다`() {
        val mapped = listOf(nested(32)).toContentBlocks().single()

        assertEquals(32, depthOf(mapped))
    }

    @Test
    fun `NF-8 상자 문구의 숫자는 한도 값에서 온다`() {
        val limit = listOf(unsupported(reason = UnsupportedReason.LimitExceeded, text = null, raw = "- x"))

        val byDefault = limit.toContentBlocks().single() as NoteContentBlock.Unsupported
        val changed =
            limit
                .toContentBlocks(
                    NoteLimits(maxDepth = 7, maxKilobytes = 3, maxTableCells = 5),
                ).single() as NoteContentBlock.Unsupported

        assertEquals(
            UnsupportedKind.LimitExceeded(
                NoteLimits.Default.maxDepth,
                NoteLimits.Default.maxKilobytes,
                NoteLimits.Default.maxTableCells,
            ),
            byDefault.kind,
        )
        assertEquals(
            UnsupportedKind.LimitExceeded(maxDepth = 32, maxKilobytes = 512, maxTableCells = 4_096),
            byDefault.kind,
        )
        assertEquals(UnsupportedKind.LimitExceeded(maxDepth = 7, maxKilobytes = 3, maxTableCells = 5), changed.kind)
        assertEquals("- x", byDefault.raw)
        assertNull(byDefault.salvaged)
    }
}
