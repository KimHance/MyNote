package com.hancekim.mynote.feature.notes.screen.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.component.MyNoteIcons
import com.hancekim.mynote.core.designsystem.component.PressIndication
import com.hancekim.mynote.core.designsystem.component.throttledProcess
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.notes.R

@Composable
internal fun ConnectBand(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val indication = remember(colors, shape) { PressIndication(shape, pressColor = colors.accentSoftBorder) }
    val open = throttledProcess(action = onClick)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(shape)
                .background(colors.accentSoft)
                .border(1.dp, colors.accentSoftBorder, shape)
                .clickable(interactionSource = null, indication = indication, role = Role.Button, onClick = open)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.notes_home_connect_title),
                style = MyNoteTheme.textStyles.bodySmall.copy(fontWeight = FontWeight.W700),
                color = colors.text,
            )
            Text(
                text = stringResource(R.string.notes_home_connect_desc),
                style = MyNoteTheme.textStyles.caption,
                color = colors.text2,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.notes_home_connect_action),
                style = MyNoteTheme.textStyles.button.copy(fontWeight = FontWeight.W700),
                color = colors.accent,
                maxLines = 1,
                softWrap = false,
            )
            Icon(
                imageVector = MyNoteIcons.ChevronRight,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Preview(name = "연결 안내 띠", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun ConnectBandPreview() {
    MyNoteTheme {
        Column(Modifier.padding(16.dp)) { ConnectBand(onClick = {}) }
    }
}

@Preview(name = "연결 안내 띠 · ×1.3", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, fontScale = 1.3f)
@Composable
private fun ConnectBandLargePreview() {
    MyNoteTheme {
        Column(Modifier.padding(16.dp)) { ConnectBand(onClick = {}) }
    }
}
