package com.hancekim.mynote.core.designfoundation.window

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

fun adaptiveTokensFor(windowClass: MyNoteWindowClass): AdaptiveTokens =
    when (windowClass) {
        MyNoteWindowClass.Compact -> {
            AdaptiveTokens(
                windowClass = windowClass,
                page = 16.dp,
                cardGap = 10.dp,
                paneWidth = 300.dp,
                readingMaxWidth = null,
                homeGridColumns = 1,
                useModalDrawer = true,
            )
        }

        MyNoteWindowClass.Medium -> {
            AdaptiveTokens(
                windowClass = windowClass,
                page = 24.dp,
                cardGap = 12.dp,
                paneWidth = 268.dp,
                readingMaxWidth = null,
                homeGridColumns = 2,
                useModalDrawer = false,
            )
        }

        MyNoteWindowClass.Expanded -> {
            AdaptiveTokens(
                windowClass = windowClass,
                page = 32.dp,
                cardGap = 14.dp,
                paneWidth = 300.dp,
                readingMaxWidth = 720.dp,
                homeGridColumns = 2,
                useModalDrawer = false,
            )
        }

        MyNoteWindowClass.Large -> {
            AdaptiveTokens(
                windowClass = windowClass,
                page = 40.dp,
                cardGap = 16.dp,
                paneWidth = 320.dp,
                readingMaxWidth = 740.dp,
                homeGridColumns = 3,
                useModalDrawer = false,
            )
        }

        MyNoteWindowClass.ExtraLarge -> {
            AdaptiveTokens(
                windowClass = windowClass,
                page = 56.dp,
                cardGap = 18.dp,
                paneWidth = 340.dp,
                readingMaxWidth = 780.dp,
                homeGridColumns = 3,
                useModalDrawer = false,
            )
        }
    }
