package com.hancekim.mynote.core.format

import com.hancekim.mynote.core.domain.model.DeviceNoteDraft
import com.hancekim.mynote.core.domain.model.DeviceNoteFields
import com.hancekim.mynote.core.domain.model.NoteDates
import com.hancekim.mynote.core.domain.model.NoteLimits
import com.hancekim.mynote.core.domain.repository.DeviceNoteText
import java.time.LocalDate

/** 기기 노트 원문 앞에 붙이는 빈 앞머리. 본문 첫 줄 `---` 가 앞머리로 먹히지 않게 한다(LN-3 ③). */
private const val EMPTY_FRONT_MATTER = "---\n---\n"

/** [EMPTY_FRONT_MATTER] 의 UTF-8 바이트 수. 본문이 쓸 수 있는 크기는 원문 한도에서 이만큼 작다. */
internal const val EMPTY_FRONT_MATTER_BYTES = 8

private const val HEADING_MARK = "# "
private val CLOSING_HASHES = Regex("""(?:^|[ \t]+)#+[ \t]*$""")
private const val QUOTES = "\"'"

/** 기기 노트의 저장 경로 — `.md` 로 끝나 리더 · 재변환이 마크다운으로 해석한다(NF-2). */
fun deviceNotePath(docId: String): String = "$docId.md"

/** 편집기 본문을 저장 원문으로 — 빈 앞머리 + 본문. */
fun deviceNoteRaw(body: String): String = EMPTY_FRONT_MATTER + body

/** 저장 원문을 편집기 본문으로 — 앞의 빈 앞머리를 뗀다. */
fun deviceNoteBody(raw: String): String = raw.removePrefix(EMPTY_FRONT_MATTER)

/**
 * 기기 노트 원문 규칙. 크기 경계는 리더의 원문 한도와 같은 바이트 셈이고, 가져오기의 앞머리 경계 · 줄 끝 다듬기는
 * 마크다운 해석(NF-1)과 같은 코드다.
 */
class DeviceNoteMarkdown(
    private val limits: NoteLimits = NoteLimits.Default,
) : DeviceNoteText {
    override fun fits(body: String): Boolean = !utf8LengthExceeds(body, limits.maxBytes - EMPTY_FRONT_MATTER_BYTES)

    override fun rawFits(text: String): Boolean = !utf8LengthExceeds(text, limits.maxBytes)

    override fun imported(
        text: String,
        nameHint: String?,
        today: LocalDate,
    ): DeviceNoteDraft {
        val source = markdownSource(text)
        val fields = frontMatterFields(source.head)
        val fromFrontMatter = fields[TITLE]?.takeIf { it.isNotBlank() }
        val heading = if (fromFrontMatter == null) firstHeading(source.body) else null
        val body =
            when {
                heading != null -> heading.rest
                source.frontMatter -> source.body.dropLeadingBlankLines()
                else -> source.body
            }
        val title = fromFrontMatter ?: heading?.title ?: nameHint.orEmpty()
        return DeviceNoteDraft(
            title = DeviceNoteFields.title(title),
            body = body,
            date = fields[DATE]?.let(NoteDates::parse) ?: today,
        )
    }

    private companion object {
        const val TITLE = "title"
        const val DATE = "date"
    }
}

private class Heading(
    val title: String,
    val rest: String,
)

/** 앞머리 줄 `키: 값` — 값은 첫 `:` 뒤에서 앞뒤 공백과 감싼 따옴표 한 쌍을 뺀 것(notes/SCHEMA.md 2절). 같은 키는 처음 것. */
private fun frontMatterFields(head: List<String>): Map<String, String> {
    val fields = LinkedHashMap<String, String>()
    for (line in head) {
        val colon = line.indexOf(':')
        if (colon < 0) continue
        fields.putIfAbsent(line.substring(0, colon).trim(), line.substring(colon + 1).unquote())
    }
    return fields
}

private fun String.unquote(): String {
    val value = trim()
    return if (value.length >= 2 && value.first() == value.last() && value.first() in QUOTES) {
        value.substring(1, value.length - 1)
    } else {
        value
    }
}

/** 빈 줄을 건너뛴 첫 줄이 1단 제목 줄(`# `)이면 그 글자와 그 줄을 뺀 본문. 닫는 `#` 은 떼고 인라인 표시는 그대로 둔다. */
private fun firstHeading(body: String): Heading? {
    val lines = body.split('\n')
    val index = lines.indexOfFirst { it.isNotBlank() }
    if (index < 0 || !lines[index].startsWith(HEADING_MARK)) return null
    val title =
        lines[index]
            .substring(HEADING_MARK.length)
            .trimEnd()
            .replace(CLOSING_HASHES, "")
            .trim()
    if (title.isEmpty()) return null
    return Heading(title, lines.drop(index + 1).joinToString("\n").dropLeadingBlankLines())
}

private fun String.dropLeadingBlankLines(): String {
    var start = 0
    while (true) {
        val end = indexOf('\n', start)
        if (end < 0 || substring(start, end).isNotBlank()) return substring(start)
        start = end + 1
    }
}
