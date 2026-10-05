package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    hint: String? = null,
) {
    val colors = MyNoteTheme.colors
    val styles = MyNoteTheme.textStyles
    Row(modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() }) {
        Text(
            text = text,
            style = styles.overline,
            color = colors.text3,
            modifier = Modifier.alignByBaseline(),
        )
        if (count != null) {
            Spacer(Modifier.width(SIDE_GAP))
            Text(
                text = count.toString(),
                style = styles.monoSmall,
                color = colors.text3,
                modifier = Modifier.alignByBaseline(),
            )
        }
        if (hint != null) {
            Spacer(Modifier.width(SIDE_GAP))
            Text(
                text = hint,
                style = styles.caption.copy(fontWeight = FontWeight.W400),
                color = colors.text3,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

private val SIDE_GAP = 8.dp

@Preview(name = "SectionLabel · 개수", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun SectionLabelCountPreview() {
    MyNoteTheme {
        SectionLabel(text = "읽다 멈춘 노트", count = 3, modifier = Modifier.padding(16.dp))
    }
}

@Preview(name = "SectionLabel · 힌트", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun SectionLabelHintPreview() {
    MyNoteTheme {
        SectionLabel(text = "재도전 추이", hint = "첫 시도 → 최근", modifier = Modifier.padding(16.dp))
    }
}
