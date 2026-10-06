package com.hancekim.mynote.core.domain.model

private const val BYTES_PER_KILOBYTE = 1_024

/**
 * 마크다운 원문을 해석하기 전에 보는 한도. 넘은 노트는 [UnsupportedReason.LimitExceeded] 상자 하나가 된다.
 *
 * @property maxDepth 줄 앞 컨테이너(인용 · 목록 표시) 중첩과 블록 깊이의 상한
 * @property maxKilobytes 원문 UTF-8 크기의 상한(KB)
 */
data class NoteLimits(
    val maxDepth: Int,
    val maxKilobytes: Int,
) {
    val maxBytes: Int get() = maxKilobytes * BYTES_PER_KILOBYTE

    companion object {
        val Default = NoteLimits(maxDepth = 32, maxKilobytes = 512)
    }
}
