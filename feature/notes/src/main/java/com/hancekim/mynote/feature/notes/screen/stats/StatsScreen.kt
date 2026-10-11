package com.hancekim.mynote.feature.notes.screen.stats

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designsystem.component.fadingEdges
import com.hancekim.mynote.core.designsystem.motion.rememberReduceMotion
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.stats.BucketState
import com.hancekim.mynote.core.domain.stats.StatsPeriod
import com.hancekim.mynote.feature.notes.stats.AccuracyRowUi
import com.hancekim.mynote.feature.notes.stats.BookmarkBucketUi
import com.hancekim.mynote.feature.notes.stats.BookmarkStatsUi
import com.hancekim.mynote.feature.notes.stats.BucketLabel
import com.hancekim.mynote.feature.notes.stats.CategoryMinutesUi
import com.hancekim.mynote.feature.notes.stats.ComparisonUi
import com.hancekim.mynote.feature.notes.stats.DeltaUi
import com.hancekim.mynote.feature.notes.stats.PeriodNoteUi
import com.hancekim.mynote.feature.notes.stats.QuizStatsUi
import com.hancekim.mynote.feature.notes.stats.ReadingUi
import com.hancekim.mynote.feature.notes.stats.RetryUi
import com.hancekim.mynote.feature.notes.stats.StaleBookmarkUi
import com.hancekim.mynote.feature.notes.stats.StaleReasonUi
import com.hancekim.mynote.feature.notes.stats.StalledNoteUi
import com.hancekim.mynote.feature.notes.stats.StatKind
import com.hancekim.mynote.feature.notes.stats.StatNumberUi
import com.hancekim.mynote.feature.notes.stats.StatsContentUi
import com.hancekim.mynote.feature.notes.stats.StatsIntent
import com.hancekim.mynote.feature.notes.stats.StatsSection
import com.hancekim.mynote.feature.notes.stats.StatsUiState
import com.hancekim.mynote.feature.notes.stats.StatsViewModel
import com.hancekim.mynote.feature.notes.stats.TimeBarUi
import kotlinx.collections.immutable.persistentListOf
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
internal fun StatsRoute(
    twoPane: Boolean,
    listState: LazyListState,
    sideState: LazyListState,
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
    contentTopPadding: Dp = 0.dp,
    contentEndPadding: Dp = 0.dp,
    viewModel: StatsViewModel = hiltViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.onIntent(StatsIntent.Entered) }
    StatsScreen(
        state = state,
        twoPane = twoPane,
        onIntent = viewModel::onIntent,
        modifier = modifier,
        contentBottomPadding = contentBottomPadding,
        contentTopPadding = contentTopPadding,
        contentEndPadding = contentEndPadding,
        listState = listState,
        sideState = sideState,
    )
}

@Composable
internal fun StatsScreen(
    state: StatsUiState,
    twoPane: Boolean,
    onIntent: (StatsIntent) -> Unit,
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
    contentTopPadding: Dp = 0.dp,
    contentEndPadding: Dp = 0.dp,
    listState: LazyListState = rememberLazyListState(),
    sideState: LazyListState = rememberLazyListState(),
) {
    val reduceMotion = rememberReduceMotion()
    Surface(modifier = modifier.fillMaxSize(), color = MyNoteTheme.colors.bg) {
        val content = state.content
        if (content != null) {
            val padding = StatsPadding(bottom = contentBottomPadding, top = contentTopPadding, end = contentEndPadding)
            AnimatedContent(
                targetState = twoPane,
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else {
                        fadeIn(tween(PANE_FADE_IN_MILLIS, delayMillis = PANE_FADE_OUT_MILLIS)) togetherWith
                            fadeOut(tween(PANE_FADE_OUT_MILLIS))
                    }
                },
                contentAlignment = Alignment.TopCenter,
                label = "statsPanes",
            ) { two ->
                val scope = StatsScope(state, content, onIntent)
                if (two) {
                    StatsTwoPane(scope, listState, sideState, padding)
                } else {
                    StatsOnePane(scope, listState, padding)
                }
            }
        }
    }
}

@Immutable
private data class StatsPadding(
    val bottom: Dp,
    val top: Dp,
    val end: Dp,
)

@Immutable
private class StatsScope(
    val state: StatsUiState,
    val content: StatsContentUi,
    val onIntent: (StatsIntent) -> Unit,
)

