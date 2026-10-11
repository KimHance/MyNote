package com.hancekim.mynote.feature.notes.screen.bookmarks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.fadingEdges
import com.hancekim.mynote.core.designsystem.component.throttledProcess
import com.hancekim.mynote.core.designsystem.motion.MyNoteEasing
import com.hancekim.mynote.core.designsystem.motion.MyNoteExitEasing
import com.hancekim.mynote.core.designsystem.motion.rememberReduceMotion
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.navigation.NotesTab
import com.hancekim.mynote.core.ui.screen.SecondaryButton
import com.hancekim.mynote.feature.notes.BookmarkUi
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.screen.emptyStateTitle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun BookmarksScreen(
    bookmarks: ImmutableList<BookmarkUi>,
    isLoading: Boolean,
    twoPane: Boolean,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
    contentTopPadding: Dp = 0.dp,
    contentEndPadding: Dp = 0.dp,
    listState: LazyListState = rememberLazyListState(),
) {
    val colors = MyNoteTheme.colors
    val reduceMotion = rememberReduceMotion()

    Surface(modifier = modifier.fillMaxSize(), color = colors.bg) {
        Box(contentAlignment = Alignment.TopCenter) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.accent,
                )
            } else {
                AnimatedContent(
                    targetState = bookmarks,
                    contentKey = { it.isEmpty() },
                    transitionSpec = {
                        if (reduceMotion) {
                            fadeIn(snap()) togetherWith fadeOut(snap())
                        } else {
                            fadeIn(tween(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS)) togetherWith
                                fadeOut(tween(FADE_OUT_MILLIS, easing = MyNoteExitEasing))
                        }
                    },
                    contentAlignment = Alignment.TopCenter,
                    label = "bookmarksEmpty",
                ) { shown ->
                    if (shown.isEmpty()) {
                        BookmarksEmpty(
                            topPadding = contentTopPadding,
                            endPadding = contentEndPadding,
                            onPickFromHome = { onIntent(NotesIntent.SwitchTab(NotesTab.Home)) },
                        )
                    } else {
                        val padding =
                            BookmarksPadding(
                                bottom = contentBottomPadding,
                                top = contentTopPadding,
                                end = contentEndPadding,
                            )
                        if (twoPane) {
                            BookmarksTwoPane(shown, onIntent, listState, padding)
                        } else {
                            BookmarksOnePane(shown, onIntent, listState, padding)
                        }
                    }
                }
            }
        }
    }
}

@Immutable
private data class BookmarksPadding(
    val bottom: Dp,
    val top: Dp,
    val end: Dp,
)

@Immutable
private data class RowMotion(
    val fadeIn: FiniteAnimationSpec<Float>,
    val fadeOut: FiniteAnimationSpec<Float>,
    val placement: FiniteAnimationSpec<IntOffset>,
)

@Composable
private fun rememberRowMotion(): RowMotion {
    val reduceMotion = rememberReduceMotion()
    return remember(reduceMotion) {
        if (reduceMotion) {
            RowMotion(snap(), snap(), snap())
        } else {
            RowMotion(
                fadeIn = tween(FADE_IN_MILLIS, easing = MyNoteEasing),
                fadeOut = tween(FADE_OUT_MILLIS, easing = MyNoteExitEasing),
                placement = tween(PLACEMENT_MILLIS, easing = MyNoteEasing),
            )
        }
    }
}

