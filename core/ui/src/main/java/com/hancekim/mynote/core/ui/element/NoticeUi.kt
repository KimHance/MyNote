package com.hancekim.mynote.core.ui.element

import androidx.compose.runtime.Stable

/** 스낵바 알림. 되돌리기는 알림을 낸 기능의 인텐트로 돌아간다. */
@Stable
data class NoticeUi<out E>(
    val message: String,
    val undo: E?,
    val id: Int,
)
