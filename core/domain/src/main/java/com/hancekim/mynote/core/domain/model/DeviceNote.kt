package com.hancekim.mynote.core.domain.model

import com.hancekim.mynote.core.domain.repository.DeviceNoteText
import java.time.DateTimeException
import java.time.LocalDate

/**
 * 기기 노트 편집기의 값 — 새 노트 · 가져오기로 미리 채운 노트 · 고칠 노트가 같은 모양이다.
 *
 * @property title 제목 칸
 * @property group 그룹 칸. 비면 기본 그룹에 든다(LN-4)
 * @property body 본문 칸 — 마크다운 글자 그대로. 첫 줄이 `---` 여도 본문이다(LN-3)
 * @property date 노트 날짜. 가져온 글의 앞머리 날짜 · 고칠 노트의 만든 날이고, null 이면 저장하는 날이다
 */
data class DeviceNoteDraft(
    val title: String = "",
    val group: String = "",
    val body: String = "",
    val date: LocalDate? = null,
)

/**
 * 저장할 수 있는지(LN-3 ④). 크기는 저장 원문 기준으로 리더의 원문 한도와 같다.
 *
 * @property titleBlank 제목이 비었거나 공백만이다
 * @property bodyBlank 본문이 비었거나 공백만이다
 * @property bodyTooLarge 본문이 크기 경계를 넘는다
 */
data class DraftCheck(
    val titleBlank: Boolean,
    val bodyBlank: Boolean,
    val bodyTooLarge: Boolean,
) {
    val canSave: Boolean get() = !titleBlank && !bodyBlank && !bodyTooLarge
}

/** [DraftCheck.canSave] 가 거짓인 초안을 저장하려 했다. */
class InvalidDraftException(
    val check: DraftCheck,
) : IllegalArgumentException("draft cannot be saved: $check")

fun DeviceNoteDraft.check(text: DeviceNoteText): DraftCheck =
    DraftCheck(titleBlank = title.isBlank(), bodyBlank = body.isBlank(), bodyTooLarge = !text.fits(body))

/** 제목 · 그룹 칸의 한 줄 규칙(LN-3 ⑦ · LN-4). 편집기 칸 · 가져오기 제목 · 저장이 같이 쓴다. */
object DeviceNoteFields {
    /** 제목 · 그룹 칸에 드는 글자 수(코드 포인트). 목록 질의 한 행 크기를 묶는다. */
    const val MAX_LENGTH: Int = 200

    /** 줄바꿈(LF · CRLF · CR) 하나를 공백 하나로 바꾸고 [MAX_LENGTH] 글자까지만 둔다. */
    fun singleLine(text: String): String {
        val out = StringBuilder(minOf(text.length, MAX_LENGTH * 2))
        var count = 0
        var index = 0
        while (index < text.length && count < MAX_LENGTH) {
            val ch = text[index]
            when {
                ch == '\r' -> {
                    out.append(' ')
                    if (text.getOrNull(index + 1) == '\n') index++
                }

                ch == '\n' -> {
                    out.append(' ')
                }

                ch.isHighSurrogate() && text.getOrNull(index + 1)?.isLowSurrogate() == true -> {
                    out.append(ch).append(text[index + 1])
                    index++
                }

                else -> {
                    out.append(ch)
                }
            }
            count++
            index++
        }
        return out.toString()
    }

    /** 저장할 제목 — 한 줄로 다듬고 앞뒤 공백을 뺀다. */
    fun title(input: String): String = singleLine(input).trim()

    /** 저장할 그룹 — 한 줄로 다듬고 앞뒤 공백만 뺀 글자 그대로(대소문자를 가린다). 비면 [defaultGroup]. */
    fun group(
        input: String,
        defaultGroup: String,
    ): String = singleLine(input).trim().ifEmpty { singleLine(defaultGroup).trim() }
}

/** 노트 날짜 글자 `YYYY.MM.DD` — 저장소 노트 목록과 같은 모양이다(LN-4). */
object NoteDates {
    private val SHAPE = Regex("""(\d{4})\.(\d{2})\.(\d{2})""")

    fun format(date: LocalDate): String = "%04d.%02d.%02d".format(date.year, date.monthValue, date.dayOfMonth)

    /** `YYYY.MM.DD` 모양이고 있는 날이면 그 날, 아니면 null. */
    fun parse(text: String): LocalDate? {
        val (year, month, day) = SHAPE.matchEntire(text.trim())?.destructured ?: return null
        return try {
            LocalDate.of(year.toInt(), month.toInt(), day.toInt())
        } catch (_: DateTimeException) {
            null
        }
    }
}

/** 가져오기를 편집기 없이 끝낸 이유(LN-7). 다섯이 서로 다른 안내가 된다. */
enum class ImportRejection {
    /** 글이 비었거나 공백만이다. */
    Empty,

    /** 파일이 UTF-8 이 아니다. */
    NotUtf8,

    /** 원문이 512KB(524,288 바이트)를 넘는다. */
    TooLarge,

    /** 파일을 읽지 못했다(권한 · 사라짐). */
    Unreadable,

    /** 받지 않는 파일 종류다(표시 이름 확장자가 `.md` · `.markdown` 이 아니다). */
    UnsupportedType,
}

/** 가져오기 한 번의 결과. */
sealed interface ImportOutcome {
    /** 미리 채운 편집기를 연다. 노트는 저장해야 생긴다(LN-6). */
    data class Ready(
        val draft: DeviceNoteDraft,
    ) : ImportOutcome

    /** 편집기를 열지 않고 이유를 알린다. */
    data class Rejected(
        val reason: ImportRejection,
    ) : ImportOutcome

    /** 파일을 고르지 않고 돌아왔다 — 아무 일도 없다. */
    data object Dismissed : ImportOutcome
}

/** 파일 읽기 어댑터가 읽은 결과. 읽지 못하면 어댑터가 던진다. */
sealed interface FileText {
    data class Read(
        val text: String,
    ) : FileText

    data object TooLarge : FileText

    data object NotUtf8 : FileText
}

/** 가져올 수 있는 파일 이름 — 표시 이름 확장자 `.md` · `.markdown`(대소문자 무시). */
object ImportFileTypes {
    private val EXTENSIONS = listOf(".md", ".markdown")

    fun accepts(name: String?): Boolean = name != null && EXTENSIONS.any { name.endsWith(it, ignoreCase = true) }

    /** 이름 후보 — 확장자를 뺀 파일 이름. */
    fun stem(name: String): String =
        EXTENSIONS.firstOrNull { name.endsWith(it, ignoreCase = true) }?.let { name.dropLast(it.length) } ?: name
}
