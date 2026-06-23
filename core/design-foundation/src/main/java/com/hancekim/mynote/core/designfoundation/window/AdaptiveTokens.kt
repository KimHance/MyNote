package com.hancekim.mynote.core.designfoundation.window

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 안드로이드 공식 Window size class 너비(dp) 기준 5단계.
 *
 * Material3 의 기본 WindowSizeClass 는 Compact/Medium/Expanded 3단계뿐이라,
 * 핸드오프의 5단계(Large·ExtraLarge 포함) 반응형 토큰을 정확히 재현하기 위해 자체 enum 으로 둔다.
 */
enum class MyNoteWindowClass {
    Compact, // < 600
    Medium, // 600–839
    Expanded, // 840–1199
    Large, // 1200–1599
    ExtraLarge; // ≥ 1600

    companion object {
        /** 가용 너비(dp)로부터 단계 판정. */
        fun fromWidth(widthDp: Dp): MyNoteWindowClass = when {
            widthDp < 600.dp -> Compact
            widthDp < 840.dp -> Medium
            widthDp < 1200.dp -> Expanded
            widthDp < 1600.dp -> Large
            else -> ExtraLarge
        }
    }
}

/**
 * 폼팩터 단계별 레이아웃 토큰. 화면은 간격/폭/열수를 항상 이 토큰을 통해 가져온다(하드코딩 금지).
 *
 * @param page 콘텐츠 좌우 여백
 * @param cardGap 홈 카드 그리드 간격
 * @param paneWidth 좌측 고정 목록 패널 너비(Compact 는 모달 드로어 폭)
 * @param readingMaxWidth 본문/홈 콘텐츠 최대 폭. null 이면 full width
 * @param homeGridColumns "최근 노트" 카드 그리드 열 수
 * @param useModalDrawer Compact 전용 모달 드로어 사용 여부
 */
@Immutable
data class AdaptiveTokens(
    val windowClass: MyNoteWindowClass,
    val page: Dp,
    val cardGap: Dp,
    val paneWidth: Dp,
    val readingMaxWidth: Dp?,
    val homeGridColumns: Int,
    val useModalDrawer: Boolean,
)

/** [MyNoteWindowClass] → [AdaptiveTokens] 매핑(핸드오프 반응형 표 그대로). */
fun adaptiveTokensFor(windowClass: MyNoteWindowClass): AdaptiveTokens = when (windowClass) {
    MyNoteWindowClass.Compact -> AdaptiveTokens(
        windowClass = windowClass,
        page = 16.dp,
        cardGap = 10.dp,
        paneWidth = 300.dp, // 모달 드로어 폭
        readingMaxWidth = null,
        homeGridColumns = 1,
        useModalDrawer = true,
    )

    MyNoteWindowClass.Medium -> AdaptiveTokens(
        windowClass = windowClass,
        page = 24.dp,
        cardGap = 12.dp,
        paneWidth = 268.dp,
        readingMaxWidth = null,
        homeGridColumns = 2,
        useModalDrawer = false,
    )

    MyNoteWindowClass.Expanded -> AdaptiveTokens(
        windowClass = windowClass,
        page = 32.dp,
        cardGap = 14.dp,
        paneWidth = 300.dp,
        readingMaxWidth = 720.dp,
        homeGridColumns = 2,
        useModalDrawer = false,
    )

    MyNoteWindowClass.Large -> AdaptiveTokens(
        windowClass = windowClass,
        page = 40.dp,
        cardGap = 16.dp,
        paneWidth = 320.dp,
        readingMaxWidth = 740.dp,
        homeGridColumns = 3,
        useModalDrawer = false,
    )

    MyNoteWindowClass.ExtraLarge -> AdaptiveTokens(
        windowClass = windowClass,
        page = 56.dp,
        cardGap = 18.dp,
        paneWidth = 340.dp,
        readingMaxWidth = 780.dp,
        homeGridColumns = 3,
        useModalDrawer = false,
    )
}
