package com.hancekim.mynote.feature.notes.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.R
import com.hancekim.mynote.feature.notes.element.HomeBody
import com.hancekim.mynote.feature.notes.screen.connect.ConnectIcons
import com.hancekim.mynote.feature.notes.screen.connect.KeepWordsText
import com.hancekim.mynote.feature.notes.screen.connect.ProgressBar

@Composable
internal fun HomeEmpty(
    body: HomeBody,
    onRefresh: () -> Unit,
    endPadding: Dp,
    topPadding: Dp,
    modifier: Modifier = Modifier,
) {
    when (body) {
        HomeBody.Receiving -> {
            EmptyColumn(
                icon = ConnectIcons.Download,
                title = R.string.notes_home_receiving_title,
                desc = R.string.notes_home_receiving_desc,
                endPadding = endPadding,
                topPadding = topPadding,
                modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                ProgressBar(Modifier.size(width = 120.dp, height = 4.dp))
            }
        }

        HomeBody.NoNotes -> {
            EmptyColumn(
                icon = ConnectIcons.Folder,
                title = R.string.notes_home_none_title,
                desc = R.string.notes_home_none_desc,
                endPadding = endPadding,
                topPadding = topPadding,
                modifier = modifier,
            ) {
                Box(Modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
                    SecondaryButton(text = stringResource(R.string.notes_action_refresh), onClick = onRefresh)
                }
            }
        }

        HomeBody.Notes -> {
            Unit
        }
    }
}

@Composable
private fun EmptyColumn(
    icon: ImageVector,
    @StringRes title: Int,
    @StringRes desc: Int,
    endPadding: Dp,
    topPadding: Dp,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit,
) {
    val colors = MyNoteTheme.colors
    val tileShape = RoundedCornerShape(15.dp)
    Column(
        modifier = modifier.padding(start = 8.dp, end = 8.dp + endPadding, top = topPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
        Spacer(Modifier.height(18.dp))
        KeepWordsText(
            text = stringResource(title),
            style = MyNoteTheme.textStyles.emptyStateTitle(),
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        KeepWordsText(
            text = stringResource(desc),
            style =
                MyNoteTheme.textStyles.body
                    .copy(fontSize = 14.sp, lineHeight = 23.sp),
            color = colors.text2,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp),
        )
        Spacer(Modifier.height(20.dp))
        action()
    }
}
