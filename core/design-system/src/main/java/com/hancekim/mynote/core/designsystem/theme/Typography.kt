package com.hancekim.mynote.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** 본문 폰트 스케일 토글(본문 계열 size 만 영향). */
enum class MyNoteFontScale(
    val bodySizeSp: Float,
) {
    Normal(16f),
    Large(18f),
}

/**
 * 핸드오프 타이포 토큰. 폰트는 시스템 기본(Roboto + 한글) / 코드는 Monospace 로 매핑.
 *
 * `mdHeading2`(본문 `##` = body+5), `mdHeading3`(body+1), `body` 는 [MyNoteFontScale] 에 따라 스케일된다.
 */
@Immutable
data class MyNoteTextStyles(
    /** H1 — 노트 제목 */
    val noteTitle: TextStyle,
    /** H2 — UI 섹션 제목 */
    val sectionTitle: TextStyle,
    /** "최근 노트" 같은 대문자 섹션 라벨 */
    val overline: TextStyle,
    /** 카드 제목 16/500 */
    val cardTitle: TextStyle,
    /** 트리/칩 라벨 14/500 */
    val treeLabel: TextStyle,
    /** 메타·캡션·날짜 13/400 */
    val caption: TextStyle,
    /** 마크다운 본문 ## */
    val mdHeading2: TextStyle,
    /** 마크다운 본문 ### */
    val mdHeading3: TextStyle,
    /** 마크다운 본문 단락 */
    val body: TextStyle,
    /** 코드(블록·인라인) */
    val mono: TextStyle,
)

/** [MyNoteFontScale] 의 본문 크기를 기준으로 타이포 세트를 생성. */
fun myNoteTextStyles(bodySizeSp: Float): MyNoteTextStyles =
    MyNoteTextStyles(
        noteTitle =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 28.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.015).em,
                lineHeight = (28 * 1.25f).sp,
            ),
        sectionTitle =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 21.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.01).em,
            ),
        overline =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 12.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = 0.08.em,
            ),
        cardTitle =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                lineHeight = 22.sp,
            ),
        treeLabel =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
            ),
        caption =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 13.sp,
                fontWeight = FontWeight.W400,
            ),
        mdHeading2 =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = (bodySizeSp + 5).sp,
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.01).em,
            ),
        mdHeading3 =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = (bodySizeSp + 1).sp,
                fontWeight = FontWeight.W600,
            ),
        body =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = bodySizeSp.sp,
                fontWeight = FontWeight.W400,
                lineHeight = (bodySizeSp * 1.78f).sp,
            ),
        mono =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.W400,
                lineHeight = (13 * 1.65f).sp,
            ),
    )
