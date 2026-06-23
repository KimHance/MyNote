package com.hancekim.mynote.core.designfoundation.token

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 핸드오프 디자인 토큰의 색상 팔레트(다크 단일 테마).
 *
 * Material `ColorScheme` 만으로는 표현이 부족한 의미론적 토큰(surface-2, body-text, text-3 등)을
 * 직접 들고 있는다. [com.hancekim.mynote.core.designsystem] 의 `MyNoteTheme` 가 CompositionLocal 로 노출한다.
 */
@Immutable
data class MyNoteColors(
    /** 앱 배경 / 메인 콘텐츠 배경 */
    val bg: Color,
    /** 카드 · 패널 배경 */
    val surface: Color,
    /** 코드블록 · 검색창 · 인라인 코드 배경 */
    val surface2: Color,
    /** 구분선 · 카드 테두리 */
    val border: Color,
    /** 링크 · 활성 노트 · 카테고리 강조 · 로고 */
    val accent: Color,
    /** 칩/활성 배경 (accent 14%) */
    val accentSoft: Color,
    /** 칩/활성 테두리 (accent 26%) */
    val accentSoftBorder: Color,
    /** 본문 강조 · 제목 */
    val text: Color,
    /** 본문 보조 */
    val text2: Color,
    /** 메타 · 캡션 · 비활성 */
    val text3: Color,
    /** 노트 본문 단락 */
    val bodyText: Color,
)

/** 핸드오프에 정의된 유일한(다크) 팔레트. */
val StudyNotesDarkColors: MyNoteColors = MyNoteColors(
    bg = Color(0xFF0E1217),
    surface = Color(0xFF161C24),
    surface2 = Color(0xFF1B232D),
    border = Color(0xFF283039),
    accent = Color(0xFF6EA8FE),
    // rgba(110,168,254,0.14) → alpha ≈ 0x24
    accentSoft = Color(0x246EA8FE),
    // rgba(110,168,254,0.26) → alpha ≈ 0x42
    accentSoftBorder = Color(0x426EA8FE),
    text = Color(0xFFE6EDF3),
    text2 = Color(0xFF9AA7B4),
    text3 = Color(0xFF687585),
    bodyText = Color(0xFFC8D2DC),
)
