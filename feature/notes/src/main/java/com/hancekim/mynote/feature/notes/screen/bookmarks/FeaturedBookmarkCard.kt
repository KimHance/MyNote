package com.hancekim.mynote.feature.notes.screen.bookmarks

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.component.CategoryChip
import com.hancekim.mynote.core.designsystem.motion.MyNoteEasing
import com.hancekim.mynote.core.designsystem.motion.rememberReduceMotion
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.ui.screen.bookmarks.rememberBookmarkTimeLabel
import com.hancekim.mynote.feature.notes.BookmarkUi
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.R

internal enum class FeaturedVariant(
    val padTop: Dp,
    val padHorizontal: Dp,
    val padBottom: Dp,
    val titleSize: TextUnit,
    val titleLineHeight: TextUnit,
    val titleMaxLines: Int,
    val titleTop: Dp,
    val titleBottom: Dp,
    val excerptSize: TextUnit,
    val excerptLineHeight: TextUnit,
    val excerptMaxLines: Int,
) {
    Compact(18.dp, 18.dp, 20.dp, 21.sp, 28.35.sp, 2, 14.dp, 10.dp, 15.sp, 25.5.sp, 3),
    ExpandedOnePane(22.dp, 24.dp, 24.dp, 24.sp, 31.2.sp, 2, 16.dp, 10.dp, 16.sp, 28.sp, 3),
    TwoPane(22.dp, 24.dp, 26.dp, 26.sp, 33.8.sp, 2, 18.dp, 12.dp, 16.sp, 28.sp, 5),
    Short(14.dp, 16.dp, 16.dp, 18.sp, 24.3.sp, 1, 10.dp, 6.dp, 14.sp, 22.4.sp, 2),
}

internal fun featuredVariantFor(
    windowClass: MyNoteWindowClass,
    heightClass: MyNoteHeightClass,
    twoPane: Boolean,
): FeaturedVariant =
    when {
        heightClass == MyNoteHeightClass.Compact -> FeaturedVariant.Short
        twoPane -> FeaturedVariant.TwoPane
        windowClass == MyNoteWindowClass.Compact || windowClass == MyNoteWindowClass.Medium -> FeaturedVariant.Compact
        else -> FeaturedVariant.ExpandedOnePane
    }

@Composable
internal fun FeaturedBookmarkCard(
    bookmark: BookmarkUi,
    variant: FeaturedVariant,
    onClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(CARD_RADIUS)
    val reduceMotion = rememberReduceMotion()
    val crossfadeSpec: FiniteAnimationSpec<Float> = if (reduceMotion) snap() else tween(CROSSFADE_MILLIS)
    val sizeSpec: FiniteAnimationSpec<IntSize> =
        if (reduceMotion) snap() else tween(SIZE_MILLIS, easing = MyNoteEasing)
    val time = rememberBookmarkTimeLabel(bookmark.addedAt, long = true)
    val description =
        stringResource(R.string.notes_bookmark_featured_desc, bookmark.note.title, bookmark.categoryName, time)
    val noteId = bookmark.note.id

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.border, shape)
                .clickable(role = Role.Button) { onClick(noteId) }
                .semantics(mergeDescendants = true) { contentDescription = description }
                .animateContentSize(sizeSpec),
    ) {
        Crossfade(
            targetState = bookmark,
            animationSpec = crossfadeSpec,
            label = "featuredBookmark",
        ) { shown ->
            FeaturedContent(bookmark = shown, variant = variant, onRemove = { onRemove(shown.note.id) })
        }
    }
}

@Composable
private fun FeaturedContent(
    bookmark: BookmarkUi,
    variant: FeaturedVariant,
    onRemove: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val styles = MyNoteTheme.textStyles
    val time = rememberBookmarkTimeLabel(bookmark.addedAt, long = true)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = variant.padHorizontal, end = variant.padHorizontal)
                .padding(top = variant.padTop, bottom = variant.padBottom),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryChip(bookmark.categoryName, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(10.dp))
            Text(
                text = time,
                style = styles.monoSmall,
                color = colors.text3,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            RemoveBookmarkButton(iconSize = FEATURED_REMOVE_ICON, onClick = onRemove)
        }
        Spacer(Modifier.height(variant.titleTop))
        Text(
            text = bookmark.note.title,
            style =
                styles.title.copy(
                    fontSize = variant.titleSize,
                    lineHeight = variant.titleLineHeight,
                    fontWeight = FontWeight.W700,
                ),
            color = colors.text,
            maxLines = variant.titleMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
        val excerpt = bookmark.note.excerpt
        if (excerpt != null) {
            Spacer(Modifier.height(variant.titleBottom))
            Text(
                text = excerpt,
                style = styles.body.copy(fontSize = variant.excerptSize, lineHeight = variant.excerptLineHeight),
                color = colors.bodyText,
                maxLines = variant.excerptMaxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

private val CARD_RADIUS = 18.dp
private val FEATURED_REMOVE_ICON = 20.dp
private const val CROSSFADE_MILLIS = 200
private const val SIZE_MILLIS = 300

private class FeaturedExcerptProvider : PreviewParameterProvider<String?> {
    override val values =
        sequenceOf(
            "연결을 맺기 전에 SYN, SYN-ACK, ACK 세 번의 패킷을 주고받아 양쪽의 순서 번호를 맞춥니다.",
            null,
        )

    override fun getDisplayName(index: Int): String? = if (index == 0) "excerpt" else "no excerpt"
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun FeaturedBookmarkCardPreview(
    @PreviewParameter(FeaturedExcerptProvider::class) excerpt: String?,
) {
    MyNoteTheme {
        FeaturedBookmarkCard(
            bookmark = BookmarkUi(NoteUi("tcp", "TCP 3-way handshake", "2026.09.28", excerpt = excerpt), "네트워크", 0L),
            variant = FeaturedVariant.Compact,
            onClick = {},
            onRemove = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
