package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import java.util.Locale

@Composable
fun NumberedRow(
    number: Int,
    title: String,
    subtitle: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compactHeight: Boolean = false,
    trailing: @Composable () -> Unit = {},
) {
    val colors = MyNoteTheme.colors
    val styles = MyNoteTheme.textStyles
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick, role = Role.Button)
                    .semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
                    .padding(vertical = if (compactHeight) ROW_PAD_SHORT else ROW_PAD)
                    .heightIn(min = MIN_CONTENT),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
        ) {
            Text(
                text = NUMBER_FORMAT.format(Locale.ROOT, number),
                style = styles.monoSmall.copy(fontSize = 13.sp),
                color = colors.text3,
                modifier = Modifier.widthIn(min = NUMBER_MIN_WIDTH),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = styles.treeLabel.copy(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.W500),
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = styles.caption.copy(fontSize = 12.sp),
                    color = colors.text3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailing()
        }
        HorizontalDivider(color = colors.border, thickness = 1.dp)
    }
}

private const val NUMBER_FORMAT = "%02d"
private val ROW_PAD = 12.dp
private val ROW_PAD_SHORT = 6.dp
private val ROW_GAP = 12.dp
private val MIN_CONTENT = 44.dp
private val NUMBER_MIN_WIDTH = 24.dp

private class NumberedRowHeightProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)

    override fun getDisplayName(index: Int): String? = if (index == 0) "세로" else "높이 Compact"
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun NumberedRowPreview(
    @PreviewParameter(NumberedRowHeightProvider::class) compactHeight: Boolean,
) {
    MyNoteTheme {
        NumberedRow(
            number = 1,
            title = "해시 테이블 충돌 처리 — 아주 길어서 한 줄에서 말줄임되는 제목",
            subtitle = "datastructure · 40%에서 멈춤 · 어제",
            contentDescription = "",
            onClick = {},
            compactHeight = compactHeight,
            modifier = Modifier.padding(horizontal = 16.dp),
            trailing = {
                Box(
                    Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .padding(0.dp),
                )
            },
        )
    }
}
