package com.hancekim.mynote.core.designfoundation.token

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 핸드오프의 radius 토큰. (object 는 본래 stable 이라 @Immutable 불필요)
 *
 * 프레임(폼팩터) radius 는 데모용 디바이스 프레임에만 쓰이므로 여기엔 콘텐츠 토큰만 둔다.
 */
object MyNoteRadius {
    /** 카드 · 패널 */
    val card: Dp = 14.dp
    /** 리스트 · 코드블록 · 검색창 */
    val list: Dp = 10.dp
    /** 칩 · 트리 아이템 */
    val chip: Dp = 8.dp
    /** 인라인 코드 */
    val inlineCode: Dp = 5.dp
}

/** 핸드오프의 간격 스케일: 4 · 8 · 12 · 16 · 24 · 32. */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
}
