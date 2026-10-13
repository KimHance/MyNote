package com.hancekim.mynote.feature.connect.screen

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.core.ui.screen.QuietButton
import com.hancekim.mynote.core.ui.screen.SecondaryButton
import com.hancekim.mynote.core.ui.screen.connect.ConnectIcons
import com.hancekim.mynote.core.ui.screen.connect.KeepWordsText
import com.hancekim.mynote.feature.connect.R
import kotlinx.coroutines.delay

private val ENTRY_STEPS =
    intArrayOf(R.string.connect_entry_step_1, R.string.connect_entry_step_2, R.string.connect_entry_step_3)

/**
 * 저장소가 없는 사람에게 템플릿으로 만드는 길을 안내한다. [collapsible] 이면 `저장소 만들기` 단추가 안내 상자를
 * 펼치고 접는다 — 안내를 다 읽은 뒤 상자 끝의 단추로 템플릿 만들기 화면을 연다(사용자 결정). 두 칸 배치처럼 자리가
 * 넉넉하면 펼친 채로 단추 없이 보인다.
 */
@Composable
internal fun EntrySection(
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onToggle: () -> Unit = {},
    collapsible: Boolean = false,
) {
    val colors = MyNoteTheme.colors
    val panelInView = remember { BringIntoViewRequester() }
    // 펼친 상자가 화면 아래로 잘리지 않게 펼침이 끝난 뒤 상자 끝(템플릿 단추)까지 보이게 올린다
    LaunchedEffect(expanded, collapsible) {
        if (collapsible && expanded) {
            delay(GUIDE_MILLIS.toLong())
            panelInView.bringIntoView()
        }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        KeepWordsText(
            text = stringResource(R.string.connect_entry_title),
            style =
                MyNoteTheme.textStyles.screenTitle.copy(
                    fontSize = ENTRY_TITLE_SIZE.sp,
                    lineHeight = (ENTRY_TITLE_SIZE * ENTRY_TITLE_LINE).sp,
                ),
            color = colors.text,
            modifier = Modifier.semantics { heading() },
        )
        KeepWordsText(
            text = stringResource(R.string.connect_entry_desc),
            style = MyNoteTheme.textStyles.bodySmall,
            color = colors.text2,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (collapsible) {
            EntryToggleButton(
                expanded = expanded,
                onClick = onToggle,
                modifier = Modifier.padding(top = 14.dp).fillMaxWidth(),
            )
        }
        AnimatedVisibility(
            visible = expanded || !collapsible,
            enter = expandVertically(tween(GUIDE_MILLIS)) + fadeIn(tween(GUIDE_MILLIS)),
            exit = shrinkVertically(tween(GUIDE_MILLIS)) + fadeOut(tween(GUIDE_MILLIS)),
        ) {
            EntryGuidePanel(
                onCreate = onCreate,
                modifier = Modifier.padding(top = 12.dp).bringIntoViewRequester(panelInView),
            )
        }
    }
}

@Composable
private fun EntryToggleButton(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = stringResource(if (expanded) R.string.connect_entry_state_open else R.string.connect_entry_state_closed)
    SecondaryButton(
        text = stringResource(R.string.connect_entry_create),
        onClick = onClick,
        trailingIcon = if (expanded) ConnectIcons.ChevronUp else ConnectIcons.ChevronDown,
        modifier = modifier.minimumInteractiveComponentSize().semantics { stateDescription = state },
    )
}

@Composable
private fun EntryGuidePanel(
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(PANEL_RADIUS)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.border, shape)
                .padding(PANEL_PADDING),
    ) {
        Column(
            modifier =
                Modifier.semantics {
                    collectionInfo =
                        CollectionInfo(rowCount = ENTRY_STEPS.size, columnCount = 1)
                },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ENTRY_STEPS.forEachIndexed { index, text ->
                EntryStep(number = index + 1, text = text)
            }
        }
        KeepWordsText(
            text = stringResource(R.string.connect_entry_private),
            style = MyNoteTheme.textStyles.caption,
            color = colors.text3,
            modifier = Modifier.padding(top = 14.dp),
        )
        EntryOpenTemplateButton(
            onClick = onCreate,
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
        )
    }
}

@Composable
private fun EntryOpenTemplateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val label = stringResource(R.string.connect_entry_open_a11y)
    Row(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .height(OPEN_BUTTON_HEIGHT)
                .clip(shape)
                .background(colors.accentSoft)
                .border(1.dp, colors.accentSoftBorder, shape)
                .clickable(role = Role.Button, onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = label
                    role = Role.Button
                    onClick {
                        onClick()
                        true
                    }
                },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KeepWordsText(
            text = stringResource(R.string.connect_entry_open),
            style = MyNoteTheme.textStyles.body.copy(fontWeight = FontWeight.W600),
            color = colors.accent,
        )
        Icon(
            imageVector = ConnectIcons.External,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.padding(start = 8.dp).size(15.dp),
        )
    }
}

@Composable
private fun EntryStep(
    number: Int,
    @StringRes text: Int,
) {
    val colors = MyNoteTheme.colors
    Row(
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                collectionItemInfo =
                    CollectionItemInfo(rowIndex = number - 1, rowSpan = 1, columnIndex = 0, columnSpan = 1)
            },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(BADGE_SIZE)
                    .clip(CircleShape)
                    .background(colors.surface2)
                    .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            KeepWordsText(
                text = number.toString(),
                style = MyNoteTheme.textStyles.monoSmall.copy(fontWeight = FontWeight.W600),
                color = colors.text2,
                textAlign = TextAlign.Center,
            )
        }
        KeepWordsText(
            text = stringResource(text),
            style = MyNoteTheme.textStyles.bodySmall,
            color = colors.bodyText,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun EntryCreateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.connect_entry_create_a11y)
    SecondaryButton(
        text = stringResource(R.string.connect_entry_create),
        onClick = onClick,
        trailingIcon = ConnectIcons.External,
        modifier = modifier.minimumInteractiveComponentSize().semantics { contentDescription = label },
    )
}

@Composable
internal fun EntryGuideButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QuietButton(
        text = stringResource(R.string.connect_entry_guide),
        onClick = onClick,
        contentDescription = stringResource(R.string.connect_entry_guide_a11y),
        trailingIcon = ConnectIcons.External,
        modifier = modifier.minimumInteractiveComponentSize(),
    )
}

@Composable
internal fun FlowRowScope.NoticeWayButtons(
    onCreate: () -> Unit,
    onGuide: () -> Unit,
) {
    EntryCreateButton(onClick = onCreate)
    EntryGuideButton(onClick = onGuide)
}

@Composable
internal fun EntryDivider(modifier: Modifier = Modifier) {
    val colors = MyNoteTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(colors.border))
        KeepWordsText(
            text = stringResource(R.string.connect_entry_or),
            style = MyNoteTheme.textStyles.caption,
            color = colors.text3,
        )
        Box(Modifier.weight(1f).height(1.dp).background(colors.border))
    }
}

@Composable
internal fun EntryColumnDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.width(COLUMN_GAP).fillMaxHeight().clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(1.dp).fillMaxHeight().background(MyNoteTheme.colors.border))
    }
}

internal val COLUMN_GAP: Dp = 24.dp
private val PANEL_RADIUS: Dp = 16.dp
private val PANEL_PADDING: Dp = 16.dp
private val OPEN_BUTTON_HEIGHT: Dp = 48.dp
private const val GUIDE_MILLIS = 220
private val BADGE_SIZE: Dp = 24.dp
private const val ENTRY_TITLE_SIZE = 19f
private const val ENTRY_TITLE_LINE = 1.35f
