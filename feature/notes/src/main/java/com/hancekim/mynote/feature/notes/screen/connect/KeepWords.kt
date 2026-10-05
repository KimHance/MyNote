package com.hancekim.mynote.feature.notes.screen.connect

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

private const val WORD_JOINER = "⁠"

private val HangulPair = Regex("(?<=[가-힣])(?=[가-힣])")

internal fun String.keepKoreanWords(): String = replace(HangulPair, WORD_JOINER)

@Composable
internal fun keepWordsResource(
    @StringRes id: Int,
    vararg args: Any,
): String = stringResource(id, *args).keepKoreanWords()
