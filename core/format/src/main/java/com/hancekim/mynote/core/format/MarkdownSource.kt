package com.hancekim.mynote.core.format

private const val FRONT_MATTER_FENCE = "---"

/**
 * 마크다운 원문을 엔진에 넘길 본문으로 가른 결과.
 *
 * @property frontMatter 앞머리 경계를 찾았는지
 * @property body 앞머리 뒤의 본문. 경계가 없으면 글 전체
 * @property head 앞머리 두 경계 사이의 줄. 경계가 없으면 빈 목록
 */
internal data class MarkdownSource(
    val frontMatter: Boolean,
    val body: String,
    val head: List<String> = emptyList(),
)

/** 맨 앞 BOM 을 떼고 줄 끝(CRLF · CR)을 LF 로 바꾼 뒤 앞머리를 가른다. */
internal fun markdownSource(raw: String): MarkdownSource = splitFrontMatter(normalizeLineEnds(raw))

internal fun normalizeLineEnds(raw: String): String =
    raw
        .removePrefix("\uFEFF")
        .replace("\r\n", "\n")
        .replace('\r', '\n')

private fun splitFrontMatter(text: String): MarkdownSource {
    val lines = text.split('\n')
    if (lines.first() != FRONT_MATTER_FENCE) return MarkdownSource(frontMatter = false, body = text)
    val close =
        (1 until lines.size).firstOrNull { lines[it] == FRONT_MATTER_FENCE }
            ?: return MarkdownSource(frontMatter = false, body = text)
    return MarkdownSource(
        frontMatter = true,
        body = lines.drop(close + 1).joinToString("\n"),
        head = lines.subList(1, close),
    )
}