@Composable
private fun BookmarksOnePane(
    bookmarks: ImmutableList<BookmarkUi>,
    onIntent: (NotesIntent) -> Unit,
    listState: LazyListState,
    padding: BookmarksPadding,
) {
    val adaptive = MyNoteTheme.adaptive
    val reading = adaptive.readingMaxWidth
    val variant = featuredVariantFor(adaptive.windowClass, adaptive.heightClass, twoPane = false)
    val compactHeight = adaptive.heightClass == MyNoteHeightClass.Compact
    val motion = rememberRowMotion()

    LazyColumn(
        state = listState,
        modifier =
            (if (reading != null) Modifier.widthIn(max = reading) else Modifier)
                .fadingEdges(top = adaptive.fadeHeight, bottom = adaptive.fadeHeight + 6.dp),
        contentPadding =
            PaddingValues(
                start = adaptive.page,
                end = adaptive.page + padding.end,
                top = adaptive.padY + padding.top,
                bottom = adaptive.page + padding.bottom,
            ),
    ) {
        item(key = FEATURED_KEY) {
            Column(Modifier.fillMaxWidth()) {
                SectionLabel(text = stringResource(R.string.notes_bookmark_latest))
                Spacer(Modifier.height(LABEL_GAP))
                FeaturedBookmarkCard(
                    bookmark = bookmarks[0],
                    variant = variant,
                    onClick = { onIntent(NotesIntent.SelectNote(it)) },
                    onRemove = { onIntent(NotesIntent.RemoveBookmark(it)) },
                )
            }
        }
        if (bookmarks.size >= 2) {
            item(key = EARLIER_KEY) {
                EarlierLabel(
                    count = bookmarks.size - 1,
                    modifier =
                        Modifier
                            .animateItem(
                                fadeInSpec = motion.fadeIn,
                                placementSpec = motion.placement,
                                fadeOutSpec = motion.fadeOut,
                            ).padding(top = EARLIER_TOP, bottom = EARLIER_BOTTOM),
                )
            }
            earlierRows(bookmarks, compactHeight, motion, onIntent)
        }
    }
}

@Composable
private fun BookmarksTwoPane(
    bookmarks: ImmutableList<BookmarkUi>,
    onIntent: (NotesIntent) -> Unit,
    listState: LazyListState,
    padding: BookmarksPadding,
) {
    val adaptive = MyNoteTheme.adaptive
    val compactHeight = adaptive.heightClass == MyNoteHeightClass.Compact
    val variant = featuredVariantFor(adaptive.windowClass, adaptive.heightClass, twoPane = true)
    val motion = rememberRowMotion()

    val top = adaptive.padY + padding.top
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(start = adaptive.page, end = adaptive.page + padding.end),
        horizontalArrangement = Arrangement.spacedBy(if (compactHeight) PANE_GAP_SHORT else PANE_GAP),
    ) {
        Column(Modifier.weight(FEATURED_WEIGHT).padding(top = top)) {
            SectionLabel(text = stringResource(R.string.notes_bookmark_latest))
            Spacer(Modifier.height(LABEL_GAP))
            FeaturedBookmarkCard(
                bookmark = bookmarks[0],
                variant = variant,
                onClick = { onIntent(NotesIntent.SelectNote(it)) },
                onRemove = { onIntent(NotesIntent.RemoveBookmark(it)) },
            )
        }
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .fadingEdges(top = adaptive.fadeHeight, bottom = adaptive.fadeHeight + 6.dp),
            contentPadding = PaddingValues(top = top, bottom = adaptive.page + padding.bottom),
        ) {
            if (bookmarks.size >= 2) {
                item(key = EARLIER_KEY) {
                    EarlierLabel(
                        count = bookmarks.size - 1,
                        modifier =
                            Modifier
                                .animateItem(
                                    fadeInSpec = motion.fadeIn,
                                    placementSpec = motion.placement,
                                    fadeOutSpec = motion.fadeOut,
                                ).padding(bottom = EARLIER_BOTTOM),
                    )
                }
                earlierRows(bookmarks, compactHeight, motion, onIntent)
            }
        }
    }
}

