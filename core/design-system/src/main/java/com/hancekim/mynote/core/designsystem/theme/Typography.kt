package com.hancekim.mynote.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

enum class MyNoteFontScale(
    val bodySizeSp: Float,
) {
    Normal(16f),
    Large(18f),
}

@Immutable
data class MyNoteTextStyles(
    val noteTitle: TextStyle,
    val sectionTitle: TextStyle,
    val overline: TextStyle,
    val cardTitle: TextStyle,
    val treeLabel: TextStyle,
    val caption: TextStyle,
    val mdHeading2: TextStyle,
    val mdHeading3: TextStyle,
    val body: TextStyle,
    val mono: TextStyle,
)

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
