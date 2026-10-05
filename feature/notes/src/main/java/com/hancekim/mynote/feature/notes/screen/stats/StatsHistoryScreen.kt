package com.hancekim.mynote.feature.notes.screen.stats

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hancekim.mynote.core.designsystem.component.fadingEdges
import com.hancekim.mynote.core.designsystem.motion.MyNoteEasing
import com.hancekim.mynote.core.designsystem.motion.rememberReduceMotion
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.stats.HistoryWeekUi
import com.hancekim.mynote.feature.notes.stats.StatsIntent
import com.hancekim.mynote.feature.notes.stats.StatsViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDate

@Composable
internal fun StatsHistoryRoute(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
    contentTopPadding: Dp = 0.dp,
    contentEndPadding: Dp = 0.dp,
    viewModel: StatsViewModel = hiltViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.onIntent(StatsIntent.Entered) }
    StatsHistoryScreen(
        history = state.history,
        listState = listState,
        modifier = modifier,
        contentBottomPadding = contentBottomPadding,
        contentTopPadding = contentTopPadding,
        contentEndPadding = contentEndPadding,
    )
}

@Composable
internal fun StatsHistoryScreen(
    history: ImmutableList<HistoryWeekUi>?,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentBottomPadding: Dp = 0.dp,
    contentTopPadding: Dp = 0.dp,
    contentEndPadding: Dp = 0.dp,
) {
    val adaptive = MyNoteTheme.adaptive
    val reading = adaptive.readingMaxWidth
    val formats = rememberStatsFormats()
    var expanded by rememberSaveable { mutableStateOf(listOf<Long>()) }
    Surface(modifier = modifier.fillMaxSize(), color = MyNoteTheme.colors.bg) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = listState,
                modifier =
                    (if (reading != null) Modifier.widthIn(max = reading) else Modifier.fillMaxWidth())
                        .fadingEdges(top = adaptive.fadeHeight, bottom = adaptive.fadeHeight + 6.dp),
                contentPadding =
                    PaddingValues(
                        start = adaptive.page,
                        end = adaptive.page + contentEndPadding,
                        top = adaptive.padY + contentTopPadding,
                        bottom = adaptive.page + contentBottomPadding,
                    ),
            ) {
                item(key = INTRO_KEY) {
                    Text(
                        text = stringResource(R.string.notes_stats_history_intro),
                        style = MyNoteTheme.textStyles.caption.copy(fontSize = 13.sp),
                        color = MyNoteTheme.colors.text2,
                        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
                    )
                }
                if (history != null) {
                    items(history, key = { it.weekStart.toEpochDay() }) { week ->
                        val key = week.weekStart.toEpochDay()
                        HistoryRow(
                            week = week,
                            expanded = key in expanded,
                            formats = formats,
                            onToggle = { expanded = if (key in expanded) expanded - key else expanded + key },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    week: HistoryWeekUi,
    expanded: Boolean,
    formats: StatsFormats,
    onToggle: () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val reduceMotion = rememberReduceMotion()
    val headline = week.headline
    val range = rangeText(week.weekStart, week.weekStart.plusDays(DAYS_IN_WEEK - 1), formats)
    val numbers =
        week.accuracy?.let { stringResource(R.string.notes_stats_history_line, week.notes, week.minutes, it) }
            ?: stringResource(R.string.notes_stats_history_line_noquiz, week.notes, week.minutes)
    val focusText = week.focus?.let { stringResource(R.string.notes_stats_focus_desc, it) }
    val generatedText = week.generatedOn?.let { stringResource(R.string.notes_stats_generated_date, formats.date(it)) }
    val opened = if (expanded) listOfNotNull(week.detail, focusText, generatedText) else emptyList()
    val description =
        (
            listOf(
                stringResource(
                    R.string.notes_stats_history_row_desc,
                    range,
                    headline ?: stringResource(R.string.notes_stats_history_none),
                ),
            ) + opened + numbers
        ).joinToString(SEPARATOR)
    val state = stringResource(if (expanded) R.string.notes_stats_expanded else R.string.notes_stats_collapsed)
    val turn by animateFloatAsState(
        targetValue = if (expanded) HALF_TURN else 0f,
        animationSpec = tween(if (reduceMotion) 0 else CHEVRON_MILLIS),
        label = "historyChevron",
    )
    Column(Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (headline != null) {
                            Modifier
                                .clickable(role = Role.Button, onClick = onToggle)
                                .semantics {
                                    contentDescription = description
                                    stateDescription = state
                                }
                        } else {
                            Modifier.semantics(mergeDescendants = true) { contentDescription = description }
                        },
                    ).padding(top = 14.dp, bottom = 15.dp)
                    .animateContentSize(tween(if (reduceMotion) 0 else EXPAND_MILLIS, easing = MyNoteEasing)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clearAndSetSemantics {}) {
                Text(
                    text = range,
                    style = MyNoteTheme.textStyles.monoSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.W400),
                    color = colors.text2,
                )
                if (week.current) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.notes_stats_period_week),
                        style = MyNoteTheme.textStyles.caption.copy(fontSize = 11.sp),
                        color = colors.accent,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.accent.copy(alpha = CHIP_TINT))
                                .padding(horizontal = 7.dp, vertical = 1.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (headline != null) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = colors.text3,
                        modifier = Modifier.size(CHEVRON_SIZE).graphicsLayer { rotationZ = turn },
                    )
                }
            }
            Column(Modifier.clearAndSetSemantics {}) {
                when {
                    headline == null -> {
                        Text(
                            text = stringResource(R.string.notes_stats_history_none),
                            style = MyNoteTheme.textStyles.bodySmall,
                            color = colors.text2,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }

                    expanded -> {
                        ExpandedSummary(week, headline, formats)
                    }

                    else -> {
                        Text(
                            text = headline,
                            style =
                                MyNoteTheme.textStyles.body.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.W500,
                                    lineHeight = 1.5.em,
                                ),
                            color = colors.text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                Text(
                    text = numbers,
                    style = MyNoteTheme.textStyles.monoSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.W400),
                    color = colors.text3,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        HorizontalDivider(color = colors.border)
    }
}

@Composable
private fun ExpandedSummary(
    week: HistoryWeekUi,
    headline: String,
    formats: StatsFormats,
) {
    val colors = MyNoteTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = headline,
            style =
                MyNoteTheme.textStyles.body.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W600,
                    lineHeight = 1.5.em,
                ),
            color = colors.text,
            modifier = Modifier.padding(top = 6.dp),
        )
        week.detail?.let {
            Text(
                text = it,
                style = MyNoteTheme.textStyles.body.copy(fontSize = 14.sp, lineHeight = 1.6.em),
                color = colors.bodyText,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            week.focus?.let {
                Text(
                    text = stringResource(R.string.notes_stats_focus),
                    style = MyNoteTheme.textStyles.caption.copy(fontSize = 12.sp),
                    color = colors.text3,
                )
                Spacer(Modifier.width(8.dp))
                FocusChip(it)
            }
            Spacer(Modifier.weight(1f))
            week.generatedOn?.let {
                Text(
                    text = stringResource(R.string.notes_stats_generated_date, formats.date(it)),
                    style = MyNoteTheme.textStyles.monoSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.W400),
                    color = colors.text3,
                )
            }
        }
    }
}