private fun LazyListScope.earlierRows(
    bookmarks: ImmutableList<BookmarkUi>,
    compactHeight: Boolean,
    motion: RowMotion,
    onIntent: (NotesIntent) -> Unit,
) {
    items(count = bookmarks.size - 1, key = { bookmarks[it + 1].note.id }) { index ->
        val bookmark = bookmarks[index + 1]
        BookmarkRow(
            bookmark = bookmark,
            number = index + 2,
            compactHeight = compactHeight,
            onClick = { onIntent(NotesIntent.SelectNote(bookmark.note.id)) },
            onRemove = { onIntent(NotesIntent.RemoveBookmark(bookmark.note.id)) },
            modifier =
                Modifier.animateItem(
                    fadeInSpec = motion.fadeIn,
                    placementSpec = motion.placement,
                    fadeOutSpec = motion.fadeOut,
                ),
        )
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MyNoteTheme.textStyles.overline,
        color = MyNoteTheme.colors.text3,
        modifier = modifier,
    )
}

@Composable
private fun EarlierLabel(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.notes_bookmark_earlier),
            style = MyNoteTheme.textStyles.overline,
            color = colors.text3,
            modifier = Modifier.alignByBaseline(),
        )
        Spacer(Modifier.width(COUNT_GAP))
        Text(
            text = count.toString(),
            style = MyNoteTheme.textStyles.monoSmall,
            color = colors.text3,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

@Composable
private fun BookmarksEmpty(
    topPadding: Dp,
    endPadding: Dp,
    onPickFromHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val adaptive = MyNoteTheme.adaptive
    val tileShape = RoundedCornerShape(EMPTY_TILE_RADIUS)
    val gap =
        if (adaptive.heightClass == MyNoteHeightClass.Compact) EMPTY_TOP_SHORT else adaptive.padY + EMPTY_TOP_EXTRA
    val pick = throttledProcess(action = onPickFromHome)

    Column(
        modifier =
            modifier
                .padding(end = endPadding)
                .padding(start = 8.dp, end = 8.dp, top = gap + topPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(EMPTY_TILE)
                    .clip(tileShape)
                    .background(colors.surface2)
                    .border(1.dp, colors.border, tileShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MyNoteIcons.Bookmark,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(EMPTY_ICON),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.notes_bookmark_empty_title),
            style = MyNoteTheme.textStyles.emptyStateTitle(),
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.notes_bookmark_empty_body),
            style = MyNoteTheme.textStyles.body.copy(fontSize = 14.sp, lineHeight = 23.sp),
            color = colors.text2,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = EMPTY_BODY_MAX_WIDTH),
        )
        Spacer(Modifier.height(20.dp))
        Box(Modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
            SecondaryButton(text = stringResource(R.string.notes_bookmark_empty_action), onClick = pick)
        }
    }
}

private const val FEATURED_KEY = "featured"
private const val EARLIER_KEY = "earlier"
private const val FEATURED_WEIGHT = 1.15f
private const val FADE_OUT_MILLIS = 180
private const val PLACEMENT_MILLIS = 300
private const val FADE_IN_MILLIS = 240
private val LABEL_GAP = 12.dp
private val EARLIER_TOP = 28.dp
private val EARLIER_BOTTOM = 4.dp
private val COUNT_GAP = 8.dp
private val PANE_GAP = 28.dp
private val PANE_GAP_SHORT = 24.dp
private val EMPTY_TILE = 52.dp
private val EMPTY_TILE_RADIUS = 15.dp
private val EMPTY_ICON = 24.dp
private val EMPTY_TOP_SHORT = 4.dp
private val EMPTY_TOP_EXTRA = 40.dp
private val EMPTY_BODY_MAX_WIDTH = 300.dp

private const val PREVIEW_DAY_MILLIS = 86_400_000L
private const val PREVIEW_NOW = 1_790_000_000_000L

