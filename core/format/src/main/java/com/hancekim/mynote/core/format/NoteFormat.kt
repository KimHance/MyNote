package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.model.NoteBlock
import com.hancekim.mynote.core.domain.model.NoteLimits
import com.hancekim.mynote.core.domain.model.firstSentence
import javax.inject.Inject

/** 해석 결과가 바뀌는 판마다 올리는 번호. */
const val PARSER_VERSION: Int = 2

/** [PARSER_VERSION] 이 기준으로 삼은 마크다운 엔진 판. */
internal const val PARSER_ENGINE: String = "0.7.16"

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

/** 한도 안의 마크다운 원문을 블록으로 바꾼다. 실패하면 던진다. */
fun interface MarkdownConverter {
    fun convert(raw: String): List<NoteBlock>
}

/**
 * 원문을 경로로 고른 형식으로 해석한다. 경로가 `.md`(대소문자 무시)로 끝나면 마크다운, 그 밖이거나 없으면 JSON 이다.
 * 어떤 입력에도 던지지 않는다. 노트 전체 상자는 캐시에 원문을 두지 않는다.
 *
 * @param markdown 마크다운 원문을 블록으로 바꾸는 변환기
 * @param limits 원문 · 캐시 한도
 */
class NoteFormat(
    private val markdown: MarkdownConverter,
    private val limits: NoteLimits,
) {
    constructor(markdown: MarkdownConverter) : this(markdown, NoteLimits.Default)

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
    ): List<NoteBlock> =
        guarded({ reason -> noteBox(reason, raw).map { it.toDomain() } }) {
            cached(path, raw).blocks.map { it.toDomain() }
        }

    /**
     * 원문을 해석해 캐시 값을 만든다.
     *
     * @param path 저장소 안 경로. null 이면 JSON 으로 읽는다
     * @param raw 원문
     */
    fun derive(
        path: String?,
        raw: String,
    ): DerivedNote =
        guarded({ reason -> DerivedNote(BlockCache.encode(noteBox(reason, "")), null) }) {
            val cached = cached(path, raw)
            DerivedNote(cached.json, cached.blocks.map { it.toDomain() }.firstSentence())
        }

    /**
     * 캐시를 블록으로 읽는다. 노트 전체 상자이거나 캐시를 못 읽을 때만 원문을 읽는다.
     * 캐시를 못 읽으면 원문을 다시 해석하고, 원문도 없으면 캐시 글자를 든 노트 전체 상자를 돌려준다.
     *
     * @param blocksJson 블록 캐시 JSON
     * @param path 저장소 안 경로. null 이면 JSON 으로 읽는다
     * @param raw 원문을 읽는다. 없으면 null
     */
    suspend fun read(
        blocksJson: String,
        path: String?,
        raw: suspend () -> String?,
    ): List<NoteBlock> {
        val blocks =
            guarded({ null }) { BlockCache.decode(blocksJson).map { it.toDomain() } }
                ?: return raw()?.let { parse(path, it) }
                    ?: noteBox(UnsupportedReasonDto.BROKEN_NOTE, blocksJson).map { it.toDomain() }
        val box = blocks.singleOrNull() as? NoteBlock.Unsupported
        return if (box != null && box.reason.wholeNote && box.raw.isEmpty()) {
            listOf(box.copy(raw = raw().orEmpty()))
        } else {
            blocks
        }
    }

    internal fun blocks(
        path: String?,
        raw: String,
    ): List<NoteBlockDto> =
        guarded({ reason -> noteBox(reason, raw) }) {
            if (path.isMarkdown()) markdownBlocks(raw) else json.parse(raw)
        }

    private fun markdownBlocks(raw: String): List<NoteBlockDto> =
        if (exceedsMarkdownLimits(raw, limits)) {
            noteBox(UnsupportedReasonDto.LIMIT_EXCEEDED, raw)
        } else {
            markdown.convert(raw).map { it.toCache() }
        }

    private fun cached(
        path: String?,
        raw: String,
    ): Cached {
        val blocks = blocks(path, raw)
        val cache = BlockCache.encode(blocks.map { if (it.coversNote) it.withoutRaw() else it }, limits.maxCacheBytes)
        return if (cache == null) {
            Cached(
                noteBox(UnsupportedReasonDto.LIMIT_EXCEEDED, raw),
                BlockCache.encode(noteBox(UnsupportedReasonDto.LIMIT_EXCEEDED, "")),
            )
        } else {
            Cached(blocks, cache)
        }
    }
}

private class Cached(
    val blocks: List<NoteBlockDto>,
    val json: String,
)

private fun String?.isMarkdown(): Boolean = this?.endsWith(".md", ignoreCase = true) == true

private fun noteBox(
    reason: UnsupportedReasonDto,
    raw: String,
): List<NoteBlockDto> = listOf(NoteBlockDto.Unsupported(reason, raw = raw))

private fun NoteBlockDto.withoutRaw(): NoteBlockDto = (this as? NoteBlockDto.Unsupported)?.copy(raw = "") ?: this

private inline fun <T> guarded(
    fallback: (reason: UnsupportedReasonDto) -> T,
    block: () -> T,
): T =
    try {
        block()
    } catch (_: Exception) {
        fallback(UnsupportedReasonDto.BROKEN_NOTE)
    } catch (_: StackOverflowError) {
        fallback(UnsupportedReasonDto.BROKEN_NOTE)
    } catch (_: OutOfMemoryError) {
        fallback(UnsupportedReasonDto.LIMIT_EXCEEDED)
    }
