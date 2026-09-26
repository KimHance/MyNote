package com.hancekim.mynote.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass

enum class MyNoteFontScale(
    val bodySizeSp: Float,
) {
    Normal(16f),
    Large(18f),
}

@Immutable
data class MyNoteTextStyles(
    val display: TextStyle,
    val noteTitle: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val sectionTitle: TextStyle,
    val screenTitle: TextStyle,
    val cardTitle: TextStyle,
    val overline: TextStyle,
    val treeLabel: TextStyle,
    val caption: TextStyle,
    val mdHeading2: TextStyle,
    val mdHeading3: TextStyle,
    val body: TextStyle,
    val bodyCompact: TextStyle,
    val bodySmall: TextStyle,
    val button: TextStyle,
    val mono: TextStyle,
    val monoLabel: TextStyle,
    val monoSmall: TextStyle,
)

fun myNoteTextStyles(
    bodySizeSp: Float,
    heightClass: MyNoteHeightClass = MyNoteHeightClass.Medium,
): MyNoteTextStyles {
    val short = heightClass == MyNoteHeightClass.Compact
    return MyNoteTextStyles(
        display =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = if (short) 40.sp else 52.sp,
                fontWeight = FontWeight.W700,
                lineHeight = if (short) 40.sp else 52.sp,
                letterSpacing = (-0.03).em,
            ),
        noteTitle =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 28.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.015).em,
                lineHeight = (28 * 1.25f).sp,
            ),
        title =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 24.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.01).em,
            ),
        headline =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 23.sp,
                fontWeight = FontWeight.W700,
                lineHeight = (23 * 1.45f).sp,
                letterSpacing = (-0.01).em,
            ),
        sectionTitle =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 21.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.01).em,
            ),
        screenTitle =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = if (short) 17.sp else 19.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.01).em,
            ),
        cardTitle =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                lineHeight = 22.sp,
            ),
        overline =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 12.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = 0.08.em,
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
        bodyCompact =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 15.sp,
                fontWeight = FontWeight.W400,
                lineHeight = (15 * 1.5f).sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 14.sp,
                fontWeight = FontWeight.W400,
                lineHeight = (14 * 1.7f).sp,
            ),
        button =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontSize = 15.5f.sp,
                fontWeight = FontWeight.W700,
            ),
        mono =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.W400,
                lineHeight = (13 * 1.65f).sp,
            ),
        monoLabel =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = 0.12.em,
            ),
        monoSmall =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5f.sp,
                fontWeight = FontWeight.W600,
            ),
    )
}
