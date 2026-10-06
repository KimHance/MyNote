package com.hancekim.mynote.core.designsystem.block

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.hancekim.mynote.core.designsystem.R
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test

class NoteContentSemanticsTest {
    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun plain(text: String) = persistentListOf(NoteContentSpan(text, NoteSpanStyle.Normal))

    private fun cell(text: String) = NoteContentCell(plain(text))

    private fun show(vararg blocks: NoteContentBlock) {
        val list: ImmutableList<NoteContentBlock> = persistentListOf(*blocks)
        rule.setContent { MyNoteTheme { NoteContent(list) } }
    }

    @Test
    fun `NF-6 체크 항목의 글이 켬과 끔을 상태 설명으로 싣는다`() {
        show(
            NoteContentBlock.BulletList(
                persistentListOf(
                    NoteContentListItem(plain("빌드 통과"), checked = true),
                    NoteContentListItem(plain("기기에서 열기"), checked = false),
                    NoteContentListItem(plain("일반 항목")),
                ),
            ),
        )

        val checked = context.getString(R.string.ds_block_checked)
        val unchecked = context.getString(R.string.ds_block_unchecked)
        rule.onNodeWithText("빌드 통과").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, checked))
        rule
            .onNodeWithText(
                "기기에서 열기",
            ).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, unchecked))
        rule.onNodeWithText("일반 항목").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun `NF-6 글이 빈 체크 항목은 상자가 상태를 읽는다`() {
        show(
            NoteContentBlock.OrderedList(
                persistentListOf(NoteContentListItem(persistentListOf(), checked = true)),
                start = 3,
            ),
        )

        rule.onNodeWithContentDescription(context.getString(R.string.ds_block_checked)).assertExists()
    }

    @Test
    fun `NF-5 표는 행과 열 수를 싣고 머리 칸은 제목이다`() {
        show(
            NoteContentBlock.Table(
                header = persistentListOf(cell("이름"), cell("값")),
                rows = persistentListOf(persistentListOf(cell("x"), cell("1")), persistentListOf(cell("y"))),
                align = persistentListOf(),
            ),
        )

        rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo))
            .assert(
                SemanticsMatcher("행 3 · 열 2") { node ->
                    val info = node.config[SemanticsProperties.CollectionInfo]
                    info.rowCount == 3 && info.columnCount == 2
                },
            )
        rule.onNodeWithText("값").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("1").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("1").assert(
            SemanticsMatcher("행 1 · 열 1") { node ->
                val info = node.config[SemanticsProperties.CollectionItemInfo]
                info.rowIndex == 1 && info.columnIndex == 1
            },
        )
    }

    @Test
    fun `NF-4 취소선 구간이 있는 글은 낭독 글자에 시작과 끝을 싣는다`() {
        show(
            NoteContentBlock.Paragraph(
                persistentListOf(
                    NoteContentSpan("a ", NoteSpanStyle.Normal),
                    NoteContentSpan("b", NoteSpanStyle.Normal, strike = true),
                    NoteContentSpan(" c", NoteSpanStyle.Normal),
                ),
            ),
        )

        val start = context.getString(R.string.ds_block_strike_start)
        val end = context.getString(R.string.ds_block_strike_end)
        rule.onNodeWithContentDescription("a $start b $end c").assertExists()
    }
}
