package com.hancekim.mynote.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designsystem.R
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

/**
 * 기기 노트의 출처 — 글리프 + `이 기기`(카드 메타 줄 · 리더 머리 · 편집기 미리보기 머리). 색 없이 모양과 낱말로 읽힌다(local-notes 디자인 C3).
 * 저장소 노트에는 그리지 않는다(local-notes 결정 8). 글리프는 장식이고 낱말이 이름이다.
 */
@Composable
fun DeviceOriginLabel(modifier: Modifier = Modifier) {
    val colors = MyNoteTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = MyNoteIcons.Device,
            contentDescription = null,
            tint = colors.text3,
            modifier = Modifier.size(DeviceOriginDefaults.GlyphSize),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.ds_origin_device),
            style = MyNoteTheme.textStyles.caption,
            color = colors.text3,
            maxLines = 1,
        )
    }
}

/** 글자 없는 자리(목록 행)의 출처 글리프. 행 이름 뒤에 `이 기기` 가 읽힌다. */
@Composable
fun DeviceGlyph(
    modifier: Modifier = Modifier,
    size: Dp = DeviceOriginDefaults.GlyphSize,
) {
    Icon(
        imageVector = MyNoteIcons.Device,
        contentDescription = stringResource(R.string.ds_origin_device),
        tint = MyNoteTheme.colors.text3,
        modifier = modifier.size(size),
    )
}

object DeviceOriginDefaults {
    val GlyphSize: Dp = 12.dp
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1217)
@Composable
private fun DeviceOriginPreview() {
    MyNoteTheme {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryChip("Kotlin")
                Spacer(Modifier.width(10.dp))
                Text("2026.10.14", style = MyNoteTheme.textStyles.caption, color = MyNoteTheme.colors.text3)
                Spacer(Modifier.width(8.dp))
                DeviceOriginLabel()
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Flow와 채널", style = MyNoteTheme.textStyles.treeLabel, color = MyNoteTheme.colors.text2)
                DeviceGlyph()
                BookmarkGlyph()
            }
        }
    }
}
