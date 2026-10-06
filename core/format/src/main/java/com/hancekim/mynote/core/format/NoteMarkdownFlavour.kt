package com.hancekim.mynote.core.format

import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.sequentialparsers.DelimiterParser
import org.intellij.markdown.parser.sequentialparsers.EmphasisLikeParser
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.SequentialParserManager
import org.intellij.markdown.parser.sequentialparsers.TokensCache
import org.intellij.markdown.parser.sequentialparsers.impl.AutolinkParser
import org.intellij.markdown.parser.sequentialparsers.impl.BacktickParser
import org.intellij.markdown.parser.sequentialparsers.impl.EmphStrongDelimiterParser
import org.intellij.markdown.parser.sequentialparsers.impl.ImageParser
import org.intellij.markdown.parser.sequentialparsers.impl.InlineLinkParser
import org.intellij.markdown.parser.sequentialparsers.impl.ReferenceLinkParser

private const val MAX_STRIKE_RUN = 2

/** GFM 에서 수식 · 형광을 빼고(원문 글자로 남는다) 취소선 짝 찾기를 고친 flavour. */
internal object NoteMarkdownFlavour : GFMFlavourDescriptor() {
    override val sequentialParserManager =
        object : SequentialParserManager() {
            override fun getParserSequence(): List<SequentialParser> =
                listOf(
                    AutolinkParser(listOf(MarkdownTokenTypes.AUTOLINK, GFMTokenTypes.GFM_AUTOLINK)),
                    BacktickParser(),
                    ImageParser(),
                    InlineLinkParser(),
                    ReferenceLinkParser(),
                    EmphasisLikeParser(EmphStrongDelimiterParser(), TildeDelimiterParser()),
                )
        }
}

/** 물결 한 개 · 두 개 짝을 취소선으로 묶는다. */
private class TildeDelimiterParser : DelimiterParser() {
    override fun scan(
        tokens: TokensCache,
        iterator: TokensCache.Iterator,
        delimiters: MutableList<Info>,
    ): Int {
        if (iterator.type != GFMTokenTypes.TILDE) return 0
        var run = 1
        var right = iterator
        while (run < maxAdvance && right.rawLookup(1) == GFMTokenTypes.TILDE) {
            right = right.advance()
            run++
        }
        val (canOpen, canClose) = canOpenClose(tokens, iterator, right, canSplitText = true)
        repeat(run) {
            delimiters += Info(GFMTokenTypes.TILDE, iterator.index + it, 0, canOpen, canClose, '~')
        }
        return run
    }

    override fun process(
        tokens: TokensCache,
        iterator: TokensCache.Iterator,
        delimiters: MutableList<Info>,
        result: SequentialParser.ParsingResultBuilder,
    ) {
        var index = delimiters.lastIndex
        while (index >= 0) {
            val delimiter = delimiters[index]
            if (delimiter.tokenType != GFMTokenTypes.TILDE || delimiter.closerIndex == -1) {
                index--
                continue
            }
            var opener = index
            var closer = delimiter.closerIndex
            var matched = 1
            while (EmphStrongDelimiterParser.areAdjacentSameMarkers(delimiters, opener, closer)) {
                opener--
                closer++
                matched++
            }
            if (matched <= MAX_STRIKE_RUN) {
                val range = delimiters[opener].position..delimiters[closer].position + 1
                result.withNode(SequentialParser.Node(range, GFMElementTypes.STRIKETHROUGH))
            }
            index = opener - 1
        }
    }
}
