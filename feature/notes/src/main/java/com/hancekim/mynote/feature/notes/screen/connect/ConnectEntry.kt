package com.hancekim.mynote.feature.notes.screen.connect

import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.screen.QuietButton
import com.hancekim.mynote.feature.notes.screen.SecondaryButton

private val ENTRY_STEPS =
    intArrayOf(R.string.notes_entry_step_1, R.string.notes_entry_step_2, R.string.notes_entry_step_3)

@Composable
internal fun EntrySection(
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        KeepWordsText(
            text = stringResource(R.string.notes_entry_title),
            style =
                MyNoteTheme.textStyles.screenTitle.copy(
                    fontSize = ENTRY_TITLE_SIZE.sp,
                    lineHeight = (ENTRY_TITLE_SIZE * ENTRY_TITLE_LINE).sp,
                ),
            color = colors.text,
            modifier = Modifier.semantics { heading() },
        )
        KeepWordsText(
            text = stringResource(R.string.notes_entry_desc),
            style = MyNoteTheme.textStyles.bodySmall,
            color = colors.text2,
            modifier = Modifier.padding(top = 6.dp),
        )
        Column(
            modifier =
                Modifier
                    .padding(top = 16.dp)
                    .semantics { collectionInfo = CollectionInfo(rowCount = ENTRY_STEPS.size, columnCount = 1) },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ENTRY_STEPS.forEachIndexed { index, text ->
                EntryStep(number = index + 1, text = text)
            }
        }
        KeepWordsText(
            text = stringResource(R.string.notes_entry_private),
            style = MyNoteTheme.textStyles.caption,
            color = colors.text3,
            modifier = Modifier.padding(top = 12.dp),
        )
        EntryCreateButton(
            onClick = onCreate,
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
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
    val label = stringResource(R.string.notes_entry_create_a11y)
    SecondaryButton(
        text = stringResource(R.string.notes_entry_create),
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
        text = stringResource(R.string.notes_entry_guide),
        onClick = onClick,
        contentDescription = stringResource(R.string.notes_entry_guide_a11y),
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
            text = stringResource(R.string.notes_entry_or),
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
private val BADGE_SIZE: Dp = 24.dp
private const val ENTRY_TITLE_SIZE = 19f
private const val ENTRY_TITLE_LINE = 1.35f
