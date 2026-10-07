package com.hancekim.mynote.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import com.hancekim.mynote.core.designfoundation.token.MyNoteColors
import com.hancekim.mynote.core.designfoundation.token.MyNoteFontScale
import com.hancekim.mynote.core.designfoundation.token.MyNoteTextStyles
import com.hancekim.mynote.core.designfoundation.token.StudyNotesDarkColors
import com.hancekim.mynote.core.designfoundation.token.myNoteTextStyles
import com.hancekim.mynote.core.designfoundation.window.AdaptiveTokens
import com.hancekim.mynote.core.designfoundation.window.MyNoteHeightClass
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designfoundation.window.adaptiveTokensFor

private val LocalMyNoteColors =
    staticCompositionLocalOf<MyNoteColors> {
        error("MyNoteColors 가 제공되지 않았습니다. MyNoteTheme 으로 감싸세요.")
    }
private val LocalAdaptiveTokens =
    staticCompositionLocalOf<AdaptiveTokens> {
        error("AdaptiveTokens 가 제공되지 않았습니다. MyNoteTheme 으로 감싸세요.")
    }
private val LocalTextStyles =
    staticCompositionLocalOf<MyNoteTextStyles> {
        error("MyNoteTextStyles 가 제공되지 않았습니다. MyNoteTheme 으로 감싸세요.")
    }
private val LocalFontScale = staticCompositionLocalOf { MyNoteFontScale.Normal }
private val LocalTitleFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/** 안쪽 [MyNoteTheme] 의 제목 글꼴 스타일이 쓸 글꼴을 준다. 주지 않으면 기기 글꼴이다. */
@Composable
fun ProvideTitleFontFamily(
    fontFamily: FontFamily,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalTitleFontFamily provides fontFamily, content = content)
}

@Composable
fun MyNoteTheme(
    windowClass: MyNoteWindowClass = MyNoteWindowClass.Compact,
    heightClass: MyNoteHeightClass = MyNoteHeightClass.Medium,
    fontScale: MyNoteFontScale = MyNoteFontScale.Normal,
    content: @Composable () -> Unit,
) {
    val colors = StudyNotesDarkColors
    val adaptive = remember(windowClass, heightClass) { adaptiveTokensFor(windowClass, heightClass) }
    val titleFontFamily = LocalTitleFontFamily.current
    val textStyles =
        remember(fontScale, heightClass, titleFontFamily) {
            myNoteTextStyles(fontScale.bodySizeSp, heightClass, titleFontFamily)
        }

    CompositionLocalProvider(
        LocalMyNoteColors provides colors,
        LocalAdaptiveTokens provides adaptive,
        LocalTextStyles provides textStyles,
        LocalFontScale provides fontScale,
        content = content,
    )
}

object MyNoteTheme {
    val colors: MyNoteColors
        @Composable @ReadOnlyComposable
        get() = LocalMyNoteColors.current

    val adaptive: AdaptiveTokens
        @Composable @ReadOnlyComposable
        get() = LocalAdaptiveTokens.current

    val textStyles: MyNoteTextStyles
        @Composable @ReadOnlyComposable
        get() = LocalTextStyles.current

    val fontScale: MyNoteFontScale
        @Composable @ReadOnlyComposable
        get() = LocalFontScale.current
}