private val previewBookmarks =
    persistentListOf(
        BookmarkUi(
            NoteUi(
                "tcp",
                "TCP 3-way handshake",
                "2026.09.28",
                excerpt = "연결을 맺기 전에 SYN, SYN-ACK, ACK 세 번의 패킷을 주고받아 양쪽의 순서 번호를 맞춥니다.",
            ),
            "네트워크",
            PREVIEW_NOW,
        ),
        BookmarkUi(NoteUi("compose", "Compose 리컴포지션 범위", "2026.06.12"), "Android", PREVIEW_NOW - 3_600_000L),
        BookmarkUi(NoteUi("coroutine", "코루틴 구조화된 동시성", "2026.05.30"), "Kotlin", PREVIEW_NOW - PREVIEW_DAY_MILLIS),
        BookmarkUi(NoteUi("http", "HTTP 캐시 헤더", "2026.04.22"), "네트워크", PREVIEW_NOW - 4 * PREVIEW_DAY_MILLIS),
        BookmarkUi(NoteUi("room", "Room 마이그레이션 메모", "2025.12.01"), "Android", PREVIEW_NOW - 400 * PREVIEW_DAY_MILLIS),
    )

private val previewSingle = persistentListOf(previewBookmarks[0])

@Preview(name = "Bookmarks · Compact · 5", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun BookmarksScreenCompactPreview() {
    MyNoteTheme {
        BookmarksScreen(bookmarks = previewBookmarks, isLoading = false, twoPane = false, onIntent = {})
    }
}

@Preview(name = "Bookmarks · Compact · 1", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun BookmarksScreenCompactSinglePreview() {
    MyNoteTheme {
        BookmarksScreen(bookmarks = previewSingle, isLoading = false, twoPane = false, onIntent = {})
    }
}

@Preview(name = "Bookmarks · Compact · 빈 상태", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun BookmarksScreenCompactEmptyPreview() {
    MyNoteTheme {
        BookmarksScreen(bookmarks = persistentListOf(), isLoading = false, twoPane = false, onIntent = {})
    }
}

@Preview(name = "Bookmarks · Expanded 한 단", showBackground = true, widthDp = 852, heightDp = 883)
@Composable
private fun BookmarksScreenExpandedOnePanePreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Expanded) {
        BookmarksScreen(bookmarks = previewBookmarks, isLoading = false, twoPane = false, onIntent = {})
    }
}

@Preview(name = "Bookmarks · 2단 · 5", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun BookmarksScreenTwoPanePreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Large) {
        BookmarksScreen(bookmarks = previewBookmarks, isLoading = false, twoPane = true, onIntent = {})
    }
}

@Preview(name = "Bookmarks · 2단 · 1", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun BookmarksScreenTwoPaneSinglePreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Large) {
        BookmarksScreen(bookmarks = previewSingle, isLoading = false, twoPane = true, onIntent = {})
    }
}

@Preview(name = "Bookmarks · 2단 · 빈 상태", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun BookmarksScreenTwoPaneEmptyPreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Large) {
        BookmarksScreen(bookmarks = persistentListOf(), isLoading = false, twoPane = true, onIntent = {})
    }
}

@Preview(name = "Bookmarks · 가로 한 단", showBackground = true, widthDp = 891, heightDp = 411)
@Composable
private fun BookmarksScreenLandscapeOnePanePreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Expanded, heightClass = MyNoteHeightClass.Compact) {
        BookmarksScreen(bookmarks = previewBookmarks, isLoading = false, twoPane = false, onIntent = {})
    }
}

@Preview(name = "Bookmarks · 가로 2단", showBackground = true, widthDp = 891, heightDp = 411)
@Composable
private fun BookmarksScreenLandscapeTwoPanePreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Expanded, heightClass = MyNoteHeightClass.Compact) {
        BookmarksScreen(bookmarks = previewBookmarks, isLoading = false, twoPane = true, onIntent = {})
    }
}

@Preview(name = "Bookmarks · 가로 빈 상태", showBackground = true, widthDp = 891, heightDp = 411)
@Composable
private fun BookmarksScreenLandscapeEmptyPreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Expanded, heightClass = MyNoteHeightClass.Compact) {
        BookmarksScreen(bookmarks = persistentListOf(), isLoading = false, twoPane = false, onIntent = {})
    }
}
