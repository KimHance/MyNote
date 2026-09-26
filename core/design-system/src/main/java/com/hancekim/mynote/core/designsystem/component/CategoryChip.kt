package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.token.MyNoteRadius
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Composable
fun CategoryChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MyNoteTheme.textStyles.caption,
        color = MyNoteTheme.colors.accent,
        modifier =
            modifier
                .background(MyNoteTheme.colors.accentSoft, RoundedCornerShape(MyNoteRadius.chip))
                .border(1.dp, MyNoteTheme.colors.accentSoftBorder, RoundedCornerShape(MyNoteRadius.chip))
                .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217)
@Composable
private fun CategoryChipPreview() {
    MyNoteTheme {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryChip("Android")
            CategoryChip("Kotlin")
            CategoryChip("CS")
        }
    }
}
