package com.hancekim.mynote.core.designsystem.block

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hancekim.mynote.core.designfoundation.token.MyNoteColors
import com.hancekim.mynote.core.designsystem.R
import com.hancekim.mynote.core.designsystem.theme.MyNoteTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 글자 구간 묶음을 한 `Text` 로 그린다. 취소선 구간이 있으면 낭독 글자에 그 시작과 끝을 넣는다.
 *
 * @param uprightItalic 바탕 글자가 이미 기울었을 때 기울임 구간을 세워 구별한다
 * @param keepCodeTogether 코드 구간을 앞뒤 여백까지 한 낱말로 둔다
 */
@Composable
internal fun NoteSpansText(
    spans: ImmutableList<NoteContentSpan>,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    uprightItalic: Boolean = false,
    keepCodeTogether: Boolean = false,
    textAlign: TextAlign = TextAlign.Unspecified,
) {
    val colors = MyNoteTheme.colors
    val text =
        remember(spans, colors, uprightItalic, keepCodeTogether) {
            spans.toAnnotatedString(colors, uprightItalic, keepCodeTogether)
        }
    val strikeStart = stringResource(R.string.ds_block_strike_start)
    val strikeEnd = stringResource(R.string.ds_block_strike_end)
    val spoken = remember(spans, strikeStart, strikeEnd) { spans.spokenWithStrike(strikeStart, strikeEnd) }
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = textAlign,
        modifier = if (spoken != null) modifier.semantics { contentDescription = spoken } else modifier,
    )
}

private fun ImmutableList<NoteContentSpan>.toAnnotatedString(
    colors: MyNoteColors,
    uprightItalic: Boolean,
    keepCodeTogether: Boolean,
): AnnotatedString {
    val codePad = if (keepCodeTogether) NO_BREAK_SPACE else " "
    return buildAnnotatedString {
        this@toAnnotatedString.forEach { span ->
            val decoration = if (span.strike) TextDecoration.LineThrough else null
            when (span.style) {
                NoteSpanStyle.Code -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = colors.surface2,
                            color = colors.accent,
                            baselineShift = BaselineShift(CODE_BASELINE_SHIFT),
                            textDecoration = decoration,
                        ),
                    ) { append("$codePad${span.text}$codePad") }
                }

                NoteSpanStyle.Bold -> {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.W700,
                            color = colors.text,
                            fontStyle = span.fontStyle(uprightItalic),
                            textDecoration = decoration,
                        ),
                    ) { append(span.text) }
                }

                NoteSpanStyle.Normal -> {
                    val fontStyle = span.fontStyle(uprightItalic)
                    if (fontStyle == null && decoration == null) {
                        append(span.text)
                    } else {
                        withStyle(SpanStyle(fontStyle = fontStyle, textDecoration = decoration)) { append(span.text) }
                    }
                }
            }
        }
    }
}

/**
 * 취소선 구간을 시작 · 끝 낱말로 감싼 낭독 글자. 취소선이 없으면 null.
 *
 * @param start 취소선 구간 앞에 읽을 낱말
 * @param end 취소선 구간 뒤에 읽을 낱말
 */
internal fun List<NoteContentSpan>.spokenWithStrike(
    start: String,
    end: String,
): String? {
    if (none { it.strike }) return null
    val spoken = StringBuilder()
    var striking = false
    forEach { span ->
        if (span.strike != striking) {
            spoken.append(' ').append(if (span.strike) start else end).append(' ')
            striking = span.strike
        }
        spoken.append(span.text)
    }
    if (striking) spoken.append(' ').append(end)
    return spoken.toString().replace(SPACE_RUN, " ").trim()
}

private fun NoteContentSpan.fontStyle(uprightItalic: Boolean): FontStyle? =
    when {
        !italic -> null
        uprightItalic -> FontStyle.Normal
        else -> FontStyle.Italic
    }

private const val NO_BREAK_SPACE = " "
private const val CODE_BASELINE_SHIFT = 0.08f
private val SPACE_RUN = Regex(" {2,}")

private val previewSpans: ImmutableList<NoteContentSpan> =
    persistentListOf(
        NoteContentSpan("굵게 ", NoteSpanStyle.Bold),
        NoteContentSpan("기울임", NoteSpanStyle.Normal, italic = true),
        NoteContentSpan(" · ", NoteSpanStyle.Normal),
        NoteContentSpan("지운 글", NoteSpanStyle.Normal, strike = true),
        NoteContentSpan(" · ", NoteSpanStyle.Normal),
        NoteContentSpan("셋 다", NoteSpanStyle.Bold, italic = true, strike = true),
        NoteContentSpan(" · ", NoteSpanStyle.Normal),
        NoteContentSpan("runBlocking", NoteSpanStyle.Code, strike = true),
    )

@Preview(showBackground = true, backgroundColor = 0xFF0E1217, widthDp = 360)
@Composable
private fun NoteSpansTextPreview() {
    MyNoteTheme {
        Column(
            modifier =
                Modifier
                    .background(MyNoteTheme.colors.bg)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NoteSpansText(previewSpans, MyNoteTheme.textStyles.body, MyNoteTheme.colors.bodyText)
            NoteSpansText(
                spans = previewSpans,
                style = MyNoteTheme.textStyles.body.copy(fontStyle = FontStyle.Italic),
                color = MyNoteTheme.colors.text2,
                uprightItalic = true,
            )
        }
    }
}
