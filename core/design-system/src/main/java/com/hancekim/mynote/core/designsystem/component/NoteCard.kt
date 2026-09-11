package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.token.MyNoteRadius
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Composable
fun NoteCard(
    category: String,
    date: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MyNoteRadius.card))
                .background(colors.surface)
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

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 320)
@Composable
private fun NoteCardPreview() {
    MyNoteTheme {
        Box(Modifier.padding(16.dp)) {
            NoteCard(
                category = "Android",
                date = "2026.06.12",
                title = "Compose 상태 관리 정리",
                onClick = {},
            )
        }
    }
}
