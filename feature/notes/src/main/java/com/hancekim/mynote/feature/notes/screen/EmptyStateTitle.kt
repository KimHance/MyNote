package com.hancekim.mynote.feature.notes.screen

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designfoundation.token.MyNoteTextStyles
import com.hancekim.mynote.core.designfoundation.token.resized

/** 빈 상태 제목 — `title` 을 작게 쓰고 줄 높이도 같은 배율로 맞춘다. */
internal fun MyNoteTextStyles.emptyStateTitle(): TextStyle = title.resized(EMPTY_TITLE_SIZE.sp)

private const val EMPTY_TITLE_SIZE = 17f
