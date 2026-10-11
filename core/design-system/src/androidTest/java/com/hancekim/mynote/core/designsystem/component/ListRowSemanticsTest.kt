package com.hancekim.mynote.core.designsystem.component

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.hancekim.mynote.core.designsystem.R
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import org.junit.Rule
import org.junit.Test

class ListRowSemanticsTest {
    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `범주 행은 펼침과 접힘을 상태 설명으로 싣고 꺾쇠는 읽지 않는다`() {
        rule.setContent {
            MyNoteTheme {
                CategoryRow(name = "android", count = 3, expanded = true, onClick = {})
                CategoryRow(name = "kotlin", count = 2, expanded = false, onClick = {})
            }
        }

        rule
            .onNodeWithText("android", useUnmergedTree = false)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    context.getString(R.string.ds_state_expanded),
                ),
            )
        rule
            .onNodeWithText("kotlin", useUnmergedTree = false)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    context.getString(R.string.ds_state_collapsed),
                ),
            )
        rule.onAllNodesWithText("▸", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `지금 읽는 노트 행만 선택된 상태다`() {
        rule.setContent {
            MyNoteTheme {
                NoteRow(title = "읽는 중", active = true, onClick = {})
                NoteRow(title = "다른 노트", active = false, onClick = {})
            }
        }

        rule.onNodeWithText("읽는 중").assertIsSelected()
        rule.onNodeWithText("다른 노트").assertIsNotSelected()
    }
}
