package com.hancekim.mynote.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.hancekim.mynote.core.designfoundation.token.MyNoteColors
import com.hancekim.mynote.core.designfoundation.token.StudyNotesDarkColors
import com.hancekim.mynote.core.designfoundation.window.AdaptiveTokens
import com.hancekim.mynote.core.designfoundation.window.MyNoteWindowClass
import com.hancekim.mynote.core.designfoundation.window.adaptiveTokensFor

private val LocalMyNoteColors = staticCompositionLocalOf<MyNoteColors> {
    error("MyNoteColors 가 제공되지 않았습니다. MyNoteTheme 으로 감싸세요.")
}
private val LocalAdaptiveTokens = staticCompositionLocalOf<AdaptiveTokens> {
    error("AdaptiveTokens 가 제공되지 않았습니다. MyNoteTheme 으로 감싸세요.")
}
private val LocalTextStyles = staticCompositionLocalOf<MyNoteTextStyles> {
    error("MyNoteTextStyles 가 제공되지 않았습니다. MyNoteTheme 으로 감싸세요.")
}
private val LocalFontScale = staticCompositionLocalOf { MyNoteFontScale.Normal }

/**
 * 앱 전역 테마. 핸드오프 토큰을 CompositionLocal 로 노출하고 Material3 ColorScheme 에 매핑한다.
 *
 * @param windowClass 반응형 토큰을 결정하는 폼팩터 단계
 * @param fontScale 본문 글자 크기 토글
 */
@Composable
fun MyNoteTheme(
    windowClass: MyNoteWindowClass = MyNoteWindowClass.Compact,
    fontScale: MyNoteFontScale = MyNoteFontScale.Normal,
    content: @Composable () -> Unit,
) {
    val colors = StudyNotesDarkColors
    val adaptive = adaptiveTokensFor(windowClass)
    val textStyles = remember(fontScale) { myNoteTextStyles(fontScale.bodySizeSp) }

    val colorScheme = darkColorScheme(
        primary = colors.accent,
        onPrimary = colors.bg,
        background = colors.bg,
        onBackground = colors.text,
        surface = colors.surface,
        onSurface = colors.text,
        surfaceVariant = colors.surface2,
        onSurfaceVariant = colors.text2,
        outline = colors.border,
    )

    CompositionLocalProvider(
        LocalMyNoteColors provides colors,
        LocalAdaptiveTokens provides adaptive,
        LocalTextStyles provides textStyles,
        LocalFontScale provides fontScale,
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

/** 핸드오프 토큰 접근자. `MyNoteTheme.colors`, `MyNoteTheme.adaptive`, `MyNoteTheme.textStyles`. */
object MyNoteTheme {
    val colors: MyNoteColors
        @Composable @ReadOnlyComposable get() = LocalMyNoteColors.current

    val adaptive: AdaptiveTokens
        @Composable @ReadOnlyComposable get() = LocalAdaptiveTokens.current

    val textStyles: MyNoteTextStyles
        @Composable @ReadOnlyComposable get() = LocalTextStyles.current

    val fontScale: MyNoteFontScale
        @Composable @ReadOnlyComposable get() = LocalFontScale.current
}
