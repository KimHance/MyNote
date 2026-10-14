package com.hancekim.mynote.feature.editor.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import com.hancekim.mynote.feature.editor.EditorIntent
import com.hancekim.mynote.feature.editor.R

// 골격 — 붙여넣기 칸 모양 · 이유 줄은 가져오기 화면 단위가 채운다. 클립보드를 읽지 않는다(D11)

/** 붙여넣기 목적지 화면(LN-6 (c)). `다음` 이면 미리 채운 편집기가 이 자리를 갈아 끼운다. 닫기는 셸 뒤로가기와 같다. */
@Composable
internal fun PasteScreen(
    text: String,
    onIntent: (EditorIntent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MyNoteTheme.colors.bg)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        EditorTopBar(
            title = stringResource(R.string.editor_import_paste),
            onClose = onClose,
            actionLabel = stringResource(R.string.editor_paste_next),
            actionEnabled = text.isNotBlank(),
            onAction = { onIntent(EditorIntent.PasteNext) },
        )
        PlainField(
            value = text,
            hint = stringResource(R.string.editor_paste_field),
            style = MyNoteTheme.textStyles.mono,
            singleLine = false,
            onChange = { onIntent(EditorIntent.PasteChanged(it)) },
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        )
    }
}