@Composable
private fun StatsOnePane(
    scope: StatsScope,
    listState: LazyListState,
    padding: StatsPadding,
) {
    val adaptive = MyNoteTheme.adaptive
    val reading = adaptive.readingMaxWidth
    val compactHeight = adaptive.heightClass == MyNoteHeightClass.Compact
    val formats = rememberStatsFormats()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier =
                (if (reading != null) Modifier.widthIn(max = reading) else Modifier.fillMaxWidth())
                    .fadingEdges(top = adaptive.fadeHeight, bottom = adaptive.fadeHeight + 6.dp),
            contentPadding =
                PaddingValues(
                    start = adaptive.page,
                    end = adaptive.page + padding.end,
                    top = adaptive.padY + padding.top,
                    bottom = adaptive.page + padding.bottom,
                ),
        ) {
            headerItems(scope, formats, if (compactHeight) SummaryForm.Short else SummaryForm.Regular)
            chipsItem(scope)
            chartsItem(scope, if (compactHeight) ChartSize.Short else ChartSize.Regular, formats)
            listsItem(scope, compactHeight, first = false, formats = formats)
        }
    }
}

@Composable
private fun StatsTwoPane(
    scope: StatsScope,
    listState: LazyListState,
    sideState: LazyListState,
    padding: StatsPadding,
) {
    val adaptive = MyNoteTheme.adaptive
    val compactHeight = adaptive.heightClass == MyNoteHeightClass.Compact
    val formats = rememberStatsFormats()
    val paneModifier =
        Modifier
            .fillMaxHeight()
            .fadingEdges(top = adaptive.fadeHeight, bottom = adaptive.fadeHeight + 6.dp)
    val contentPadding =
        PaddingValues(top = adaptive.padY + padding.top, bottom = adaptive.page + padding.bottom)
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(start = adaptive.page, end = adaptive.page + padding.end),
        horizontalArrangement = Arrangement.spacedBy(if (compactHeight) PANE_GAP_SHORT else PANE_GAP),
    ) {
        LazyColumn(
            state = listState,
            modifier = paneModifier.weight(MAIN_WEIGHT),
            contentPadding = contentPadding,
        ) {
            headerItems(scope, formats, if (compactHeight) SummaryForm.Short else SummaryForm.Wide)
            if (!compactHeight) {
                chipsItem(scope)
                chartsItem(scope, ChartSize.Wide, formats)
            }
        }
        LazyColumn(
            state = sideState,
            modifier = paneModifier.weight(1f),
            contentPadding = contentPadding,
        ) {
            if (compactHeight) {
                chipsItem(scope, top = 0.dp, key = SIDE_CHIPS_KEY)
                chartsItem(scope, ChartSize.Short, formats, key = SIDE_CHARTS_KEY)
                listsItem(scope, compactHeight = true, first = false, formats = formats, key = SIDE_LISTS_KEY)
            } else {
                listsItem(scope, compactHeight = false, first = true, formats = formats)
            }
        }
    }
}

private fun LazyListScope.headerItems(
    scope: StatsScope,
    formats: StatsFormats,
    form: SummaryForm,
) {
    val state = scope.state
    val content = scope.content
    item(key = "segment") {
        Column(Modifier.fillMaxWidth()) {
            StatsNotice(logEnabled = state.logEnabled, intro = content.intro && state.summary == null)
            SummaryCardSlot(
                summary = state.summary,
                form = form,
                formats = formats,
                onOpenHistory = { scope.onIntent(StatsIntent.OpenHistory) },
            )
            PeriodSegment(selected = state.period, onSelect = { scope.onIntent(StatsIntent.SelectPeriod(it)) })
        }
    }
    content.periodNote?.let { note ->
        item(key = "period-note") { PeriodNoteText(note = note, formats = formats) }
    }
    item(key = "numbers") {
        NumberStrip(
            period = content.period,
            numbers = content.numbers,
            comparison = content.comparison,
            formats = formats,
        )
    }
}

private fun LazyListScope.chipsItem(
    scope: StatsScope,
    top: Dp = CHIPS_TOP,
    key: String = CHIPS_KEY,
) {
    item(key = key) {
        SectionChips(
            selected = scope.state.section,
            onSelect = { scope.onIntent(StatsIntent.SelectSection(it)) },
            modifier = Modifier.padding(top = top),
        )
    }
}

