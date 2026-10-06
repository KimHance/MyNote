package com.hancekim.mynote.core.domain.model

private const val BYTES_PER_KILOBYTE = 1_024

/**
 * 노트 하나를 읽고 캐시에 두는 한도. 넘은 노트는 [UnsupportedReason.LimitExceeded] 상자 하나가 된다.
 *
 * @property maxDepth 줄 앞 컨테이너(인용 · 목록 표시) 중첩과 블록 깊이의 상한
 * @property maxKilobytes 마크다운 원문 UTF-8 크기의 상한(KB)
 * @property maxTableCells 노트 하나의 표 칸 수(머리 열 × 머리를 더한 행) 상한
 * @property maxCacheKilobytes 캐시 열 하나(블록 캐시 · 다시 읽는 원문)의 UTF-8 크기 상한(KB)
 */
data class NoteLimits(
    val maxDepth: Int = 32,
    val maxKilobytes: Int = 512,
    val maxTableCells: Int = 4_096,
    val maxCacheKilobytes: Int = 1_024,
) {
    val maxBytes: Int get() = maxKilobytes * BYTES_PER_KILOBYTE

    val maxCacheBytes: Int get() = maxCacheKilobytes * BYTES_PER_KILOBYTE

    companion object {
        val Default = NoteLimits()
    }
}
