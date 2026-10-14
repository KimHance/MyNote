package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.token.MyNoteRadius
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Composable
fun NoteRow(
    title: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bookmarked: Boolean = false,
    device: Boolean = false,
) {
    val colors = MyNoteTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MyNoteRadius.chip))
                .then(if (active) Modifier.background(colors.accentSoft) else Modifier)
                .clickable(onClick = onClick, role = Role.Button)
                .semantics { selected = active }
                .padding(start = 29.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style =
                MyNoteTheme.textStyles.treeLabel.copy(
                    fontWeight = if (active) FontWeight.W500 else FontWeight.W400,
                ),
            color = if (active) colors.accent else colors.text2,
            modifier = Modifier.weight(1f),
        )
        if (device) {
            Spacer(Modifier.width(8.dp))
            DeviceGlyph()
        }
        if (bookmarked) {
            Spacer(Modifier.width(if (device) 6.dp else 8.dp))
            BookmarkGlyph()
        }
    }
}

private data class NoteRowPreviewState(
    val active: Boolean,
    val bookmarked: Boolean,
    val device: Boolean = false,
)

private class NoteRowPreviewProvider : PreviewParameterProvider<NoteRowPreviewState> {
    override val values =
        sequenceOf(
            NoteRowPreviewState(active = false, bookmarked = false),
            NoteRowPreviewState(active = true, bookmarked = false),
            NoteRowPreviewState(active = false, bookmarked = true),
            NoteRowPreviewState(active = true, bookmarked = true),
            NoteRowPreviewState(active = false, bookmarked = true, device = true),
        )

    override fun getDisplayName(index: Int): String? =
        when (index) {
            0 -> "inactive"
            1 -> "active"
            2 -> "inactive · bookmarked"
            3 -> "active · bookmarked"
            else -> "inactive · bookmarked · device"
        }
}

@Preview(showBackground = true, backgroundColor = 0xFF161C24, widthDp = 280)
@Composable
private fun NoteRowPreview(
    @PreviewParameter(NoteRowPreviewProvider::class) state: NoteRowPreviewState,
) {
    MyNoteTheme {
        NoteRow(
            title = "Compose 상태 관리 정리 — 제목이 길어지면 글리프가 아니라 제목이 줄어든다",
            active = state.active,
            onClick = {},
            bookmarked = state.bookmarked,
            device = state.device,
        )
    }
}
