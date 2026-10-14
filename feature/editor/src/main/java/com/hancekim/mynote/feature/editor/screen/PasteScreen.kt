package com.hancekim.mynote.feature.editor.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.editor.EditorIntent
import com.hancekim.mynote.feature.editor.PasteBlock
import com.hancekim.mynote.feature.editor.R
import com.hancekim.mynote.core.designsystem.R as DsR

/**
 * 붙여넣기 목적지 화면(LN-6 (c)). 칸 안에서 세로로 스크롤한다(여러 줄 칸의 안쪽 스크롤). 자기 상단 바(뒤로 · `붙여넣기` · 주 `다음`) 아래 라벨 `가져올 글` + 큰 칸(mono) + 이유 줄.
 * 앱은 클립보드를 읽지 않는다 — 붙여넣기 버튼 · 클립보드 미리보기가 없고 사용자가 칸을 길게 눌러 붙인다(D11).
 * `다음` 이면 미리 채운 편집기가 이 자리를 갈아 끼운다. 뒤로는 셸 뒤로가기와 같다.
 */
@Composable
internal fun PasteScreen(
    text: String,
    block: PasteBlock?,
    onIntent: (EditorIntent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyNoteTheme.colors
    val adaptive = MyNoteTheme.adaptive
    val reading = adaptive.readingMaxWidth
    val fieldFocus = remember { FocusRequester() }
    // 큰 칸에 포커스 · 키보드 열림(디자인 조작 표) — 화면을 다시 만들어도 한 번만
    var focusedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!focusedOnce) {
            focusedOnce = true
            fieldFocus.requestFocus()
        }
    }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.bg)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        EditorTopBar(
            title = stringResource(R.string.editor_import_paste),
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            navigationLabel = stringResource(DsR.string.ds_action_back),
            onNavigation = onClose,
            actionLabel = stringResource(R.string.editor_paste_next),
            actionEnabled = block == null,
            onAction = { onIntent(EditorIntent.PasteNext) },
        )
        Column(
            modifier =
                (if (reading != null) Modifier.widthIn(max = reading) else Modifier)
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = adaptive.page, end = adaptive.page, top = 12.dp, bottom = BOTTOM_SPACE),
        ) {
            Text(
                text = stringResource(R.string.editor_paste_label),
                style = MyNoteTheme.textStyles.caption,
                color = colors.text3,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            val shape = RoundedCornerShape(FIELD_RADIUS)
            PlainField(
                value = text,
                hint = stringResource(R.string.editor_paste_field),
                style = MyNoteTheme.textStyles.mono,
                color = colors.bodyText,
                singleLine = false,
                onChange = { onIntent(EditorIntent.PasteChanged(it)) },
                modifier =
                    Modifier
                        .weight(1f)
                        .background(colors.surface2, shape)
                        .border(1.dp, colors.border, shape)
                        .padding(FIELD_PADDING)
                        .focusRequester(fieldFocus),
            )
            block?.let {
                ReasonLine(
                    text =
                        when (it) {
                            PasteBlock.Empty -> stringResource(R.string.editor_paste_reason_empty)
                            PasteBlock.TooLarge -> stringResource(R.string.editor_import_rejected_too_large)
                        },
                    warn = it == PasteBlock.TooLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private val FIELD_RADIUS: Dp = 12.dp
private val FIELD_PADDING: Dp = 14.dp

// 아래 40 = 제스처 바 24 + 16(디자인 README-2 §3.2) — 제스처 바는 안전 영역이 이미 빼므로 16 만 둔다
private val BOTTOM_SPACE: Dp = 16.dp

@Preview(name = "붙여넣기 · 빈", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun PasteEmptyPreview() {
    MyNoteTheme { PasteScreen(text = "", block = PasteBlock.Empty, onIntent = {}, onClose = {}) }
}

@Preview(name = "붙여넣기 · 채움", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun PasteFilledPreview() {
    MyNoteTheme {
        PasteScreen(
            text = "---\ntitle: Flow와 채널\ndate: 2026.10.14\n---\n\n## 콜드 스트림\n\nFlow 는 수집할 때 시작한다.",
            block = null,
            onIntent = {},
            onClose = {},
        )
    }
}

@Preview(name = "붙여넣기 · 크기 넘음", showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360, heightDp = 640)
@Composable
private fun PasteTooLargePreview() {
    MyNoteTheme {
        PasteScreen(
            text = "# 아주 긴 글\n".repeat(40),
            block = PasteBlock.TooLarge,
            onIntent = {},
            onClose = {},
        )
    }
}
