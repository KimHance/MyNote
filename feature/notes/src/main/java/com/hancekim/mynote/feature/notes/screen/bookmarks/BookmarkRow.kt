package com.hancekim.mynote.feature.notes.screen.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.PressIndication
import com.hancekim.mynote.core.designsystem.component.throttledProcess
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.BookmarkUi
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.R
import java.util.Locale

@Composable
internal fun BookmarkRow(
    bookmark: BookmarkUi,
    number: Int,
    compactHeight: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val styles = MyNoteTheme.textStyles
    val time = rememberBookmarkTimeLabel(bookmark.addedAt, long = false)
    val description =
        stringResource(R.string.notes_bookmark_row_desc, number, bookmark.note.title, bookmark.categoryName, time)
    val vertical = if (compactHeight) ROW_PAD_SHORT else ROW_PAD

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick, role = Role.Button)
                    .semantics(mergeDescendants = true) { contentDescription = description }
                    .padding(vertical = vertical)
                    .heightIn(min = REMOVE_TOUCH),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
        ) {
            Text(
                text = NUMBER_FORMAT.format(Locale.ROOT, number),
                style = styles.monoSmall.copy(fontSize = 13.sp),
                color = colors.text3,
                modifier = Modifier.widthIn(min = NUMBER_MIN_WIDTH),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.note.title,
                    style = styles.treeLabel.copy(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.W500),
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.notes_bookmark_row_sub, bookmark.categoryName, time),
                    style = styles.caption.copy(fontSize = 12.sp),
                    color = colors.text3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            RemoveBookmarkButton(iconSize = ROW_REMOVE_ICON, onClick = onRemove)
        }
        HorizontalDivider(color = colors.border, thickness = 1.dp)
    }
}

@Composable
internal fun RemoveBookmarkButton(
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(REMOVE_RADIUS)
    val indication = remember(colors, shape) { PressIndication(shape, pressColor = colors.border) }
    val remove = throttledProcess(action = onClick)
    val label = stringResource(R.string.notes_bookmark_remove)
    Box(
        modifier =
            modifier
                .size(iconSize)
                .wrapContentSize(unbounded = true),
    ) {
        Box(
            modifier =
                Modifier
                    .size(REMOVE_TOUCH)
                    .clip(shape)
                    .clickable(interactionSource = null, indication = indication, role = Role.Button, onClick = remove)
                    .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MyNoteIcons.BookmarkFilled,
                contentDescription = null,
                tint = colors.text2,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

private const val NUMBER_FORMAT = "%02d"
private val ROW_PAD = 12.dp
private val ROW_PAD_SHORT = 6.dp
private val ROW_GAP = 12.dp
private val NUMBER_MIN_WIDTH = 24.dp
private val ROW_REMOVE_ICON = 18.dp
private val REMOVE_TOUCH = 44.dp
private val REMOVE_RADIUS = 12.dp

private class BookmarkRowNumberProvider : PreviewParameterProvider<Int> {
    override val values = sequenceOf(2, 103)

    override fun getDisplayName(index: Int): String? = if (index == 0) "02" else "103"
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun BookmarkRowPreview(
    @PreviewParameter(BookmarkRowNumberProvider::class) number: Int,
) {
    MyNoteTheme {
        BookmarkRow(
            bookmark = BookmarkUi(NoteUi("a", "Compose 리컴포지션 범위 — 아주 길어서 한 줄에서 말줄임되는 제목", "2026.06.12"), "Android", 0L),
            number = number,
            compactHeight = false,
            onClick = {},
            onRemove = {},
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
