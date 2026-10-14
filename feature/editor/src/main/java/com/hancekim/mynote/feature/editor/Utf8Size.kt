package com.hancekim.mynote.feature.editor

/** 편집기 · 붙여넣기 칸 글의 UTF-8 바이트 수 — 크기 이유 줄용. 저장 · 가져오기 판정 자체는 UseCase(`:core:format`)가 한다. */
internal object Utf8Size {
    private const val BYTES_PER_KILOBYTE = 1_024

    /**
     * [text] 를 UTF-8 로 쓴 바이트 수. 짝 없는 대리 문자는 인코더처럼 `?` 한 바이트로 센다.
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

                    ch.isSurrogate() -> {
                        1
                    }

                    else -> {
                        3
                    }
                }
            index++
        }
        return total
    }

    /** 바이트 수를 KB 로 올림한다 — 이유 줄의 `본문이 nKB로` 숫자. */
    fun kilobytes(bytes: Int): Int = (bytes + BYTES_PER_KILOBYTE - 1) / BYTES_PER_KILOBYTE
}
