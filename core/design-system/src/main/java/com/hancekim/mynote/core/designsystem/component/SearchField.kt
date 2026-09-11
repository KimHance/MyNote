package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.token.MyNoteRadius
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "노트 검색",
) {
    val colors = MyNoteTheme.colors
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MyNoteTheme.textStyles.body.copy(color = colors.text),
        cursorBrush = SolidColor(colors.accent),
        modifier =
            modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(colors.surface2, RoundedCornerShape(MyNoteRadius.list)),
        decorationBox = { innerTextField ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MyNoteTheme.textStyles.body,
                        color = colors.text3,
                    )
                }
                innerTextField()
            }
        },
    )
}

private class SearchFieldPreviewProvider : PreviewParameterProvider<String> {
    override val values = sequenceOf("", "compose")
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 300)
@Composable
private fun SearchFieldPreview(
    @PreviewParameter(SearchFieldPreviewProvider::class) query: String,
) {
    MyNoteTheme {
        Box(Modifier.padding(12.dp)) {
            SearchField(query = query, onQueryChange = {})
        }
    }
}
