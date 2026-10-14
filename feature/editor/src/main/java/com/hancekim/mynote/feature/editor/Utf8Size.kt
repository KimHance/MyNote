package com.hancekim.mynote.feature.editor

/** 편집기 · 붙여넣기 칸 글의 UTF-8 바이트 수 — 크기 이유 줄용. 저장 · 가져오기 판정 자체는 UseCase(`:core:format`)가 한다. */
internal object Utf8Size {
    private const val BYTES_PER_KILOBYTE = 1_024

    /**
     * 저장 원문 앞의 빈 앞머리(`---` 두 줄) 바이트 수 — `:core:format` `DeviceNoteMarkdown` 의 `EMPTY_FRONT_MATTER_BYTES` 와 같다
     * (plan D19). 이 모듈은 `:core:format` 을 모르므로 값을 따로 둔다.
     */
    private const val SAVED_FRONT_MATTER_BYTES = 8

    /**
     * [text] 를 UTF-8 로 쓴 바이트 수. 짝 없는 대리 문자는 3 바이트로 센다 — 저장 판정(`:core:format` `MarkdownLimits`)과 같은 셈이다(plan D46).
     * [stopAfter] 를 넘으면 그 자리에서 멈추고 지금까지 센 값을 돌려준다(경계 판정은 끝까지 셀 필요가 없다).
     */
    fun bytes(
        text: CharSequence,
        stopAfter: Int = Int.MAX_VALUE,
    ): Int {
        var total = 0
        var index = 0
        while (index < text.length && total <= stopAfter) {
            val ch = text[index]
            total +=
                when {
                    ch.code < 0x80 -> {
                        1
                    }

                    ch.code < 0x800 -> {
                        2
                    }

                    ch.isHighSurrogate() && text.getOrNull(index + 1)?.isLowSurrogate() == true -> {
                        index++
                        4
                    }

                    else -> {
                        3
                    }
                }
            index++
        }
        return total
    }

    /** 바이트 수를 KB 로 올림한다. */
    fun kilobytes(bytes: Int): Int = (bytes + BYTES_PER_KILOBYTE - 1) / BYTES_PER_KILOBYTE

    /**
     * 이유 줄의 `본문이 nKB로` 숫자 — [body] 를 저장할 원문(빈 앞머리 + 본문)의 KB 올림이다(plan D44). 저장 경계가 원문 512KB 라
     * 경계를 넘은 본문은 늘 513 이상으로 보인다(본문만 세면 512 로 보여 "512KB를 넘습니다" 가 틀린 문장이 된다).
     */
    fun savedKilobytes(body: CharSequence): Int = kilobytes(SAVED_FRONT_MATTER_BYTES + bytes(body))
}
