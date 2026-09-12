package com.hancekim.mynote.core.designfoundation.window

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MyNoteWindowClass {
    Compact, // < 600
    Medium, // 600–839
    Expanded, // 840–1199
    Large, // 1200–1599
    ExtraLarge, // ≥ 1600
    ;

    companion object {
        fun fromWidth(widthDp: Dp): MyNoteWindowClass =
            when {
                widthDp < 600.dp -> Compact
                widthDp < 840.dp -> Medium
                widthDp < 1200.dp -> Expanded
                widthDp < 1600.dp -> Large
                else -> ExtraLarge
            }
    }
}

enum class MyNoteHeightClass {
    Compact, // < 480
    Medium, // 480–899
    Expanded, // ≥ 900
    ;

    companion object {
        fun fromHeight(heightDp: Dp): MyNoteHeightClass =
            when {
                heightDp < 480.dp -> Compact
                heightDp < 900.dp -> Medium
                else -> Expanded
            }
    }
}

@Immutable
data class AdaptiveTokens(
    val windowClass: MyNoteWindowClass,
    val heightClass: MyNoteHeightClass,
    val page: Dp,
    val cardGap: Dp,
    val paneWidth: Dp,
    val readingMaxWidth: Dp?,
    val cardMinWidth: Dp,
    val useModalDrawer: Boolean,
    val headerHeight: Dp,
    val padY: Dp,
    val fadeHeight: Dp,
    val heroLogoSize: Dp?,
    val heroTitleSize: TextUnit,
) {
    val noteTitleSize: TextUnit get() = (heroTitleSize.value + 4).sp

    val useFloatingRail: Boolean
        get() =
            windowClass == MyNoteWindowClass.Expanded ||
                windowClass == MyNoteWindowClass.Large ||
                windowClass == MyNoteWindowClass.ExtraLarge ||
                heightClass == MyNoteHeightClass.Compact

    val railRightMargin: Dp get() = maxOf(14.dp, page - 12.dp)

    val barBottomMargin: Dp get() = padY + 6.dp

    val panelHeaderPadTop: Dp get() = maxOf(10.dp, padY - 4.dp)
}

private data class WidthTokens(
    val page: Dp,
    val cardGap: Dp,
    val paneWidth: Dp,
    val readingMaxWidth: Dp?,
    val cardMinWidth: Dp,
    val useModalDrawer: Boolean,
)

private data class HeightTokens(
    val headerHeight: Dp,
    val padY: Dp,
    val fadeHeight: Dp,
    val heroLogoSize: Dp?,
    val heroTitleSize: TextUnit,
)

private fun widthTokensFor(windowClass: MyNoteWindowClass): WidthTokens =
    when (windowClass) {
        MyNoteWindowClass.Compact -> {
            WidthTokens(16.dp, 10.dp, 300.dp, null, 240.dp, useModalDrawer = true)
        }

        MyNoteWindowClass.Medium -> {
            WidthTokens(24.dp, 12.dp, 268.dp, null, 200.dp, useModalDrawer = false)
        }

        MyNoteWindowClass.Expanded -> {
            WidthTokens(32.dp, 14.dp, 300.dp, 720.dp, 220.dp, useModalDrawer = false)
        }

        MyNoteWindowClass.Large -> {
            WidthTokens(40.dp, 16.dp, 320.dp, 760.dp, 230.dp, useModalDrawer = false)
        }

        MyNoteWindowClass.ExtraLarge -> {
            WidthTokens(56.dp, 18.dp, 340.dp, 820.dp, 240.dp, useModalDrawer = false)
        }
    }

private fun heightTokensFor(heightClass: MyNoteHeightClass): HeightTokens =
    when (heightClass) {
        MyNoteHeightClass.Compact -> {
            HeightTokens(44.dp, 10.dp, 16.dp, null, 20.sp)
        }

        MyNoteHeightClass.Medium -> {
            HeightTokens(56.dp, 20.dp, 24.dp, 46.dp, 24.sp)
        }

        MyNoteHeightClass.Expanded -> {
            HeightTokens(60.dp, 28.dp, 30.dp, 54.dp, 28.sp)
        }
    }

fun adaptiveTokensFor(
    windowClass: MyNoteWindowClass,
    heightClass: MyNoteHeightClass = MyNoteHeightClass.Medium,
): AdaptiveTokens {
    val width = widthTokensFor(windowClass)
    val height = heightTokensFor(heightClass)
    return AdaptiveTokens(
        windowClass = windowClass,
        heightClass = heightClass,
        page = width.page,
        cardGap = width.cardGap,
        paneWidth = width.paneWidth,
        readingMaxWidth = width.readingMaxWidth,
        cardMinWidth = width.cardMinWidth,
        useModalDrawer = width.useModalDrawer,
        headerHeight = height.headerHeight,
        padY = height.padY,
        fadeHeight = height.fadeHeight,
        heroLogoSize = height.heroLogoSize,
        heroTitleSize = height.heroTitleSize,
    )
}
