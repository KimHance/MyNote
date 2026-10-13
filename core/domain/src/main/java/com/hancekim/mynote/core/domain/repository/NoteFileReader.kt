package com.hancekim.mynote.core.domain.repository

import com.hancekim.mynote.core.domain.model.FileText

/** 사용자가 고른 파일 하나를 읽는다(LN-6 (b)). 구현은 `:core:data`(시스템 파일 고르기의 주소). */
interface NoteFileReader {
    /** 파일 표시 이름. 없으면 null. 파일에 닿지 못하면 던진다. */
    suspend fun displayName(uri: String): String?

    /**
     * 파일을 엄격한 UTF-8 로 읽는다. [maxBytes] + 1 바이트까지만 읽고 넘으면 [FileText.TooLarge],
     * UTF-8 이 아니면 [FileText.NotUtf8] 이다. 읽지 못하면 던진다.
     */
    suspend fun read(
        uri: String,
        maxBytes: Int,
    ): FileText
}
