package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.model.NoteBlock
import javax.inject.Inject

/** 해석 결과가 바뀌는 판마다 올리는 번호. 이보다 낮은 판으로 만든 캐시는 원문에서 다시 만든다. */
const val PARSER_VERSION: Int = 2

/**
 * 원문 하나를 해석해 캐시에 두는 값.
 *
 * @property blocksJson 블록 캐시 JSON
 * @property excerpt 첫 문장. 없으면 null
 */
data class DerivedNote(
    val blocksJson: String,
    val excerpt: String?,
)

/** 한도 안의 마크다운 원문을 블록으로 바꾼다. 던진 예외는 [NoteFormat] 이 노트 전체 상자로 바꾼다. */
fun interface MarkdownConverter {
    fun convert(raw: String): List<NoteBlock>
}

/**
 * 원문을 경로로 고른 형식으로 해석한다. 경로가 `.md`(대소문자 무시)로 끝나면 마크다운, 그 밖이거나 없으면 JSON 이다.
 * 어떤 입력에도 던지지 않는다.
 *
 * @param markdown 마크다운 원문을 블록으로 바꾸는 변환기
 */
class NoteFormat(
    private val markdown: MarkdownConverter,
) {
    @Inject
    constructor() : this(GfmMarkdownConverter())

    private val json = MyNoteJsonFormat(BlockJson)

    /**
     * 원문을 블록으로 바꾼다.
     *
     * @param path 저장소 안 경로. null 이면 JSON 으로 읽는다
     * @param raw 원문
     */
    fun parse(
        path: String?,
        raw: String,
    ): List<NoteBlock> = guarded({ brokenNote(raw).map { it.toDomain() } }) { blocks(path, raw).map { it.toDomain() } }

    /**
     * 원문을 해석해 캐시 값을 만든다.
     *
     * @param path 저장소 안 경로. null 이면 JSON 으로 읽는다
     * @param raw 원문
     */
    fun derive(
        path: String?,
        raw: String,
    ): DerivedNote = guarded({ json.derive(brokenNote(raw)) }) { json.derive(blocks(path, raw)) }

    /**
     * 캐시를 블록으로 읽는다. 캐시를 못 읽으면 원문을 다시 해석하고, 원문도 없으면 캐시 글자를 든 노트 전체 상자를 돌려준다.
     *
     * @param blocksJson 블록 캐시 JSON
     * @param path 저장소 안 경로. null 이면 JSON 으로 읽는다
     * @param raw 원문. 없으면 null
     */
    fun read(
        blocksJson: String,
        path: String?,
        raw: String?,
    ): List<NoteBlock> =
        guarded({ raw?.let { parse(path, it) } ?: brokenNote(blocksJson).map { it.toDomain() } }) {
            json.decode(blocksJson).map { it.toDomain() }
        }

    internal fun blocks(
        path: String?,
        raw: String,
    ): List<NoteBlockDto> =
        guarded({ brokenNote(raw) }) {
            if (path.isMarkdown()) markdownBlocks(raw) else json.parse(raw)
        }

    private fun markdownBlocks(raw: String): List<NoteBlockDto> =
        if (exceedsMarkdownLimits(raw)) {
            listOf(NoteBlockDto.Unsupported(UnsupportedReasonDto.LIMIT_EXCEEDED, raw = raw))
        } else {
            markdown.convert(raw).map { it.toCache() }
        }
}

private fun String?.isMarkdown(): Boolean = this?.endsWith(".md", ignoreCase = true) == true

private fun brokenNote(raw: String): List<NoteBlockDto> =
    listOf(NoteBlockDto.Unsupported(UnsupportedReasonDto.BROKEN_NOTE, raw = raw))

private inline fun <T> guarded(
    fallback: () -> T,
    block: () -> T,
): T =
    try {
        block()
    } catch (_: Exception) {
        fallback()
    } catch (_: StackOverflowError) {
        fallback()
    }
