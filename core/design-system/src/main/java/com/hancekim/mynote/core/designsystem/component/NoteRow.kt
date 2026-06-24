package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.token.MyNoteRadius
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

/**
 * 목록 패널의 노트 행(카테고리 아래 들여쓰기). 활성 노트는 accent 글자색 + accent-soft 배경.
 */
@Composable
fun NoteRow(
    title: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    Text(
        text = title,
        style = MyNoteTheme.textStyles.treeLabel.copy(
            fontWeight = if (active) FontWeight.W500 else FontWeight.W400,
        ),
        color = if (active) colors.accent else colors.text2,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MyNoteRadius.chip))
            .then(if (active) Modifier.background(colors.accentSoft) else Modifier)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(start = 29.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
    )
}

private data class NoteRowPreviewState(val active: Boolean)

private class NoteRowPreviewProvider : PreviewParameterProvider<NoteRowPreviewState> {
    override val values = sequenceOf(
        NoteRowPreviewState(active = false),
        NoteRowPreviewState(active = true),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF161C24, widthDp = 280)
@Composable
private fun NoteRowPreview(
    @PreviewParameter(NoteRowPreviewProvider::class) state: NoteRowPreviewState,
) {
    MyNoteTheme {
        NoteRow(title = "Compose 상태 관리 정리", active = state.active, onClick = {})
    }
}
