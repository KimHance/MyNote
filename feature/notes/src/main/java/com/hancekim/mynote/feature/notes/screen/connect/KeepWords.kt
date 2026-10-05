package com.hancekim.mynote.feature.notes.screen.connect

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

private const val WORD_JOINER = '⁠'

private fun Char.isHangulSyllable(): Boolean = this in '가'..'힣'

internal fun String.keepKoreanWords(): String = keepKoreanWords(AnnotatedString(this)).text

internal fun keepKoreanWords(raw: AnnotatedString): AnnotatedString {
    val source = raw.text
    val starts = IntArray(source.length + 1)
    val builder = StringBuilder(source.length * 2)
    for (index in source.indices) {
        if (index > 0 && source[index - 1].isHangulSyllable() && source[index].isHangulSyllable()) {
            builder.append(WORD_JOINER)
        }
        starts[index] = builder.length
        builder.append(source[index])
    }
    starts[source.length] = builder.length

    fun end(range: Int): Int = if (range == 0) 0 else starts[range - 1] + 1
    return AnnotatedString(
        text = builder.toString(),
        spanStyles =
            raw.spanStyles.map {
                AnnotatedString.Range(it.item, starts[it.start], maxOf(starts[it.start], end(it.end)), it.tag)
            },
        paragraphStyles =
            raw.paragraphStyles.map {
                AnnotatedString.Range(it.item, starts[it.start], maxOf(starts[it.start], end(it.end)), it.tag)
            },
    )
}

internal data class KeptText(
    val display: AnnotatedString,
    val plain: AnnotatedString,
)

internal fun keptTextOf(raw: AnnotatedString): KeptText = KeptText(display = keepKoreanWords(raw), plain = raw)

@Composable
internal fun KeepWordsText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
) {
    KeepWordsText(
        text = AnnotatedString(text),
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = style,
    )
}

@Composable
internal fun KeepWordsText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
) {
    val kept = remember(text) { keptTextOf(text) }
    Text(
        text = kept.display,
        modifier = modifier.clearAndSetSemantics { this.text = kept.plain },
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = style,
    )
}