private fun LazyListScope.chartsItem(
    scope: StatsScope,
    size: ChartSize,
    formats: StatsFormats,
    key: String = CHARTS_KEY,
) {
    item(key = key) {
        val content = scope.content
        Crossfade(
            targetState = scope.state.section,
            animationSpec = tween(SECTION_FADE_MILLIS),
            label = "statsCharts",
            modifier = Modifier.padding(top = SECTION_TOP),
        ) { section ->
            Column(Modifier.fillMaxWidth()) {
                when (section) {
                    StatsSection.Reading -> {
                        ReadingCharts(
                            period = content.period,
                            reading = content.reading,
                            size = size,
                            formats = formats,
                            focus =
                                scope.state.summary
                                    ?.takeIf { it.focusInLegend }
                                    ?.focus,
                        )
                    }

                    StatsSection.Quiz -> {
                        QuizCharts(content.period, content.quiz)
                    }

                    StatsSection.Bookmarks -> {
                        BookmarkCharts(content.period, content.bookmarks, formats)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.listsItem(
    scope: StatsScope,
    compactHeight: Boolean,
    first: Boolean,
    formats: StatsFormats,
    key: String = LISTS_KEY,
) {
    item(key = key) {
        val content = scope.content
        Crossfade(
            targetState = scope.state.section,
            animationSpec = tween(SECTION_FADE_MILLIS),
            label = "statsLists",
        ) { section ->
            Column(Modifier.fillMaxWidth()) {
                when (section) {
                    StatsSection.Reading -> {
                        ReadingList(content.reading, compactHeight, first, scope.onIntent)
                    }

                    StatsSection.Quiz -> {
                        QuizList(content.quiz, scope.state.pattern, formats, compactHeight, first, scope.onIntent)
                    }

                    StatsSection.Bookmarks -> {
                        BookmarkList(content.bookmarks, compactHeight, first, scope.onIntent)
                    }
                }
            }
        }
    }
}

private const val CHIPS_KEY = "chips"
private const val CHARTS_KEY = "charts"
private const val LISTS_KEY = "lists"
private const val SIDE_CHIPS_KEY = "side-chips"
private const val SIDE_CHARTS_KEY = "side-charts"
private const val SIDE_LISTS_KEY = "side-lists"
private val CHIPS_TOP = 18.dp
private val SECTION_TOP = 14.dp
private val PANE_GAP = 28.dp
private val PANE_GAP_SHORT = 24.dp
private const val MAIN_WEIGHT = 1.15f
private const val SECTION_FADE_MILLIS = 150
private const val PANE_FADE_OUT_MILLIS = 160
private const val PANE_FADE_IN_MILLIS = 200

private const val PREVIEW_NOW = 1_790_000_000_000L
private const val PREVIEW_DAY = 86_400_000L

private val previewWeekBars =
    persistentListOf(
        TimeBarUi(BucketLabel.Day(DayOfWeek.MONDAY), 35, BucketState.Past),
        TimeBarUi(BucketLabel.Day(DayOfWeek.TUESDAY), 12, BucketState.Past),
        TimeBarUi(BucketLabel.Day(DayOfWeek.WEDNESDAY), 48, BucketState.Past),
        TimeBarUi(BucketLabel.Day(DayOfWeek.THURSDAY), 20, BucketState.Current),
        TimeBarUi(BucketLabel.Day(DayOfWeek.FRIDAY), 0, BucketState.Future),
        TimeBarUi(BucketLabel.Day(DayOfWeek.SATURDAY), 0, BucketState.Future),
        TimeBarUi(BucketLabel.Day(DayOfWeek.SUNDAY), 0, BucketState.Future),
    )

private val previewContent =
    StatsContentUi(
        period = StatsPeriod.Week,
        intro = false,
        periodNote = null,
        numbers =
            persistentListOf(
                StatNumberUi(StatKind.Notes, 7, DeltaUi.Value(2)),
                StatNumberUi(StatKind.Minutes, 115, DeltaUi.Value(30)),
                StatNumberUi(StatKind.Quizzes, 3, DeltaUi.Value(-1)),
                StatNumberUi(StatKind.Accuracy, 73, DeltaUi.Value(8)),
            ),
        comparison = ComparisonUi.Week(DayOfWeek.THURSDAY),
        reading =
            ReadingUi(
                bars = previewWeekBars,
                legend =
                    persistentListOf(
                        CategoryMinutesUi("android", 0, 38),
                        CategoryMinutesUi("kotlin", 4, 32),
                        CategoryMinutesUi("compose", 1, 31),
                        CategoryMinutesUi("datastructure", 3, 14),
                        CategoryMinutesUi("cs", 2, 0),
                    ),
                categoryTotal = 115,
                stalled =
                    persistentListOf(
                        StalledNoteUi("hash", "해시 테이블 충돌 처리", "datastructure", 40, PREVIEW_NOW - PREVIEW_DAY),
                        StalledNoteUi("room", "Room 마이그레이션", "android", 65, PREVIEW_NOW - 9 * PREVIEW_DAY),
                    ),
            ),
        quiz =
            QuizStatsUi(
                asked = 15,
                rows =
                    persistentListOf(
                        AccuracyRowUi("android", 6, 50, lowest = true),
                        AccuracyRowUi("kotlin", 5, 80, lowest = false),
                        AccuracyRowUi("compose", 4, 100, lowest = false),
                        AccuracyRowUi("cs", 0, null, lowest = false),
                    ),
                retries = persistentListOf(RetryUi("room", "Room 마이그레이션", "android", 33, 67, 3)),
            ),
        bookmarks =
            BookmarkStatsUi(
                buckets =
                    previewWeekBars
                        .mapIndexed { index, bar ->
                            BookmarkBucketUi(
                                bar.label,
                                if (index ==
                                    0
                                ) {
                                    2
                                } else {
                                    0
                                },
                                if (index == 2) 1 else 0,
                                bar.state,
                            )
                        }.let { persistentListOf(*it.toTypedArray()) },
                added = 2,
                removed = 1,
                stale =
                    persistentListOf(
                        StaleBookmarkUi("tcp", "TCP 3-way handshake", "cs", StaleReasonUi.LastOpened(30), 41),
                    ),
            ),
    )

private val previewIntro =
    previewContent.copy(
        intro = true,
        numbers =
            persistentListOf(
                StatNumberUi(StatKind.Notes, 0, DeltaUi.None),
                StatNumberUi(StatKind.Minutes, 0, DeltaUi.None),
                StatNumberUi(StatKind.Quizzes, 0, DeltaUi.None),
                StatNumberUi(StatKind.Accuracy, null, DeltaUi.None),
            ),
        comparison = ComparisonUi.Pending,
        reading =
            ReadingUi(
                bars =
                    previewWeekBars
                        .map {
                            it.copy(
                                minutes = 0,
                                state =
                                    if (it.state ==
                                        BucketState.Current
                                    ) {
                                        it.state
                                    } else {
                                        BucketState.BeforeRecord
                                    },
                            )
                        }.let { persistentListOf(*it.toTypedArray()) },
                legend = persistentListOf(CategoryMinutesUi("android", 0, 0), CategoryMinutesUi("kotlin", 1, 0)),
                categoryTotal = 0,
                stalled = persistentListOf(),
            ),
    )

private val previewShort =
    previewContent.copy(
        period = StatsPeriod.Month,
        periodNote = PeriodNoteUi(LocalDate.of(2026, 9, 25), 7),
        comparison = ComparisonUi.Since(LocalDate.of(2026, 9, 25)),
    )

@Preview(name = "Stats · Compact · 정상", showBackground = true, widthDp = 360, heightDp = 1400)
@Composable
private fun StatsCompactPreview() {
    MyNoteTheme {
        StatsScreen(state = StatsUiState(content = previewContent), twoPane = false, onIntent = {})
    }
}

@Preview(name = "Stats · Compact · 데이터 0", showBackground = true, widthDp = 360, heightDp = 1200)
@Composable
private fun StatsIntroPreview() {
    MyNoteTheme {
        StatsScreen(state = StatsUiState(content = previewIntro), twoPane = false, onIntent = {})
    }
}

@Preview(name = "Stats · Compact · 14일 미만 · 기록 꺼짐", showBackground = true, widthDp = 360, heightDp = 1200)
@Composable
private fun StatsShortPreview() {
    MyNoteTheme {
        StatsScreen(
            state = StatsUiState(period = StatsPeriod.Month, logEnabled = false, content = previewShort),
            twoPane = false,
            onIntent = {},
        )
    }
}

@Preview(name = "Stats · Compact · 퀴즈", showBackground = true, widthDp = 360, heightDp = 1000)
@Composable
private fun StatsQuizPreview() {
    MyNoteTheme {
        StatsScreen(
            state = StatsUiState(section = StatsSection.Quiz, content = previewContent),
            twoPane = false,
            onIntent = {},
        )
    }
}

@Preview(name = "Stats · 852 2단", showBackground = true, widthDp = 852, heightDp = 883)
@Composable
private fun StatsTwoPanePreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Expanded) {
        StatsScreen(state = StatsUiState(content = previewContent), twoPane = true, onIntent = {})
    }
}

@Preview(name = "Stats · 가로 2단", showBackground = true, widthDp = 891, heightDp = 411)
@Composable
private fun StatsLandscapePreview() {
    MyNoteTheme(windowClass = MyNoteWindowClass.Expanded, heightClass = MyNoteHeightClass.Compact) {
        StatsScreen(state = StatsUiState(content = previewContent), twoPane = true, onIntent = {})
    }
}
