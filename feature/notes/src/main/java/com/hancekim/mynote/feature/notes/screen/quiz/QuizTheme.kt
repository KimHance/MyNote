package com.hancekim.mynote.feature.notes.screen.quiz

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme

@Immutable
internal data class QuizTypography(
    val question: TextStyle,
    val choice: TextStyle,
    val screenTitle: TextStyle,
    val failTitle: TextStyle,
    val resultTitle: TextStyle,
    val score: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val label: TextStyle,
    val counter: TextStyle,
    val button: TextStyle,
)

@Composable
internal fun quizTypography(): QuizTypography {
    val short = MyNoteTheme.adaptive.heightClass == MyNoteHeightClass.Compact
    return remember(short) { quizTypographyFor(short) }
}

private fun quizTypographyFor(short: Boolean): QuizTypography =
    QuizTypography(
        question =
            TextStyle(
                fontSize = 23.sp,
                fontWeight = FontWeight.W700,
                lineHeight = (23 * 1.45f).sp,
                letterSpacing = (-0.01).em,
            ),
        choice = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.W400, lineHeight = (15 * 1.5f).sp),
        screenTitle =
            TextStyle(
                fontSize = if (short) 17.sp else 19.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.01).em,
            ),
        failTitle = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.W700, letterSpacing = (-0.01).em),
        resultTitle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.W700, letterSpacing = (-0.01).em),
        score =
            TextStyle(
                fontSize = if (short) 40.sp else 52.sp,
                fontWeight = FontWeight.W700,
                lineHeight = if (short) 40.sp else 52.sp,
                letterSpacing = (-0.03).em,
            ),
        body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W400, lineHeight = (14 * 1.7f).sp),
        caption = TextStyle(fontSize = 12.5f.sp, fontWeight = FontWeight.W400, lineHeight = (12.5f * 1.6f).sp),
        label =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = 0.12.em,
            ),
        counter = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.5f.sp, fontWeight = FontWeight.W600),
        button = TextStyle(fontSize = 15.5f.sp, fontWeight = FontWeight.W700),
    )

internal val QuizEasing: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

@Composable
internal fun rememberReduceMotion(): Boolean {
    val inspecting = LocalInspectionMode.current
    val context = LocalContext.current
    return remember(inspecting, context) {
        inspecting ||
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
    }
}
