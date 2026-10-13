package com.hancekim.mynote.core.domain.repository

import com.hancekim.mynote.core.domain.model.DeviceNoteDraft
import java.time.LocalDate

/**
 * 기기 노트 원문 규칙 — 리더의 해석 규칙(note-format NF-1 · NF-8)과 같은 코드로 판정한다. 구현은 `:core:format` 에 있다.
 * 순수 함수라 어느 스레드에서 불러도 된다.
 */
interface DeviceNoteText {
    /** 본문을 저장 원문으로 만들었을 때 리더의 원문 한도(NF-8 512KB) 안인가(LN-3 ④). */
    fun fits(body: String): Boolean

    /** 가져올 원문이 512KB(524,288 바이트) 안인가(LN-7). */
    fun rawFits(text: String): Boolean

    /**
     * 가져오기 규칙(LN-6) — 줄 끝 · BOM 다듬기, 앞머리 `title` · `date`, 첫 `# ` 제목 줄, 이름 후보 순으로 편집기를 채운다.
     *
     * @param text 가져온 원문
     * @param nameHint 이름 후보 — 확장자를 뺀 파일 이름 또는 공유 제목. 없으면 null
     * @param today 앞머리 날짜가 없거나 모양이 틀릴 때 쓰는 날
     */
    fun imported(
        text: String,
        nameHint: String?,
        today: LocalDate,
    ): DeviceNoteDraft
}
