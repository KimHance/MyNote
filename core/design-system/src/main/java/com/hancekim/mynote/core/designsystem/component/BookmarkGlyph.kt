package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.R
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Composable
fun BookmarkGlyph(
    modifier: Modifier = Modifier,
    size: Dp = BookmarkGlyphDefaults.Size,
) {
    Icon(
        imageVector = MyNoteIcons.BookmarkFilled,
        contentDescription = stringResource(R.string.ds_bookmarked),
        tint = MyNoteTheme.colors.text3,
        modifier = modifier.size(size),
    )
}

object BookmarkGlyphDefaults {
    val Size: Dp = 12.dp
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217)
@Composable
private fun BookmarkGlyphPreview() {
    MyNoteTheme {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("TCP 3-way handshake", style = MyNoteTheme.textStyles.treeLabel, color = MyNoteTheme.colors.text2)
            BookmarkGlyph()
        }
    }
}
