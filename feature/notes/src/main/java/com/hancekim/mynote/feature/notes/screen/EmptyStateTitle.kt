package com.hancekim.mynote.feature.notes.screen

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designfoundation.token.MyNoteTextStyles

/** 빈 상태 제목 — `title` 을 작게 쓰고 줄 높이도 그 크기에 맞춘다. */
internal fun MyNoteTextStyles.emptyStateTitle(): TextStyle =
    title.copy(fontSize = EMPTY_TITLE_SIZE.sp, lineHeight = (EMPTY_TITLE_SIZE * EMPTY_TITLE_LINE).sp)

private const val EMPTY_TITLE_SIZE = 17f
private const val EMPTY_TITLE_LINE = 1.3f
