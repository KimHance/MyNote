package com.hancekim.mynote.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.token.MyNoteRadius
import com.hancekim.mynote.core.designsystem.R
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Composable
fun NoteCard(
    category: String,
    date: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    chipModifier: Modifier = Modifier,
    dateModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    bookmarked: Boolean = false,
    device: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(MyNoteRadius.card)
    val bookmarkedLabel = stringResource(R.string.ds_bookmarked)
    val ribbonAlpha = remember { Animatable(if (bookmarked) 1f else 0f) }
    LaunchedEffect(bookmarked) {
        ribbonAlpha.animateTo(if (bookmarked) 1f else 0f, tween(RIBBON_FADE_MILLIS))
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, shape)
                    .clickable(onClick = onClick, role = Role.Button)
                    .semantics { if (bookmarked) stateDescription = bookmarkedLabel }
                    .padding(horizontal = 16.dp, vertical = 15.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = META_END_PADDING),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryChip(category, modifier = chipModifier.weight(1f, fill = false))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = date,
                    style = MyNoteTheme.textStyles.caption,
                    color = colors.text3,
                    maxLines = 1,
                    modifier = dateModifier,
                )
                if (device) {
                    Spacer(Modifier.width(8.dp))
                    DeviceOriginLabel()
                }
                trailing?.let {
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    it()
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MyNoteTheme.textStyles.cardTitle,
                color = colors.text,
                modifier = titleModifier,
            )
        }
        if (bookmarked || ribbonAlpha.isRunning) {
            BookmarkRibbon(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = -BookmarkRibbonDefaults.End, y = RIBBON_TOP_OFFSET)
                        .graphicsLayer { alpha = ribbonAlpha.value },
            )
        }
    }
}

private val META_END_PADDING = 24.dp
private val RIBBON_TOP_OFFSET = (-1).dp
private const val RIBBON_FADE_MILLIS = 160

private data class NoteCardPreviewState(
    val bookmarked: Boolean,
    val category: String,
    val device: Boolean = false,
)

private class NoteCardPreviewProvider : PreviewParameterProvider<NoteCardPreviewState> {
    override val values =
        sequenceOf(
            NoteCardPreviewState(bookmarked = false, category = "Android"),
            NoteCardPreviewState(bookmarked = true, category = "Android"),
            NoteCardPreviewState(bookmarked = true, category = "Android 아키텍처 컴포넌트"),
            NoteCardPreviewState(bookmarked = true, category = "Android 아키텍처 컴포넌트", device = true),
        )

    override fun getDisplayName(index: Int): String? =
        when (index) {
            0 -> "plain"
            1 -> "bookmarked"
            2 -> "bookmarked · long chip"
            else -> "bookmarked · long chip · device"
        }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 238)
@Composable
private fun NoteCardPreview(
    @PreviewParameter(NoteCardPreviewProvider::class) state: NoteCardPreviewState,
) {
    MyNoteTheme {
        Box(Modifier.padding(16.dp)) {
            NoteCard(
                category = state.category,
                date = "2026.06.12",
                title = "Compose 상태 관리 정리",
                onClick = {},
                bookmarked = state.bookmarked,
                device = state.device,
            )
        }
    }
}
