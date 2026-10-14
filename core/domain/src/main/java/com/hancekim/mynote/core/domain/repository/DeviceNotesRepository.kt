package com.hancekim.mynote.core.domain.repository

import com.hancekim.mynote.core.domain.model.DeviceNoteDraft
import com.hancekim.mynote.core.domain.model.NoteBlock

/**
 * 기기 노트를 쓰고 고치고 지운다 — 앱 DB 가 원본인 노트의 쓰기 길이다. 저장소 노트는 이 길로 고치거나 지울 수 없다.
 * 쓴 결과는 [NotesRepository.observeCategories] 로 보인다.
 */
interface DeviceNotesRepository {
    /**
     * 새 기기 노트를 저장하고 그 노트 id 를 돌려준다.
     *
     * @param defaultGroup 그룹 칸이 비었을 때 저장할 그룹 이름
     */
    suspend fun create(
        draft: DeviceNoteDraft,
        defaultGroup: String,
    ): Result<String>

    /** 기기 노트의 제목 · 그룹 · 본문을 바꾼다. 키 · 날짜 · 처음 본 시각은 그대로다. 기기 노트가 아니거나 없으면 실패다. */
    suspend fun update(
        noteId: String,
        draft: DeviceNoteDraft,
        defaultGroup: String,
    ): Result<Unit>

    /** 기기 노트와 그 노트의 북마크를 지운다. 학습 기록은 남는다. 기기 노트가 아니거나 없으면 실패다. */
    suspend fun delete(noteId: String): Result<Unit>

    /** 고치기용 원문 — 지금 제목 · 그룹 · 본문 · 만든 날. 기기 노트가 아니거나 없으면 null. */
    suspend fun load(noteId: String): DeviceNoteDraft?

    /** 본문을 저장했을 때 리더가 그릴 블록 — 저장과 같은 경로 · 원문으로 해석한다(LN-3 ②). */
    suspend fun preview(body: String): List<NoteBlock>
}