private const val INTRO_KEY = "intro"
private const val DAYS_IN_WEEK = 7L
private const val HALF_TURN = 180f
private const val CHEVRON_MILLIS = 200
private const val EXPAND_MILLIS = 240
private const val CHIP_TINT = 0.14f
private const val SEPARATOR = ", "
private val CHEVRON_SIZE = 16.dp

private val previewWeeks =
    persistentListOf(
        HistoryWeekUi(
            weekStart = LocalDate.of(2026, 9, 28),
            current = true,
            headline = "android와 kotlin을 고르게 읽었고, cs 노트는 이번 주에 한 번도 열지 않았습니다.",
            detail = "남은 사흘 중 하루는 cs 노트로 시작해 보세요.",
            focus = "cs",
            generatedOn = LocalDate.of(2026, 10, 1),
            notes = 7,
            minutes = 115,
            quizzes = 3,
            accuracy = 73,
        ),
        HistoryWeekUi(LocalDate.of(2026, 9, 21), false, null, null, null, null, 3, 40, 0, null),
    )

@Preview(name = "지난 요약 · Compact", widthDp = 380, heightDp = 640)
@Composable
private fun StatsHistoryPreview() {
    MyNoteTheme {
        StatsHistoryScreen(history = previewWeeks)
    }
}
