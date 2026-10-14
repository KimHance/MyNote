package com.hancekim.mynote.feature.notes.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.ui.screen.PrimaryButton
import com.hancekim.mynote.core.ui.screen.QuietButton
import com.hancekim.mynote.core.ui.screen.SecondaryButton
import com.hancekim.mynote.core.ui.screen.connect.ConnectIcons
import com.hancekim.mynote.core.ui.screen.connect.KeepWordsText
import com.hancekim.mynote.core.ui.screen.connect.ProgressBar
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.EntryWeight
import com.hancekim.mynote.feature.notes.element.HomeAction
import com.hancekim.mynote.feature.notes.element.HomeBody
import com.hancekim.mynote.feature.notes.element.HomeEntry
import com.hancekim.mynote.feature.notes.element.NotesIntent
import kotlinx.collections.immutable.ImmutableList
import com.hancekim.mynote.core.ui.R as UiR

/**
 * 노트가 없는 홈 — 첫 실행 빈 상태(LN-1)와 연결 있음의 두 빈 상태(RC-21). 히어로 없이 본문 가운데 묶음 하나이고,
 * 넘치면(글자 ×1.3) 묶음째 세로로 스크롤한다. 플로팅 바 · 레일 자리는 뺀 영역의 가운데다.
 */
@Composable
internal fun HomeEmpty(
    body: HomeBody,
    entries: ImmutableList<HomeEntry>,
    onIntent: (NotesIntent) -> Unit,
    endPadding: Dp,
    topPadding: Dp,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(top = topPadding, bottom = bottomPadding, end = endPadding),
        contentAlignment = Alignment.Center,
    ) {
        val column = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 16.dp)
        when (body) {
            HomeBody.FirstRun -> {
                FirstRunColumn(entries = entries, onIntent = onIntent, modifier = column)
            }

            HomeBody.Receiving -> {
                EmptyColumn(
                    icon = ConnectIcons.Download,
                    title = R.string.notes_home_receiving_title,
                    desc = R.string.notes_home_receiving_desc,
                    modifier = column.semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    ProgressBar(Modifier.size(width = 120.dp, height = 4.dp))
                    Spacer(Modifier.height(ENTRY_ROW_GAP))
                    HomeEntryRow(entries = entries, onIntent = onIntent)
                }
            }

            HomeBody.NoNotes -> {
                EmptyColumn(
                    icon = ConnectIcons.Folder,
                    title = R.string.notes_home_none_title,
                    desc = R.string.notes_home_none_desc,
                    modifier = column,
                ) {
                    Box(Modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
                        SecondaryButton(text = stringResource(UiR.string.ui_action_refresh), onClick = {
                            onIntent(NotesIntent.Refresh)
                        })
                    }
                    Spacer(Modifier.height(ENTRY_ROW_GAP))
                    HomeEntryRow(entries = entries, onIntent = onIntent)
                }
            }

            HomeBody.Notes -> {
                Unit
            }
        }
    }
}

/**
 * 첫 실행 빈 상태 — 타일 → 제목 → 설명 → 주 `새 노트` · 보조 `가져오기` → 한 줄 + 조용한 `저장소 연결`(LN-1 · 디자인 3.1).
 * 높이 Compact(가로 폰)는 타일을 빼고 두 버튼과 연결 줄을 각각 한 줄로 둔다.
 */
@Composable
private fun FirstRunColumn(
    entries: ImmutableList<HomeEntry>,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val short = MyNoteTheme.adaptive.heightClass == MyNoteHeightClass.Compact
    val buttons = entries.filter { it.weight != EntryWeight.Quiet }
    val quiet = entries.filter { it.weight == EntryWeight.Quiet }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (!short) {
            EmptyTile(MyNoteIcons.NewNote)
            Spacer(Modifier.height(18.dp))
        }
        KeepWordsText(
            text = stringResource(R.string.notes_home_first_title),
            style = MyNoteTheme.textStyles.emptyStateTitle(),
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        EmptyDesc(R.string.notes_home_first_desc, maxWidth = if (short) SHORT_DESC_WIDTH else DESC_WIDTH)
        Spacer(Modifier.height(if (short) 16.dp else 20.dp))
        if (short) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                buttons.forEach { EntryButton(it, onIntent, width = SHORT_BUTTON_WIDTH) }
            }
        } else {
            // 버튼마다 터치 48 상자가 위아래 4 씩 더하므로 보이는 간격은 8 이다
            Column {
                buttons.forEach { EntryButton(it, onIntent, width = BUTTON_WIDTH) }
            }
        }
        if (quiet.isNotEmpty()) {
            Spacer(Modifier.height(if (short) 12.dp else 24.dp))
            val hint =
                @Composable {
                    Text(
                        text = stringResource(R.string.notes_home_first_connect_hint),
                        style = MyNoteTheme.textStyles.caption,
                        color = colors.text3,
                        textAlign = TextAlign.Center,
                    )
                }
            if (short) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    hint()
                    quiet.forEach { EntryButton(it, onIntent) }
                }
            } else {
                hint()
                Spacer(Modifier.height(4.dp))
                quiet.forEach { EntryButton(it, onIntent) }
            }
        }
    }
}

