package com.hancekim.mynote.feature.notes.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designsystem.component.CategoryRow
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.NoteRow
import com.hancekim.mynote.core.designsystem.component.PressIndication
import com.hancekim.mynote.core.designsystem.component.SearchField
import com.hancekim.mynote.core.designsystem.component.StatusRing
import com.hancekim.mynote.core.designsystem.component.fadingEdges
import com.hancekim.mynote.core.designsystem.component.throttledProcess
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.domain.model.ModelStatus
import com.hancekim.mynote.feature.notes.CategoryUi
import com.hancekim.mynote.feature.notes.NoteUi
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.NotesIntent
import com.hancekim.mynote.feature.notes.element.NotesUiState
import com.hancekim.mynote.feature.notes.element.fractionOrNull
import com.hancekim.mynote.feature.notes.element.isBusy
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

@Composable
internal fun ListPanelScreen(
    uiState: NotesUiState,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
    inFixedPanel: Boolean = false,
) {
    val colors = MyNoteTheme.colors
    val adaptive = MyNoteTheme.adaptive
    val listState = rememberLazyListState()
    val topFade by animateDpAsState(
        targetValue = if (listState.canScrollBackward) TOP_FADE else 0.dp,
        animationSpec = tween(FADE_MILLIS),
        label = "topFade",
    )
    val bottomFade by animateDpAsState(
        targetValue = if (listState.canScrollForward) BOTTOM_FADE else 0.dp,
        animationSpec = tween(FADE_MILLIS),
        label = "bottomFade",
    )

    Surface(modifier = modifier.fillMaxSize(), color = colors.surface) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(if (inFixedPanel) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
                    .padding(top = adaptive.panelHeaderPadTop),
        ) {
            Text(
                text = stringResource(R.string.notes_panel_count, uiState.totalCount),
                style = MyNoteTheme.textStyles.caption,
                color = colors.text3,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )
            Box(Modifier.padding(horizontal = 12.dp)) {
                SearchField(
                    query = uiState.query,
                    onQueryChange = { onIntent(NotesIntent.QueryChanged(it)) },
                )
            }
            Spacer(Modifier.height(12.dp))

            Box(Modifier.weight(1f)) {
                if (uiState.query.isNotBlank() && uiState.visibleCategories.isEmpty()) {
                    Text(
                        text = stringResource(R.string.notes_panel_empty),
                        style = MyNoteTheme.textStyles.body,
                        color = colors.text3,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 18.dp),
                        modifier = Modifier.fadingEdges(top = topFade, bottom = bottomFade),
                    ) {
                        uiState.visibleCategories.forEach { category ->
                            item(key = "cat-${category.name}") {
                                CategoryRow(
                                    name = category.name,
                                    count = category.notes.size,
                                    expanded = uiState.isExpanded(category.name),
                                    onClick = { onIntent(NotesIntent.ToggleCategory(category.name)) },
                                )
                            }
                            if (uiState.isExpanded(category.name)) {
                                items(category.notes, key = { "note-${it.id}" }) { note ->
                                    NoteRow(
                                        title = note.title,
                                        active = note.id == uiState.activeNoteId,
                                        onClick = { onIntent(NotesIntent.SelectNote(note.id)) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SettingsRow(
                selected = inFixedPanel && uiState.isSettingsOpen,
                tokenMissing = !uiState.hasToken,
                modelStatus = uiState.modelStatus,
                onClick = { onIntent(NotesIntent.OpenSettings) },
                modifier =
                    Modifier
                        .padding(horizontal = 10.dp, vertical = 0.dp)
                        .padding(top = 2.dp)
                        .then(
                            if (inFixedPanel) {
                                Modifier.padding(bottom = 12.dp)
                            } else {
                                Modifier
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .padding(bottom = 10.dp)
                            },
                        ),
            )
        }
    }
}

@Composable
private fun SettingsRow(
    selected: Boolean,
    tokenMissing: Boolean,
    modelStatus: ModelStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(ROW_RADIUS)
    val indication = remember(colors, shape) { PressIndication(shape, pressColor = colors.surface2) }
    val tint = if (selected) colors.accent else colors.bodyText
    val open = throttledProcess(action = onClick)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT)
                .clip(shape)
                .then(if (selected) Modifier.background(colors.accentSoft) else Modifier)
                .clickable(interactionSource = null, indication = indication, role = Role.Button, onClick = open)
                .padding(horizontal = 12.dp)
                .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = MyNoteIcons.Settings,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.notes_settings),
            style = MyNoteTheme.textStyles.treeLabel.copy(fontWeight = FontWeight.W500),
            color = tint,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (modelStatus.isBusy) {
            SettingsProgress(fraction = modelStatus.fractionOrNull(), warn = modelStatus is ModelStatus.Retrying)
        } else {
            val modelBadge =
                when {
                    modelStatus is ModelStatus.Failed -> R.string.notes_model_badge_failed
                    modelStatus == ModelStatus.Ready -> null
                    else -> R.string.notes_model_badge_missing
                }
            if (modelBadge != null) WarnBadge(text = stringResource(modelBadge))
            if (tokenMissing) WarnBadge(text = stringResource(R.string.notes_settings_token_missing_badge))
        }
    }
}

@Composable
private fun SettingsProgress(
    fraction: Float?,
    warn: Boolean,
) {
    val colors = MyNoteTheme.colors
    val tone = if (warn) colors.warn else colors.accent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatusRing(progress = fraction, tone = tone, diameter = 16.dp)
        if (fraction != null) {
            Text(
                text = stringResource(R.string.notes_model_percent, (fraction * 100).toInt()),
                style = MyNoteTheme.textStyles.monoSmall.copy(fontWeight = FontWeight.W600),
                color = colors.text2,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun WarnBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(BADGE_RADIUS)
    Text(
        text = text,
        style = MyNoteTheme.textStyles.caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight.W500),
        color = colors.warn,
        maxLines = 1,
        modifier =
            modifier
                .clip(shape)
                .background(colors.warn.copy(alpha = BADGE_FILL_ALPHA))
                .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private val TOP_FADE = 16.dp
private val BOTTOM_FADE = 52.dp
private val ROW_HEIGHT = 48.dp
private val ROW_RADIUS = 12.dp
private val BADGE_RADIUS = 7.dp
private const val FADE_MILLIS = 200
private const val BADGE_FILL_ALPHA = 0.09f

private val previewState =
    NotesUiState(
        categories =
            persistentListOf(
                CategoryUi("Android", persistentListOf(NoteUi("a", "Compose 상태 관리 정리", "2026.06.12"))),
                CategoryUi("Kotlin", persistentListOf(NoteUi("b", "코루틴 디스패처 메모", "2026.05.30"))),
            ),
        visibleCategories =
            persistentListOf(
                CategoryUi("Android", persistentListOf(NoteUi("a", "Compose 상태 관리 정리", "2026.06.12"))),
                CategoryUi("Kotlin", persistentListOf(NoteUi("b", "코루틴 디스패처 메모", "2026.05.30"))),
            ),
        activeNoteId = "a",
        expanded = persistentMapOf("Android" to true),
        isLoading = false,
    )

@Preview(name = "ListPanel · 토큰 없음", showBackground = true, backgroundColor = 0xFF161C24, widthDp = 300, heightDp = 520)
@Composable
private fun ListPanelScreenPreview() {
    MyNoteTheme {
        ListPanelScreen(uiState = previewState, onIntent = {})
    }
}

@Preview(
    name = "ListPanel · 카테고리 많음",
    showBackground = true,
    backgroundColor = 0xFF161C24,
    widthDp = 300,
    heightDp = 520,
)
@Composable
private fun ListPanelScreenManyPreview() {
    val many =
        (1..9).map { index ->
            CategoryUi(
                "분류 $index",
                persistentListOf(NoteUi("n$index", "노트 제목 $index", "2026.06.0$index")),
            )
        }
    MyNoteTheme {
        ListPanelScreen(
            uiState =
                previewState.copy(
                    categories = persistentListOf(*many.toTypedArray()),
                    visibleCategories = persistentListOf(*many.toTypedArray()),
                    expanded = persistentMapOf(*many.map { it.name to true }.toTypedArray()),
                    hasToken = true,
                ),
            onIntent = {},
        )
    }
}
