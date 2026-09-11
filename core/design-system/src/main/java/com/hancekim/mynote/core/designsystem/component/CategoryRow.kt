package com.hancekim.mynote.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

/**
 * 목록 패널의 카테고리 헤더 행. caret(▸) 은 펼침 시 90° 회전한다.
 *
 * @param count 카테고리에 속한 노트 개수
 * @param expanded 펼침 상태
 */
@Composable
fun CategoryRow(
    name: String,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val caretRotation by animateFloatAsState(if (expanded) 90f else 0f, label = "caret")
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick, role = Role.Button)
                .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "▸",
            style = MyNoteTheme.textStyles.treeLabel,
            color = colors.text3,
            // deferred read: 회전값은 graphicsLayer 람다(draw phase)에서 읽어 caret 애니메이션의
            // 매 프레임 recomposition 을 피한다. Modifier.rotate(value) 는 composition 에서 읽혀 금지.
            modifier = Modifier.graphicsLayer { rotationZ = caretRotation },
        )
        Text(
            text = name,
            style = MyNoteTheme.textStyles.treeLabel,
            color = colors.text,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
        )
        Text(
            text = count.toString(),
            style = MyNoteTheme.textStyles.mono,
            color = colors.text3,
        )
    }
}

private data class CategoryRowPreviewState(
    val expanded: Boolean,
)

private class CategoryRowPreviewProvider : PreviewParameterProvider<CategoryRowPreviewState> {
    override val values =
        sequenceOf(
            CategoryRowPreviewState(expanded = false),
            CategoryRowPreviewState(expanded = true),
        )
}

@Preview(showBackground = true, backgroundColor = 0xFF161C24, widthDp = 280)
@Composable
private fun CategoryRowPreview(
    @PreviewParameter(CategoryRowPreviewProvider::class) state: CategoryRowPreviewState,
) {
    MyNoteTheme {
        CategoryRow(name = "Android", count = 4, expanded = state.expanded, onClick = {})
    }
}
