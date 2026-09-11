package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.token.MyNoteRadius
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

/**
 * 홈 "최근 노트" 카드. 상단에 카테고리 칩 + 날짜, 아래에 제목.
 *
 * @param active 눌린/선택 상태이면 `surface-2` 배경
 */
@Composable
fun NoteCard(
    category: String,
    date: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val colors = MyNoteTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MyNoteRadius.card))
                .background(if (active) colors.surface2 else colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(MyNoteRadius.card))
                .clickable(onClick = onClick, role = Role.Button)
                .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryChip(category)
            Text(text = date, style = MyNoteTheme.textStyles.caption, color = colors.text3)
        }
        Spacer(Modifier.height(10.dp))
        Text(text = title, style = MyNoteTheme.textStyles.cardTitle, color = colors.text)
    }
}

private data class NoteCardPreviewState(
    val active: Boolean,
)

private class NoteCardPreviewProvider : PreviewParameterProvider<NoteCardPreviewState> {
    override val values =
        sequenceOf(
            NoteCardPreviewState(active = false),
            NoteCardPreviewState(active = true),
        )
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 320)
@Composable
private fun NoteCardPreview(
    @PreviewParameter(NoteCardPreviewProvider::class) state: NoteCardPreviewState,
) {
    MyNoteTheme {
        androidx.compose.foundation.layout.Box(Modifier.padding(16.dp)) {
            NoteCard(
                category = "Android",
                date = "2026.06.12",
                title = "Compose 상태 관리 정리",
                onClick = {},
                active = state.active,
            )
        }
    }
}