/** 노트를 만드는 입구 두 버튼 한 줄(디자인 21A) — 내용 폭 · 간격 8. 노트가 있는 홈 · 연결 있는 빈 홈이 쓴다(LN-2). */
@Composable
internal fun HomeEntryRow(
    entries: ImmutableList<HomeEntry>,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { EntryButton(it, onIntent) }
    }
}

/** 입구 버튼 하나 — 보이는 높이는 버튼 그대로 두고 터치 영역을 48 로 둔다. [width] 가 없으면 내용 폭이다. */
@Composable
private fun EntryButton(
    entry: HomeEntry,
    onIntent: (NotesIntent) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = null,
) {
    val text = stringResource(entry.action.label)
    val icon = entry.action.icon
    val onClick = { onIntent(entry.action.intent) }
    val sized = if (width != null) Modifier.width(width) else Modifier
    Box(modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        when (entry.weight) {
            EntryWeight.Primary -> PrimaryButton(text, onClick, sized, leadingIcon = icon)
            EntryWeight.Secondary -> SecondaryButton(text, onClick, sized, leadingIcon = icon)
            EntryWeight.Quiet -> QuietButton(text, onClick, sized)
        }
    }
}

private val HomeAction.label: Int
    @StringRes get() =
        when (this) {
            HomeAction.NewNote -> R.string.notes_home_new_note
            HomeAction.Import -> R.string.notes_home_import
            HomeAction.Connect -> R.string.notes_home_first_connect
        }

private val HomeAction.icon: ImageVector?
    get() =
        when (this) {
            HomeAction.NewNote -> MyNoteIcons.NewNote
            HomeAction.Import -> MyNoteIcons.Import
            HomeAction.Connect -> null
        }

private val HomeAction.intent: NotesIntent
    get() =
        when (this) {
            HomeAction.NewNote -> NotesIntent.NewNote
            HomeAction.Import -> NotesIntent.Import
            HomeAction.Connect -> NotesIntent.OpenConnect
        }

@Composable
private fun EmptyColumn(
    icon: ImageVector,
    @StringRes title: Int,
    @StringRes desc: Int,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        EmptyTile(icon)
        Spacer(Modifier.height(18.dp))
        KeepWordsText(
            text = stringResource(title),
            style = MyNoteTheme.textStyles.emptyStateTitle(),
            color = MyNoteTheme.colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        EmptyDesc(desc, maxWidth = DESC_WIDTH)
        Spacer(Modifier.height(20.dp))
        action()
    }
}

@Composable
private fun EmptyTile(icon: ImageVector) {
    val colors = MyNoteTheme.colors
    val tileShape = RoundedCornerShape(15.dp)
    Box(
        modifier =
            Modifier
                .size(52.dp)
                .clip(tileShape)
                .background(colors.surface2)
                .border(1.dp, colors.border, tileShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.text3,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun EmptyDesc(
    @StringRes desc: Int,
    maxWidth: Dp,
) {
    KeepWordsText(
        text = stringResource(desc),
        style =
            MyNoteTheme.textStyles.body
                .copy(fontSize = 14.sp, lineHeight = 23.sp),
        color = MyNoteTheme.colors.text2,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = maxWidth),
    )
}

private val DESC_WIDTH = 280.dp
private val SHORT_DESC_WIDTH = 480.dp
private val BUTTON_WIDTH = 240.dp
private val SHORT_BUTTON_WIDTH = 180.dp
private val ENTRY_ROW_GAP = 16.dp
